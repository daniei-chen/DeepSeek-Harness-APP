package com.labteto.dshmobile.ui.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items



import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.text.style.TextOverflow
import com.labteto.dshmobile.ui.components.FeatherIcons
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.labteto.dshmobile.R
import com.labteto.dshmobile.core.session.AssistantMessageNode
import com.labteto.dshmobile.core.session.ConversationSnapshot
import com.labteto.dshmobile.core.session.UserMessageNode
import com.labteto.dshmobile.ui.components.DsButton
import com.labteto.dshmobile.ui.components.DsButtonSize
import com.labteto.dshmobile.ui.components.DsButtonVariant
import com.labteto.dshmobile.ui.components.EmptyHero
import com.labteto.dshmobile.ui.components.skeleton
import com.labteto.dshmobile.ui.theme.DsTheme
import com.labteto.dshmobile.ui.theme.DsType

/**
 * How close to the far end of the list — the oldest message — a reader must get before the next
 * page is fetched.
 *
 * Deliberately not zero: firing a row or two early means the page is on its way before the reader
 * reaches the end of what is loaded, so scrolling back stays continuous rather than stopping dead
 * at a spinner.
 *
 * In reverse layout this no longer has a second job. The list is anchored at the newest message,
 * older ones extend away from that anchor, and a page arriving at the far end cannot move anything
 * the reader is looking at — which is what the pre-0.11.2 comment here was working around.
 */
private const val LOAD_OLDER_THRESHOLD = 2

/**
 * How many pages the transcript may fetch on its own before it needs to be asked.
 *
 * Paging used to be automatic without limit while the transcript was shorter than the screen. That
 * reads as reasonable and is not: a page is counted in *events*, and most events — chunk deltas,
 * tool traffic, turn boundaries — render nothing at all. A session whose log is mostly machinery
 * therefore never fills the screen however much is loaded, so the fill loop pulled the entire
 * history in, four thousand events at a time, re-folding everything already held on each pass until
 * the heap gave out.
 *
 * One extra page is the whole of what the fill is for. A page carries up to sixty messages, which
 * is several screens' worth already; if it still does not reach the bottom of the viewport then the
 * session's log is mostly machinery, and pulling more of it is buying thousands more events for a
 * row or two. Past that the reader asks, via the row at the head of the list.
 */
private const val MAX_AUTO_PAGES = 1

/** Pages fetched back-to-back to get past a rewind's hidden range before a row appears. */
private const val MAX_CUT_PAGES = 12

/**
 * The conversation itself.
 *
 * The list is laid out in reverse: the newest row is index 0, pinned at the bottom of the
 * viewport, and older rows extend upward from it. That is what makes following a streaming reply
 * free. A normally-ordered list anchors on its *first* visible row, so a tail row growing 20 times
 * a second displaces every row after it, and each displacement was being animated — the text under
 * the reader's eyes jittered, and a scroll was needed every frame just to stay at the bottom.
 * Reversed, the growing row is the anchor: it extends in place, nothing else moves, and no scroll
 * happens at all.
 *
 * It also makes reading history mid-turn stable for free. Scrolled up, the reader's anchor sits
 * above index 0, so the tail can grow as much as it likes without touching the viewport — no
 * freezing the tail and catching up later. And a page of older messages lands at the far end,
 * where it cannot move anything on screen.
 */
