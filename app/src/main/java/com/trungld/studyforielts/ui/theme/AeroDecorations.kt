package com.trungld.studyforielts.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Aero & Nature UX styling helpers for Windows 7 Aero Glass / TouchWiz Nature aesthetics.
 * Provides glossy glassmorphism brushes, realistic light reflections, gradients, and soft glow borders.
 */
object AeroDecorations {
    // Translucent Aero Glass background gradient
    @Composable
    fun glassBrush(
        isDark: Boolean = false,
        elevation: Float = 1f,
    ): Brush {
        return if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xCC1A384C),
                    Color(0x990E2232),
                    Color(0xAA142E40),
                ),
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xF5FFFFFF),
                    Color(0xD9E9F5FD),
                    Color(0xCCDCF0FA),
                ),
            )
        }
    }

    // Glossy button top highlight brush (simulates glassy sheen reflection)
    fun glossyButtonBrush(
        topColor: Color,
        bottomColor: Color,
        highlightColor: Color = Color(0x66FFFFFF),
    ): Brush {
        return Brush.verticalGradient(
            0.0f to topColor,
            0.48f to topColor,
            0.50f to bottomColor,
            1.0f to bottomColor,
        )
    }

    // Glass rim border
    @Composable
    fun glassBorderBrush(isDark: Boolean = false): Brush {
        return if (isDark) {
            Brush.verticalGradient(
                listOf(
                    Color(0x8080D8FF),
                    Color(0x3340C4FF),
                    Color(0x1A0091EA),
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(0xCCFFFFFF),
                    Color(0x80B3E5FC),
                    Color(0x4081D4FA),
                )
            )
        }
    }
}

/**
 * Extension modifier to turn any standard Card / Box into an Aero Glass panel
 * with top-edge reflection, soft inner/outer glow, and rounded gloss.
 */
fun Modifier.aeroGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 4.dp,
    isDark: Boolean = false,
): Modifier = composed {
    this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = if (isDark) Color(0x6600E5FF) else Color(0x330288D1),
            spotColor = if (isDark) Color(0x4D00B0FF) else Color(0x2601579B),
        )
        .clip(shape)
        .background(AeroDecorations.glassBrush(isDark = isDark))
        .border(
            width = borderWidth,
            brush = AeroDecorations.glassBorderBrush(isDark = isDark),
            shape = shape,
        )
}
