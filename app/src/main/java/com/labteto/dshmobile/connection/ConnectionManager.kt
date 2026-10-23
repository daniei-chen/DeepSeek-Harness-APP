package com.labteto.dshmobile.connection

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.labteto.dshmobile.core.wire.ConnectionLoop
import com.labteto.dshmobile.core.wire.ConnectionState
import com.labteto.dshmobile.core.wire.DshApiClient
import com.labteto.dshmobile.core.wire.dto.HostDescription
import com.labteto.dshmobile.core.wire.LoopConfig
import com.labteto.dshmobile.core.wire.GenerationFailure
import com.labteto.dshmobile.core.wire.HandshakeStep
import com.labteto.dshmobile.core.wire.LoopSinks
import com.labteto.dshmobile.core.wire.HostGeneration
import com.labteto.dshmobile.core.wire.RemoteStreamMux
import com.labteto.dshmobile.core.wire.dto.RemoteEventFrame
import com.labteto.dshmobile.core.wire.TransportFailure
import com.labteto.dshmobile.core.wire.TransportFailures
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** UI-facing connection state. */
enum class ConnectionPhase { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING }

/**
 * How far the readiness handshake has got, so the connect screen can say what it is doing.
 *
 * `OpeningStreams` is now one socket rather than two, and `Verifying` is waiting for the
 * host's ready frame rather than a `host.describe` answer. The names are kept because what
 * they mean to someone watching the screen has not changed.
 */
enum class ConnectStage { Idle, Validating, Reaching, OpeningStreams, Verifying, Connected }

data class ConnectionUiState(
    val phase: ConnectionPhase = ConnectionPhase.DISCONNECTED,
    val host: HostConfig? = null,
    val description: HostDescription? = null,
    val stage: ConnectStage = ConnectStage.Idle,
    /**
     * Why the most recent generation failed, or null.
     *
     * Survives the backoff ticks between attempts on purpose: the loop keeps retrying, and wiping
     * this on every state change would blank the only explanation the user gets.
     *
     * It is *not* written on every failed generation. See [ConnectionManager.onGenerationFailed]:
     * the loop's first retries are the shape a Wi-Fi blip and a background network switch take as
     * well, so they are reported only once they have lasted long enough to be more than that.
     */
    val failure: ConnectFailure? = null,
    /** Consecutive failed handshake attempts; 0 while none has failed. */
    val attempts: Int = 0,
    /** True once at least one generation completed the readiness handshake. */
    val hasConnected: Boolean = false,
)

/**
 * Owns the live connection to one harness: the ConnectionLoop (readiness
 * handshake + reconnect/backoff), the foreground service binding for
 * background operation, and the UI state mirror. Single active host at a time.
 *
 * Two policies live here rather than in the loop because the loop is shared protocol code and must
 * keep retrying forever:
 *
 * - **No false alarms.** A failed generation is not the same thing as a connection that has failed.
 *   The first few are reported to nobody, because one dropped handshake is what every network
 *   switch looks like, and the next attempt normally succeeds.
 * - **No infinite retrying.** A run of failures that will not end stops itself, with an error that
 *   says retrying is now a manual act.
 */
