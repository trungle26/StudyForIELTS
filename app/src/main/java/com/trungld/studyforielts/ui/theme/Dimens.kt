package com.trungld.studyforielts.ui.theme

import androidx.compose.ui.unit.dp

// Design system constants. Every spacer/padding/radius in the app should
// reference one of these. Add a new tier here before adding a new magic number
// to a screen.

object Dimens {
    // --- Spacing ---
    val SpacingTiny = 2.dp
    val SpacingXs = 4.dp
    val SpacingSm = 8.dp
    val SpacingMd = 16.dp
    val SpacingLg = 24.dp
    val SpacingXl = 32.dp

    // Aliases for the most common usages.
    val ContentPaddingSmall = SpacingSm
    val ContentPadding = SpacingMd
    val ContentPaddingLarge = SpacingLg

    // Compact icon tile (used for chart thumbnails in lesson lists).
    val IconTileSize = 48.dp

    // Large icon tile (used for celebration icons, hero markers).
    val IconTileSizeLarge = 56.dp

    // Compact circular indicator (small loading spinner in lists).
    val SmallSpinnerSize = 24.dp

    // Writing: essay textarea minimum height, chart image min/max height.
    val WritingEssayMin = 200.dp
    val WritingChartMin = 160.dp
    val WritingChartMax = 320.dp

    // --- Corner radii (3 tiers) ---
    val CornerSmall = 8.dp
    val CornerMedium = 16.dp
    val CornerLarge = 28.dp

    // --- Elevation ---
    val CardElevation = 1.dp
    val ElevatedCardElevation = 3.dp

    // --- Surface alpha (for semi-transparent overlays) ---
    const val SurfaceAlpha = 0.5f

    // --- Standard content max width (tablet-friendly) ---
    val ContentMaxWidth = 600.dp
}