@Composable
internal fun ChatTranscript(
    conversation: ConversationSnapshot?,
    loading: Boolean,
    loadingOlder: Boolean,
    loadOlderFailed: Boolean,
    context: ChatNodeContext,
    listState: LazyListState,
    onLoadOlder: () -> Unit,
    modifier: Modifier = Modifier,
    jumpToSeq: Long? = null,
    onJumpHandled: () -> Unit = {},
) {
    // Only the nodes that draw something: a zero-height item still costs its 4dp gap, and a turn's
    // worth of structural events stacks those gaps into a blank band under the chrome.
    //
    // Reversed here rather than at the call site: the fold's natural order is oldest-first, and
    // that is the order every other reader of `nodes` wants.
    val rows = remember(conversation?.nodes, context.running) {
        groupToolRows(renderableChatNodes(conversation?.nodes.orEmpty(), context.running)).asReversed()
    }
    val hasMore = conversation?.hasMore == true
    val itemCount = rows.size + if (hasMore) 1 else 0
    val sessionId = conversation?.sessionId
    // Turn navigation from the top bar: the reversed list is newest-first, so the first row at or
    // before the requested turn start is the visual top of that turn.
    LaunchedEffect(jumpToSeq) {
        val target = jumpToSeq ?: return@LaunchedEffect
        val offset = if (context.running) 1 else 0
        val index = rows.indexOfFirst { it.seq <= target }
        if (index >= 0) listState.animateScrollToItem(index + offset)
        onJumpHandled()
    }    // "Deep diving" elapsed time. Take the newest turn start from the loaded events; a session
    // resumed from history may have none, in which case the newest event time is the best anchor.
    val runningTurnStart = remember(conversation?.nodes) {
        conversation?.nodes?.lastOrNull { it is com.labteto.dshmobile.core.session.TurnStartNode }
    }
    val runStartedAt = remember(runningTurnStart, context.eventTimes) {
        runningTurnStart?.let { context.eventTimes[it.seq] } ?: context.eventTimes.values.maxOrNull()
    }
    var runningNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(context.running) {
        while (context.running) {
            runningNow = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000)
        }
    }

    // Opening a session lands on its newest message. In reverse layout that is index 0, which is
    // also where the list starts, so this only has to undo a position inherited from the session
    // that was open before. Nothing else scrolls the transcript any more: following the tail costs
    // no scroll, which is the point of the reversal.
    var lastSession by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(sessionId) {
        if (sessionId == lastSession) return@LaunchedEffect
        lastSession = sessionId
        if (itemCount > 0) listState.scrollToItem(0)
    }

    // Reaching the oldest loaded message pulls the next page. The guard matters: this effect sits
    // above the `loading` early return, so without it the trigger would fire against an empty list
    // and race the initial history fetch. It also re-arms once a page lands, which is what fills
    // the first screen when a session opens on fewer messages than the viewport holds.
    //
    // Two different things want a page, and only one of them is safe to repeat without limit.
    // Scrolling back to the oldest loaded row is the reader asking, and can page as far back as
    // they care to go.
    // Filling a screen that the transcript does not yet cover is the app asking, and is bounded by
    // MAX_AUTO_PAGES — a page that adds thousands of events and no visible rows would otherwise
    // keep the app asking forever.
    var autoPages by rememberSaveable(sessionId) { mutableIntStateOf(0) }
    val canPage = hasMore && !loading && !loadingOlder && !loadOlderFailed
    val autoPagingExhausted = hasMore && !loading && autoPages >= MAX_AUTO_PAGES

    // A rewind hides everything from its target through its marker. A page that lands entirely
    // inside that range adds events and no rows, so a tap on "Load older" looks like it did
    // nothing. When the oldest row loaded — the top of the transcript — is a rewind divider and a
    // page just landed without changing the row count, keep paging (bounded) until something
    // above the cut appears.
    val rowCount = rows.size
    val dividerOnTop = (rows.lastOrNull() as? TranscriptRow.Message)?.node.let { it is UserMessageNode && it.isRewindMarker }
    var lastPagedRowCount by remember(sessionId) { mutableIntStateOf(-1) }
    var cutPages by remember(sessionId) { mutableIntStateOf(0) }
    LaunchedEffect(rowCount, loadingOlder, hasMore, dividerOnTop) {
        if (loadingOlder || !hasMore || loadOlderFailed || !dividerOnTop) return@LaunchedEffect
        if (lastPagedRowCount == -1) { lastPagedRowCount = rowCount; return@LaunchedEffect }
        if (rowCount != lastPagedRowCount) { lastPagedRowCount = rowCount; cutPages = 0; return@LaunchedEffect }
        if (cutPages >= MAX_CUT_PAGES) return@LaunchedEffect
        cutPages++
        onLoadOlder()
    }
    LaunchedEffect(listState, sessionId, canPage) {
        if (!canPage) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val covered = info.visibleItemsInfo.sumOf { it.size }
            val viewport = info.viewportEndOffset - info.viewportStartOffset
            // The oldest row is at the *end* of a reversed list, so distance-to-history is measured
            // from the last visible index rather than the first.
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val remaining = if (last < 0) Int.MAX_VALUE else info.totalItemsCount - 1 - last
            remaining to (viewport > 0 && covered >= viewport)
        }.collect { (remaining, fillsViewport) ->
            if (remaining > LOAD_OLDER_THRESHOLD) return@collect
            if (!fillsViewport) {
                if (autoPages >= MAX_AUTO_PAGES) return@collect
                autoPages++
            }
            onLoadOlder()
        }
    }

    if (loading) {
        // A session opens at its newest message, so the load affordance belongs at the bottom:
        // the skeleton and spinner rise from the composer instead of appearing at the top and
        // being scrolled away the moment history lands.
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = DsTheme.colors.labelTertiary,
                    )
                    Text(
                        stringResource(R.string.common_loading),
                        style = DsType.caption11,
                        color = DsTheme.colors.labelTertiary,
                    )
                }
                TranscriptSkeleton()
            }
        }
        return
    }

    // A blank session's hero sits in the middle of the transcript area. Left inside the reversed
    // list it was bottom-aligned, which put the greeting near the composer instead of centre.
    if (rows.isEmpty() && !hasMore && !context.running) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyHero(
                headline = stringResource(R.string.chat_empty_title),
                subtitle = stringResource(R.string.chat_empty_hint),
            )
        }
        return
    }

    val colors = DsTheme.colors

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier.widthIn(max = 920.dp).fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        // Newest first, so the row the viewport anchors on is the one that grows.
        reverseLayout = true,
        // Still bottom-aligned: a transcript shorter than the viewport belongs above the composer,
        // not pinned under the tab strip with the empty half below it. The alignment is in visual
        // space, not the reversed one, so this reads the same as it always did — and it only has
        // any effect while the content is shorter than the viewport, which is exactly when no row
        // is growing under anyone's eyes.
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
    ) {
        // The desktop client keeps a "deep diving" line just above the composer while a turn
        // runs. Reverse layout puts this first item at the visual bottom, which is the same spot.
        if (context.running) {
            item(key = "deep-diving") {
                val elapsed = (runningNow - (runStartedAt ?: runningNow)).coerceAtLeast(0)
                val elapsedText = if (elapsed >= 60_000) {
                    stringResource(
                        R.string.duration_min_sec,
                        (elapsed / 60_000).toInt(),
                        ((elapsed / 1_000) % 60).toInt(),
                    )
                } else {
                    stringResource(R.string.duration_sec, (elapsed / 1_000).toInt())
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        FeatherIcons.Compass,
                        contentDescription = null,
                        tint = DsTheme.colors.accent,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        stringResource(R.string.chat_deep_diving_for, elapsedText),
                        style = DsType.small13,
                        color = DsTheme.colors.labelTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (rows.isEmpty()) {
            item(key = "empty") {
                EmptyHero(
                    headline = stringResource(R.string.chat_empty_title),
                    subtitle = stringResource(R.string.chat_empty_hint),
                )
            }
        } else {
            items(
                rows,
                // A streaming row's seq is minted past the durable cursor and is explicitly not
                // stable across folds, so keying on it destroyed and rebuilt the row the moment the
                // settlement landed — a visible pop at the end of every reply. One constant key
                // instead: there is only ever one provisional row, and it is the same row before
                // and after it settles.
                key = { row ->
                    val node = (row as? TranscriptRow.Message)?.node
                    if (node is AssistantMessageNode && node.streaming) STREAMING_ROW_KEY else row.seq
                },
                // Reuse slots by row kind: a run of messages should not recycle a tool row's
                // composition, and vice versa. Keys still identify individual rows.
                contentType = { row ->
                    when (row) {
                        is TranscriptRow.Message -> "message"
                        is TranscriptRow.Tools -> "tools"
                        is TranscriptRow.Context -> "context"
                    }
                },
            ) { row ->
                val node = (row as? TranscriptRow.Message)?.node
                val streaming = node is AssistantMessageNode && node.streaming
                // Placement animation is for rows that move. The streaming row grows in place many
                // times a second, and animating that reads as jitter rather than motion.
                Column(if (streaming) Modifier else Modifier.animateItem()) {
                    when (row) {
                        is TranscriptRow.Message -> ChatNodeItem(node = row.node, context = context)
                        // The original client lists every tool call as its own compact row; a
                        // "工具调用：4" group header told the reader a count and nothing about
                        // what ran. Keep the grouping fold for semantics, draw it flat.
                        is TranscriptRow.Tools -> Column(Modifier.fillMaxWidth()) {
                            row.calls.forEach { call -> ToolCallRow(call, context) }
                        }
                        is TranscriptRow.Context -> ContextGroupRow(row.nodes, context)
                    }
                }
            }
        }
        if (hasMore) {
            // The far end of a reversed list is the oldest message, so the paging row goes last.
            item(key = "load-older") {
                LoadOlderRow(
                    loading = loadingOlder,
                    failed = loadOlderFailed,
                    offerManual = autoPagingExhausted,
                    onRetry = onLoadOlder,
                )
            }
        }
        }
    }
}

/** Stable identity for the one provisional streaming row; see the keying note above. */
private const val STREAMING_ROW_KEY = "streaming-tail"

/**
 * Tail of the reversed list — visually the top of the transcript — while more history exists.
 *
 * Silent by default — paging is automatic, so an affordance would only invite a tap that does
 * nothing. It speaks up while fetching, and offers a retry when a page failed, because the scroll
 * trigger will not fire again on its own until the reader moves.
 *
 * [offerManual] is the third case: automatic paging has spent its budget on a session whose events
 * are mostly not messages, so the list may still be too short to scroll. Without a button there
 * would be nothing left to trigger a page, and the rest of the history would be unreachable.
 */
@Composable
private fun LoadOlderRow(
    loading: Boolean,
    failed: Boolean,
    offerManual: Boolean,
    onRetry: () -> Unit,
) {
    val colors = DsTheme.colors
    when {
        failed -> DsButton(
            text = stringResource(R.string.chat_load_older_retry),
            onClick = onRetry,
            variant = DsButtonVariant.Ghost,
            size = DsButtonSize.Small,
            modifier = Modifier.fillMaxWidth(),
        )

        offerManual && !loading -> DsButton(
            text = stringResource(R.string.chat_load_older),
            onClick = onRetry,
            variant = DsButtonVariant.Ghost,
            size = DsButtonSize.Small,
            modifier = Modifier.fillMaxWidth(),
        )

        loading -> Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = colors.labelTertiary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.chat_loading_older),
                style = DsType.caption11,
                color = colors.labelTertiary,
            )
        }

        else -> Spacer(Modifier.height(1.dp))
    }
}

/** Placeholder bubbles while a session's history loads, instead of an empty white screen. */
@Composable
private fun TranscriptSkeleton(modifier: Modifier = Modifier) {
    val colors = DsTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(0.55f, 0.9f, 0.75f, 0.4f).forEach { fraction ->
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(14.dp)
                    .skeleton(colors.bgLayer2, colors.hover),
            )
        }
    }
}