@Singleton
class ConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clientFactory: HarnessClientFactory,
    private val hostsStore: HostsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(ConnectionUiState())
    val state: StateFlow<ConnectionUiState> = _state.asStateFlow()

    private var loop: ConnectionLoop? = null
    private var api: DshApiClient? = null
    private var activeHost: HostConfig? = null

    /**
     * The current generation, or null while disconnected.
     *
     * Screens need it for two things the unary client cannot give them: the mux, to open a
     * session journal or the control stream, and the `clientId` every waterfall answer has to
     * carry. It is replaced wholesale on each reconnect, so nothing may cache the mux out of it.
     */
    @Volatile
    var generation: HostGeneration? = null
        private set

    /**
     * Host event frames (screens subscribe here).
     *
     * One flow now, where there were two: 0.1.2 delivers notifications and pending waterfalls
     * on the single `$events` stream, and the mux/host split it replaced no longer exists.
     *
     * Nothing here is replayed after a reconnect. State that must survive one has to come from
     * a query or a stream baseline instead — an `emit` that arrives while disconnected is gone.
     */
    val eventFrames = kotlinx.coroutines.flow.MutableSharedFlow<RemoteEventFrame>(extraBufferCapacity = 256)

    /**
     * Identity of the loop whose callbacks this manager still believes, or null when none runs.
     *
     * A rebuild stops one loop and starts another, and `stop()` cancels a coroutine without being
     * able to retract a callback already inside it — the sink call runs in the loop's own coroutine,
     * so a stop that lands mid-callback returns normally and whatever that callback then writes is
     * a report about a connection that no longer exists. Each loop is given a fresh token; a
     * callback from a loop whose token is no longer current does nothing.
     *
     * A stale `onConnected` is the worst of the set: it would publish CONNECTED, hand out a dead
     * `generation`, and start the foreground service for a socket nobody holds.
     */
    @Volatile
    private var loopToken: Any? = null

    /**
     * `elapsedRealtime()` at the first failure of the current run of failures, or 0 when nothing is
     * failing.
     *
     * Both thresholds below are measured from here: an error is worth showing only once the run has
     * lasted long enough not to be a blip, and automatic reconnecting is abandoned once it has
     * lasted too long. Every path that begins a new run clears it — a completed handshake, a
     * rebuilt loop, [connect] and [disconnect].
     *
     * Monotonic rather than wall-clock on purpose: the phone's clock is adjusted by the network,
     * and a backwards jump would either stall the brake or trip it immediately.
     */
    @Volatile
    private var failingSinceMs: Long = 0L

    /** Guards the two fields below. See [reconnectIfNeeded]. */
    private val rebuildLock = Any()

    /** Whose rebuild cycle is current, or null when none is running. */
    private var rebuildToken: Any? = null

    /** A caller arrived while a cycle was running; the cycle runs one more pass for it. */
    private var rebuildPending = false

    /** Whether the foreground service is believed to be up. */
    @Volatile
    private var serviceRunning = false

    /** A start was refused (background), so the next foreground moment should try again. */
    @Volatile
    private var serviceStartDeferred = false

    /**
     * The sink set for the loop identified by [id].
     *
     * One object per loop rather than one shared for the life of the manager: every callback has to
     * be attributable to the loop that made it, and closing over the token is the least machinery
     * that gets there.
     */
    private fun sinks(id: Any): LoopSinks = object : LoopSinks {
        override fun onEventFrame(frame: RemoteEventFrame) {
            // A frame from a replaced loop belongs to a generation nothing is reading any more.
            if (loopToken !== id) return
            eventFrames.tryEmit(frame)
        }

        override fun onConnected(generation: HostGeneration) {
            if (loopToken !== id) return
            // Nothing below may throw: this runs inside the connection loop's own coroutine, and an
            // exception here ends the loop — the socket dies, the notification never appears and the
            // user sees the app go quiet in the background. Every step that can fail (a DataStore
            // read, a foreground-service start Android refuses) is allowed to fail on its own.
            runCatching {
            this@ConnectionManager.generation = generation
            val host = activeHost
            if (host != null) scope.launch { hostsStore.touchHost(host.id) }
            // A completed handshake settles the question the failure policy was waiting on, so the
            // run of failures ends here, with its clock.
            failingSinceMs = 0L
            _state.value = ConnectionUiState(
                phase = ConnectionPhase.CONNECTED,
                host = activeHost,
                description = generation.description,
                stage = ConnectStage.Connected,
                failure = null,
                attempts = 0,
                hasConnected = true,
            )
                maybeStartService()
            }
        }

        override fun onStateChange(state: ConnectionState) {
            if (loopToken !== id) return
            val current = _state.value
            val phase = when {
                state == ConnectionState.CONNECTED -> ConnectionPhase.CONNECTED
                // The loop opens every generation the same way, but the first one is not a
                // *re*connect — calling it that is what let a never-connected attempt look like a
                // healthy session dropping, and hid it from the connect screen entirely.
                current.hasConnected -> ConnectionPhase.RECONNECTING
                else -> ConnectionPhase.CONNECTING
            }
            // Note: does not clear `failure`. The loop emits this on every retry, so clearing here
            // would erase the explanation a fraction of a second after showing it.
            _state.value = current.copy(phase = phase)
        }

        override fun onHandshakeStep(step: HandshakeStep) {
            if (loopToken !== id) return
            val stage = when (step) {
                HandshakeStep.OPENING_MUX -> ConnectStage.OpeningStreams
                HandshakeStep.AWAITING_READY -> ConnectStage.Verifying
            }
            _state.value = _state.value.copy(stage = stage)
        }

        override fun onGenerationFailed(attempt: Int, failure: GenerationFailure) {
            if (loopToken !== id) return
            val host = activeHost
            val nowMs = SystemClock.elapsedRealtime()
            // `attempt` is the loop's own consecutive count and it resets on success, so attempt 1
            // is the first failure of a new run — which is where the clock starts.
            if (attempt <= 1 || failingSinceMs == 0L) failingSinceMs = nowMs
            val failingForMs = nowMs - failingSinceMs
            val reported = ConnectFailure.from(failure, relay = host?.isRelay == true)

            if (failure.isRateLimited()) {
                // HTTP 429 from whatever fronts the harness — nginx's limit_req is the usual one.
                // The loop's cadence (from 500 ms) is exactly the wrong answer to a limiter: every
                // attempt refreshes the bucket, so a momentary limit becomes a lockout and the screen
                // says "reconnecting" forever. Stop, say why, and try once more after it drains.
                stopRetrying(id, RATE_LIMITED)
                scheduleRateLimitedRetry()
                return
            }

            if (host != null && isTerminalForRelay(host, failure)) {
                // Nothing the phone can retry fixes a revoked credential, so this one is worth
                // saying on the first failure instead of after the dwell.
                stopRetrying(id, reported)
                return
            }

            if (attempt >= STOP_AFTER_FAILURES || failingForMs >= STOP_AFTER_MS) {
                // The brake. The loop would retry until the process dies; a phone on a host that is
                // gone for a day should stop trying, and say that trying has become manual.
                stopRetrying(id, RETRIES_EXHAUSTED)
                return
            }

            // Below both thresholds a failure is a blip until proven otherwise: keep retrying and
            // claim nothing. `phase` already reads RECONNECTING and `attempts` keeps moving, so the
            // screen can show that something is being tried without saying the connection failed.
            val worthReporting =
                attempt >= REPORT_AFTER_FAILURES && failingForMs >= REPORT_AFTER_MS
            _state.value = _state.value.copy(
                failure = if (worthReporting) reported else _state.value.failure,
                attempts = attempt,
            )
        }
    }

    val connectedApi: DshApiClient? get() = api

    /**
     * Start driving [config]. Progress and failure arrive through [state], not a callback.
     *
     * There used to be a 2500ms timer here that reported failure if the phase was still CONNECTING.
     * It could never fire: the loop's first act is to publish RECONNECTING, so the phase had always
     * moved on by the time the timer checked. The result was a Connect button that stayed disabled
     * forever with nothing on screen. The loop now reports each failed generation directly, which
     * is both sooner and specific.
     */
    suspend fun connect(config: HostConfig) {
        disconnect()
        activeHost = config
        _state.value = ConnectionUiState(
            phase = ConnectionPhase.CONNECTING,
            host = config,
            stage = ConnectStage.OpeningStreams,
        )
        api = clientFactory.clientFor(config)
        // A fresh loop is a fresh run of failures: [startLoop] resets the clock, and the loop's own
        // counter starts at 1, so a host gets the full window again however often it is retried.
        startLoop(config)
        hostsStore.upsertHost(config)
    }

    fun disconnect() {
        // Before the teardown: a cycle that commits afterwards would rebuild the connection the
        // user just dismissed.
        cancelRebuild()
        // Dropped before the stop, not after: `stop()` cannot interrupt a callback that is already
        // inside it, and that callback writes [state]. Dropping the token first is what makes the
        // clean state written at the end of this function the last word.
        loopToken = null
        loop?.stop()
        loop = null
        api = null
        generation = null
        activeHost = null
        failingSinceMs = 0L
        stopService()
        _state.value = ConnectionUiState()
    }

    /**
     * Start a fresh connection cycle, unless one is already running.
     *
     * Three callers reach this on a bad network — the network callback, [KeepAliveWorker] and the
     * connect screen through `SessionStore.retryConnection` — and this used to stop the loop and
     * launch its own replacement for each. On a slow network they overlapped: every caller stopped
     * the loop the previous caller had just started, so the socket was rebuilt over and over and
     * the screen never left "reconnecting".
     *
     * Now one cycle runs at a time, and a caller that arrives during it merges: [rebuildPending]
     * asks the running cycle for one more pass rather than racing it for the socket. Merging is
     * also what keeps the backoff honest — a rebuild replaces the loop, and a new loop starts its
     * own backoff and failure count from zero, which is why network recovery is not meant to
     * continue the old count.
     *
     * Calling this after the brake has fired is the manual retry the brake message asks for, so it
     * clears that state and starts counting again.
     */
    fun reconnectIfNeeded() {
        val host = activeHost ?: return
        val token = Any()
        synchronized(rebuildLock) {
            if (rebuildToken != null) {
                // Merge with the cycle already running. The flag is read again at its tail, under
                // this same lock, so a request that lands here cannot be lost between the check and
                // the release of the token.
                rebuildPending = true
                return
            }
            rebuildToken = token
        }
        scope.launch { runRebuild(host, token) }
    }

    /**
     * Rebuild the connection, one pass per request, for as long as callers keep merging.
     *
     * The client is built *before* the old loop is stopped: building it can fail (a credential that
     * will not decrypt, a relay entry that is half written), and in that case the old loop is still
     * retrying and costs nothing. The swap itself is synchronous and inside [rebuildLock], so a
     * [disconnect] or [connect] that lands while the client is being built wins — it clears the
     * token, and this cycle then stops instead of reconnecting behind the user's back.
     */
    private suspend fun runRebuild(host: HostConfig, token: Any) {
        try {
            while (true) {
                synchronized(rebuildLock) { rebuildPending = false }
                val client = clientFactory.clientFor(host)
                val started = synchronized(rebuildLock) {
                    if (rebuildToken !== token) {
                        false
                    } else {
                        markRetrying()
                        loop?.stop()
                        api = client
                        startLoop(host)
                        true
                    }
                }
                // Superseded by a disconnect or a connect: that call owns the socket now.
                if (!started) return
                // The tail, still under the lock: either a merged caller wants another pass, or the
                // cycle ends and releases its token — so a caller arriving after this point starts
                // its own cycle instead of merging into a finished one.
                val again = synchronized(rebuildLock) {
                    when {
                        rebuildToken !== token -> return
                        rebuildPending -> {
                            rebuildPending = false
                            true
                        }
                        else -> {
                            rebuildToken = null
                            false
                        }
                    }
                }
                if (!again) break
            }
        } finally {
            // Whatever ended the cycle — including an exception out of the factory — the token must
            // not stay current, or no later call could ever start a cycle again.
            synchronized(rebuildLock) { if (rebuildToken === token) rebuildToken = null }
        }
    }

    /**
     * Publish "trying again" before the rebuilt loop exists.
     *
     * The state a brake leaves behind still carries the failure that ended it. Left there, it would
     * contradict the attempt that replaced it for as long as the factory takes to build a client,
     * and worse, it would survive the new loop's own quiet first second — the failure below the
     * reporting threshold is deliberately not rewritten. The phase is the one the loop would pick
     * next: RECONNECTING once something has connected before, CONNECTING before that.
     */
    private fun markRetrying() {
        val current = _state.value
        _state.value = current.copy(
            phase = if (current.hasConnected) {
                ConnectionPhase.RECONNECTING
            } else {
                ConnectionPhase.CONNECTING
            },
            failure = null,
            attempts = 0,
        )
    }

    /**
     * Drop a queued or running rebuild.
     *
     * [disconnect] and [connect] both call this because they take ownership of the socket: a cycle
     * that committed after them would reconnect behind the user's back, which is the "I pressed
     * disconnect and it came back" bug. The brake calls it for the same reason, against the
     * automatic path.
     */
    private fun cancelRebuild() {
        synchronized(rebuildLock) {
            rebuildToken = null
            rebuildPending = false
        }
    }

    /**
     * Build and start one loop, and believe only it from here on.
     *
     * The token is current before [ConnectionLoop.start] is called, which is what schedules the
     * first callback, so no callback can arrive before the manager knows which loop it belongs to.
     *
     * A new loop counts its own failures from one (the alternative — carrying a count across loops
     * — is what would let a flapping network keep a connection permanently in "failed"), so the
     * clock that bounds a run of failures starts over here too.
     */
    /** Whether the peer refused for volume reasons rather than because the host is gone. */
    private fun GenerationFailure.isRateLimited(): Boolean = when (this) {
        is GenerationFailure.MuxFailed -> kind == TransportFailure.RATE_LIMITED
        is GenerationFailure.ReadyFailed -> TransportFailures.of(error) == TransportFailure.RATE_LIMITED
        else -> false
    }

    /**
     * One retry after the limiter has had time to forget us.
     *
     * A single attempt rather than a timer chain: a limiter still tripping after the wait is a
     * configuration problem on the host, and pacing against it from the phone only makes the phone
     * complicit. The user can always press reconnect.
     */
    private fun scheduleRateLimitedRetry() {
        scope.launch {
            delay(RATE_LIMITED_COOLDOWN_MS)
            // Something else may have taken over: a manual reconnect, a host switch, a disconnect.
            if (activeHost != null && _state.value.phase == ConnectionPhase.DISCONNECTED) {
                reconnectIfNeeded()
            }
        }
    }

    private fun startLoop(host: HostConfig) {
        val id = Any()
        val created = ConnectionLoop(muxFactory(host), sinks(id), LoopConfig())
        loop = created
        loopToken = id
        failingSinceMs = 0L
        created.start()
    }

    /**
     * A per-generation mux builder for [host].
     *
     * The loop calls this once per attempt and owns the socket's lifetime, so the credential is
     * re-read on every reconnect rather than baked in once. That matters for the same reason
     * [reconnectIfNeeded] rebuilds its client: a relay token can rotate while the app is
     * backgrounded, and a socket built with the old one is refused at the upgrade.
     */
    private fun muxFactory(host: HostConfig): () -> RemoteStreamMux = {
        kotlinx.coroutines.runBlocking { clientFactory.muxFor(host) }
    }

    /**
     * Whether this failure means retrying is pointless against [host].
     *
     * The relay answers 403 for a missing, expired or revoked credential, and none of those come
     * back on their own — the client integration contract says so outright: "prompt to pair again,
     * do not retry with backoff". A changed certificate is the same kind of fact. The loop's default
     * is to retry forever, which against a relay that revoked this device is a request every few
     * seconds until the app is killed.
     */
    private fun isTerminalForRelay(host: HostConfig, failure: GenerationFailure): Boolean {
        if (!host.isRelay) return false
        val kind = when (failure) {
            is GenerationFailure.MuxFailed -> failure.kind
            is GenerationFailure.ReadyFailed -> TransportFailures.of(failure.error)
            is GenerationFailure.MuxTimedOut -> null
        }
        // UNAUTHENTICATED is terminal for a different reason. Behind a relay it is the harness
        // refusing the relay's own request, not this device's credential — but nothing the phone
        // retries will make the relay sign in; that takes a relay fixed on its own computer.
        return kind == TransportFailure.TRUST_FENCE ||
            kind == TransportFailure.CERTIFICATE_PIN ||
            kind == TransportFailure.UNAUTHENTICATED
    }

    /**
     * Stop the loop and keep [failure] on screen.
     *
     * Not [disconnect]: that resets the whole state object, which would wipe the very explanation
     * the user needs in order to know that pairing again is the fix.
     *
     * [id] is the token of the loop that reported the failure. If a rebuild has already replaced
     * that loop, the report describes a connection nobody is trying to make any more, and stopping
     * the replacement would undo work the user asked for — so a stale report is dropped instead.
     */
    private fun stopRetrying(id: Any, failure: ConnectFailure) {
        if (loopToken !== id) return
        // Dropped before the stop for the same reason as in [disconnect]: a callback already inside
        // `stop()` must not be able to rewrite the state written at the end of this function.
        loopToken = null
        // Nothing may rebuild behind a stop: the brake is not a pause between attempts.
        cancelRebuild()
        loop?.stop()
        loop = null
        failingSinceMs = 0L
        _state.value = _state.value.copy(
            phase = ConnectionPhase.DISCONNECTED,
            // Back to Idle, not left on whatever handshake step the last generation died at. The
            // connect screen derives "still connecting" from the stage, so a stage frozen mid-
            // handshake leaves the Connect button disabled with a spinner that will never finish —
            // which is precisely the failure this screen already learned once.
            stage = ConnectStage.Idle,
            failure = failure,
            attempts = 0,
        )
    }

    /**
     * Bring the foreground service up when the user asked for a background connection.
     *
     * Safe to call at any time and from any state: it re-reads the preference, swallows every failure
     * (Android refuses a background foreground-service start by throwing, and that must not escape
     * into the connection loop) and remembers the refusal so the next foreground moment can retry.
     */
    fun maybeStartService(force: Boolean = false) {
        // `force` is the foreground path: the process may believe the service is up while Android
        // killed it under a background restriction, and coming back to the foreground is the one
        // moment a restart is guaranteed to be allowed.
        if (serviceRunning && !force) return
        val settings = runCatching { runBlockingRead { hostsStore.settingsOnce() } }.getOrNull() ?: return
        if (!settings.keepConnectedInBackground) return
        startService()
    }

    private fun startService() {
        val intent = Intent(context, ConnectionService::class.java)
        val started = runCatching { ContextCompat.startForegroundService(context, intent) }.isSuccess
        serviceRunning = started
        // A refusal is not an error to report: Android says no whenever the app is in the background
        // on 12+, which is precisely the state this service exists for. The activity retries on
        // resume, and the flag makes that retry cheap.
        if (!started) serviceStartDeferred = true
    }

    private fun stopService() {
        runCatching { context.stopService(Intent(context, ConnectionService::class.java)) }
        serviceRunning = false
        serviceStartDeferred = false
    }

    private fun <T> runBlockingRead(block: suspend () -> T): T =
        kotlinx.coroutines.runBlocking { block() }

    private companion object {

        /**
         * Failed generations before [ConnectionUiState.failure] may be written at all.
         *
         * The loop backs off from 500ms, so three of them already mean about a second and a half of
         * trying. One failed handshake is precisely what a Wi-Fi blip and a background network
         * switch look like, and the attempt after them normally succeeds.
         */
        const val REPORT_AFTER_FAILURES = 3

        /**
         * How long a run of failures must last before an error is worth showing.
         *
         * The count alone is not enough: against a host that refuses at once, three failures arrive
         * inside two seconds, and that is still nothing more than a phone choosing a new cell.
         * Eight seconds is longer than a switch observed here takes to settle, and the backoff means
         * the report lands on the fifth or sixth attempt rather than the third.
         */
        const val REPORT_AFTER_MS = 8_000L

        /**
         * Consecutive failures at which automatic reconnecting stops.
         *
         * The loop retries forever by design, so the brake has to live here. With the loop's backoff
         * (500ms doubling to a 10s cap, jittered) twenty failures span roughly two minutes — long
         * enough for an outage worth riding out, short enough that a phone left on a host that is
         * gone overnight is not opening a socket every ten seconds until the battery dies.
         */
        const val STOP_AFTER_FAILURES = 20

        /**
         * How long reconnecting may run before it stops.
         *
         * The wall-clock companion to [STOP_AFTER_FAILURES]: twenty fast failures land near two
         * minutes, but a host that answers slowly makes each attempt cost up to the mux and ready
         * timeouts (8s), and the count alone would then mean five minutes or more. Three minutes
         * bounds what the count cannot.
         */
        const val STOP_AFTER_MS = 3 * 60_000L

        /**
         * What the brake reports.
         *
         * The last failure is not repeated here: it has usually been on screen for a minute or two
         * already, by design, and what the user needs at this point is that retrying has become
         * theirs to do. Reuses [ConnectFailure.Other] so nothing has to be added to the failure
         * enum — or to `strings.xml`, which this file must not touch.
         */
        /**
         * How long to stay quiet after the host answered 429.
         *
         * Longer than the loop's 10 s backoff cap on purpose: a limiter's burst is spent in seconds,
         * but the bucket it fills only drains over a minute.
         */
        const val RATE_LIMITED_COOLDOWN_MS = 60_000L

        /** What the brake reports when the host refuses for volume reasons. */
        val RATE_LIMITED: ConnectFailure = ConnectFailure.Other(
            "The server is rate limiting this phone (HTTP 429). Automatic reconnect paused for a minute.",
        )

        val RETRIES_EXHAUSTED: ConnectFailure = ConnectFailure.Other(
            "Automatic reconnect stopped after repeated failures. Reconnect to try again.",
        )
    }
}

