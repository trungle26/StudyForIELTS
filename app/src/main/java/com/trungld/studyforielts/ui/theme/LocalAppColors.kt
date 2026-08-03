package com.trungld.studyforielts.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Semantic feedback colors that aren't part of MaterialTheme.colorScheme.
// These are *application* color roles (correct/wrong/missing/extra, swipe intents)
// that still respect light/dark via the theme. Access via `AppTheme.colors.wrongAmber`.

data class AppColors(
    val correctGreen: Color,
    val correctGreenContainer: Color,
    val wrongAmber: Color,
    val wrongAmberContainer: Color,
    val missingRed: Color,
    val missingRedContainer: Color,
    val extraBlue: Color,
    val extraBlueContainer: Color,
    val swipeLearned: Color,
    val swipeLearnedContainer: Color,
    val swipeReview: Color,
    val swipeReviewContainer: Color,
)

internal fun lightAppColors(): AppColors = AppColors(
    correctGreen = CorrectGreenLight,
    correctGreenContainer = CorrectGreenContainerLight,
    wrongAmber = WrongAmberLight,
    wrongAmberContainer = WrongAmberContainerLight,
    missingRed = MissingRedLight,
    missingRedContainer = MissingRedContainerLight,
    extraBlue = ExtraBlueLight,
    extraBlueContainer = ExtraBlueContainerLight,
    swipeLearned = SwipeLearnedLight,
    swipeLearnedContainer = SwipeLearnedContainerLight,
    swipeReview = SwipeReviewLight,
    swipeReviewContainer = SwipeReviewContainerLight,
)

internal fun darkAppColors(): AppColors = AppColors(
    correctGreen = CorrectGreenDark,
    correctGreenContainer = CorrectGreenContainerDark,
    wrongAmber = WrongAmberDark,
    wrongAmberContainer = WrongAmberContainerDark,
    missingRed = MissingRedDark,
    missingRedContainer = MissingRedContainerDark,
    extraBlue = ExtraBlueDark,
    extraBlueContainer = ExtraBlueContainerDark,
    swipeLearned = SwipeLearnedDark,
    swipeLearnedContainer = SwipeLearnedContainerDark,
    swipeReview = SwipeReviewDark,
    swipeReviewContainer = SwipeReviewContainerDark,
)

// CompositionLocal wired by Theme.kt via CompositionLocalProvider.
val AppColorsLocal = compositionLocalOf<AppColors> {
    error("AppColorsLocal not provided. Wrap content in StudyForIELTSTheme.")
}

// Convenience accessor. Usage: `AppTheme.colors.wrongAmber`
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = AppColorsLocal.current
}
