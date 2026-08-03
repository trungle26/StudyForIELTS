package com.trungld.studyforielts.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

// Material3 shape scale. The three tiers match Material 3's component intent:
//   small  — buttons, chips, small surfaces
//   medium — cards, dialogs, snackbars
//   large  — bottom sheets, large modals
//
// Wire into MaterialTheme via StudyForIELTSTheme.
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Dimens.CornerSmall / 2),
    small = RoundedCornerShape(Dimens.CornerSmall),
    medium = RoundedCornerShape(Dimens.CornerMedium),
    large = RoundedCornerShape(Dimens.CornerLarge),
    extraLarge = RoundedCornerShape(Dimens.CornerLarge),
)
