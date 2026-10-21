package com.labteto.dshmobile.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import com.labteto.dshmobile.ui.theme.DsTheme
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.labteto.dshmobile.R
import com.labteto.dshmobile.ui.components.DsToastHost
import com.labteto.dshmobile.ui.components.rememberDsToast
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.labteto.dshmobile.ui.theme.DsAnimations
import kotlinx.coroutines.launch

/**
 * Discord-style shell:
 *  - swipe right from the LEFT edge (or anywhere on the content) opens the chat-list drawer
 *    (ModalNavigationDrawer's built-in gesture; swipe left on the drawer
 *    content closes it, scrim tap and Back also work)
 *  - swipe left from the RIGHT edge opens the session Details panel
 *  - swipe right anywhere on the open Details panel closes it
 *
 * The details gesture detector only claims the drags it owns (leftward from the right edge
 * band, or any drag on the details area while it is open) and leaves every other horizontal
 * drag unconsumed. ModalNavigationDrawer puts an anchoredDraggable(Horizontal) on the whole
 * surface, so an always-consuming detector here would starve the drawer's open gesture.
 * Horizontal edge drags are axis-orthogonal to the chat list's vertical scroll, so the two
 * never conflict.
 */
@Composable
fun MainScreen() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var automationsOpen by remember { mutableStateOf(false) }
    var automationSession by remember { mutableStateOf<String?>(null) }
    val store = com.labteto.dshmobile.ui.rememberSessionStore()
    val automationAvailable by store.automationAvailable.collectAsStateWithLifecycle()
    val toast = rememberDsToast()
    val unsupportedAutomation = stringResource(R.string.ux_b_automation_unavailable)
    LaunchedEffect(automationAvailable, automationsOpen) {
        if (automationAvailable == false && automationsOpen) {
            automationsOpen = false
            automationSession = null
            toast.second(unsupportedAutomation)
        }
    }
    var detailsOpen by remember { mutableStateOf(false) }
    val detailsWidth = 300.dp

    // Web-replica shell: 0 = chat (the app), 1 = full-screen sessions list, 2 = settings hub.
    // Projects/tasks are gone; workspaces are rows in the sessions list and the built-in pages
    // (models / general / plugins / agents) live under Settings.
    var mainTab by rememberSaveable { mutableIntStateOf(0) }
    val sessionsListState = rememberLazyListState()
    val currentSessionId by store.currentSessionId.collectAsStateWithLifecycle()
    val sessions by store.sessions.collectAsStateWithLifecycle()
    var autoSessionInFlight by remember { mutableStateOf(false) }
    // A web page always has a composer. When the restored conversation has no session yet, open a
    // blank one (or reuse an existing blank session) instead of showing a dead input.
    LaunchedEffect(currentSessionId, sessions.size, mainTab) {
        if (mainTab == 0 && currentSessionId == null && !autoSessionInFlight) {
            autoSessionInFlight = true
            try {
                val blank = sessions.firstOrNull { it.blank && it.origin != "subagent" }
                if (blank != null) store.openSession(blank.sessionId) else store.createSession()
            } finally {
                autoSessionInFlight = false
            }
        }
    }
    // Android back: a secondary tab returns to the chat; sub-pages intercept it first.
    BackHandler(enabled = mainTab != 0) { mainTab = 0 }

    // Fork: a notification tap carries a session id; consume it as soon as this screen exists and
    // the connection is up, opening that chat.
    val pendingSession by com.labteto.dshmobile.ui.IncomingSession.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pendingSession) {
        val sessionId = pendingSession ?: return@LaunchedEffect
        store.openSession(sessionId)
        mainTab = 0
        com.labteto.dshmobile.ui.IncomingSession.consume()
    }

    if (mainTab != 0) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (mainTab) {
                    1 -> AutomationScreen(sessionId = null, onClose = { mainTab = 0 })
                    2 -> FileBrowserScreen()
                    else -> MeScreen(onOpenChat = { mainTab = 0 })
                }
            }
            MainBottomBar(selected = mainTab, onSelect = { mainTab = it })
        }
        return
    }

    Scaffold(bottomBar = { MainBottomBar(selected = 0, onSelect = { mainTab = it }) }) { inner ->
    Box(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatListDrawer(
                onClose = { scope.launch { drawerState.close() } },
                onOpenSettings = { mainTab = 3; scope.launch { drawerState.close() } },
                listState = sessionsListState,
                onOpenAutomations = { mainTab = 1; scope.launch { drawerState.close() } },
            )
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(detailsOpen) {
                    val width = size.width.toFloat()
                    val edgeBandPx = 28.dp.toPx()
                    val detailsAreaPx = detailsWidth.toPx() * 0.9f
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        // Claim only gestures this screen handles: leftward drags starting in
                        // the right edge band open details; drags starting on the open details
                        // panel close it. Everything else (notably left-to-right swipes) must
                        // stay unconsumed for the drawer's built-in open gesture.
                        val owned = if (!detailsOpen) {
                            startX >= width - edgeBandPx
                        } else {
                            startX <= detailsAreaPx
                        }
                        if (!owned) return@awaitEachGesture

                        var claimed = false
                        awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                            // While closed, only a leftward drag belongs to this screen; a
                            // rightward drag from the edge is the drawer's to open with.
                            claimed = detailsOpen || overSlop < 0f
                            if (claimed) change.consume()
                        } ?: return@awaitEachGesture
                        if (!claimed) return@awaitEachGesture

                        var totalX = 0f
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUpIgnoreConsumed()) break
                            // A deeper scrollable claimed the drag; let it keep it.
                            if (change.isConsumed) break
                            totalX += change.positionChange().x
                            change.consume()
                            val threshold = width * 0.12f
                            if (!detailsOpen && totalX <= -threshold) {
                                detailsOpen = true
                                break
                            }
                            if (detailsOpen && totalX >= threshold) {
                                detailsOpen = false
                                break
                            }
                        }
                    }
                },
        ) {
            // The chat softens under the open panel: a blur where the platform renders one (API 31+;
            // below that `blur` is a no-op and the scrim alone does the work).
            val blur by animateDpAsState(if (detailsOpen) 6.dp else 0.dp, tween(220, easing = FastOutSlowInEasing), label = "detailsBlur")
            Box(Modifier.fillMaxSize().blur(blur)) {
                ChatScreen(
                    onOpenDetails = { detailsOpen = true },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    detailsOpen = detailsOpen,
                )
            }

            // Tap anywhere outside the panel to close it — the same contract as the drawer's
            // scrim. The drag-to-close gesture above still works; this adds the tap.
            AnimatedVisibility(
                visible = detailsOpen,
                enter = fadeIn(DsAnimations.fade),
                exit = fadeOut(DsAnimations.fade),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(DsTheme.colors.overlayMask)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = stringResource(R.string.common_close),
                        ) { detailsOpen = false },
                )
            }

            AnimatedVisibility(
                visible = detailsOpen,
                // Explicit spec: the platform default runs 300ms, which lags behind the drag the
                // panel is usually opened with.
                enter = slideInHorizontally(DsAnimations.panelSlide) { it },
                exit = slideOutHorizontally(DsAnimations.panelSlide) { it },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                DetailsPanel(
                    onClose = { detailsOpen = false },
                    onOpenAutomations = { automationSession = store.currentSessionId.value; automationsOpen = true; detailsOpen = false },
                    modifier = Modifier.width(detailsWidth),
                )
            }
            DsToastHost(toast, Modifier.align(Alignment.TopCenter))
        }
    }
    }
    }
    if (automationsOpen && automationAvailable != false) AutomationScreen(automationSession, onClose = { automationsOpen = false })
}









