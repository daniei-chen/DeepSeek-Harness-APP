package com.labteto.dshmobile.ui.screens.main

/**
 * The one-line tool summary: the tool's own summary plus, when known, its elapsed time.
 *
 * Kept pure and tested because the two pieces come from different projections (the card and the
 * event timestamps); rendering them as two separate lines used to leave an orphan duration line
 * between transcript rows.
 */
internal fun toolSummaryWithDuration(summary: String?, elapsed: String?): String? =
    listOfNotNull(summary?.takeIf { it.isNotBlank() }, elapsed?.takeIf { it.isNotBlank() })
        .joinToString(" · ")
        .ifBlank { null }
