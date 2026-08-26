package com.trungld.studyforielts.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Skeuomorphic & Aero Glass styling kit reminiscent of Windows 7 Aero Glass
 * and TouchWiz Nature UX (Samsung Galaxy S3 / S4 era):
 * - Specular glossy top reflections / split highlights
 * - Embossed bevel inner borders & light source highlights
 * - Skeuomorphic tactile pill / orb buttons with physical pressed depression
 * - Glassmorphic panels with frosted cyan/nature glow
 */

enum class AeroButtonStyle {
    AERO_BLUE,       // Windows 7 glassy cyan/sky blue
    NATURE_EMERALD,  // TouchWiz organic leaf green
    WARM_AMBER,      // Sunlit amber / orange gloss
    FROSTED_GLASS,   // Translucent water/glass button
    RUBY_DANGER,     // Glossy crimson/ruby
}

/**
 * High-authenticity Aero/TouchWiz Glass Card container.
 */
@Composable
fun AeroCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    isGlass: Boolean = true,
    accentGlow: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else 6f,
        label = "aero_card_elevation",
    )

    val clickableMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    } else Modifier

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation.dp,
                shape = shape,
                ambientColor = accentGlow ?: Color(0x330288D1),
                spotColor = accentGlow ?: Color(0x2B01579B),
            )
            .clip(shape)
            .then(
                if (isGlass) {
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xF5FFFFFF),
                                    Color(0xE8EDF7FD),
                                    Color(0xDBE3F2FC),
                                )
                            )
                        )
                        .drawWithContent {
                            drawContent()
                            // Top glass specular reflection band (characteristic of Windows 7 Aero window headers)
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color(0x66FFFFFF),
                                    0.45f to Color(0x22FFFFFF),
                                    0.46f to Color(0x00FFFFFF),
                                    1.0f to Color(0x00FFFFFF),
                                ),
                                topLeft = Offset.Zero,
                                size = Size(size.width, size.height),
                            )
                        }
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surface)
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xEEFFFFFF),
                        Color(0x99B3E5FC),
                        Color(0x4D81D4FA),
                    )
                ),
                shape = shape,
            )
            .then(clickableMod),
        content = content,
    )
}

/**
 * Tactile Skeuomorphic Button with 3D split-gradient gloss, bevel border,
 * and realistic pressed-depth animation.
 */
@Composable
fun AeroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AeroButtonStyle = AeroButtonStyle.AERO_BLUE,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val (topColor, bottomColor, contentColor, borderColor, shadowColor) = when (style) {
        AeroButtonStyle.AERO_BLUE -> Quintuple(
            if (isPressed) Color(0xFF0277BD) else Color(0xFF29B6F6),
            if (isPressed) Color(0xFF01579B) else Color(0xFF0288D1),
            Color.White,
            Color(0xFF81D4FA),
            Color(0x660288D1),
        )
        AeroButtonStyle.NATURE_EMERALD -> Quintuple(
            if (isPressed) Color(0xFF2E7D32) else Color(0xFF66BB6A),
            if (isPressed) Color(0xFF1B5E20) else Color(0xFF388E3C),
            Color.White,
            Color(0xFFA5D6A7),
            Color(0x662E7D32),
        )
        AeroButtonStyle.WARM_AMBER -> Quintuple(
            if (isPressed) Color(0xFFE65100) else Color(0xFFFFA726),
            if (isPressed) Color(0xFFBF360C) else Color(0xFFFB8C00),
            Color.White,
            Color(0xFFFFCC80),
            Color(0x66F57C00),
        )
        AeroButtonStyle.FROSTED_GLASS -> Quintuple(
            if (isPressed) Color(0xCCDCEDF8) else Color(0xFAFFFFFF),
            if (isPressed) Color(0xBBD0E7F5) else Color(0xD8E6F3FA),
            Color(0xFF01579B),
            Color(0xEEFFFFFF),
            Color(0x3381D4FA),
        )
        AeroButtonStyle.RUBY_DANGER -> Quintuple(
            if (isPressed) Color(0xFFC2185B) else Color(0xFFE91E63),
            if (isPressed) Color(0xFF880E4F) else Color(0xFFC2185B),
            Color.White,
            Color(0xFFF48FB1),
            Color(0x66C2185B),
        )
    }

    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 1f else 4f,
        label = "aero_button_elevation",
    )

    val backgroundBrush = if (isPressed) {
        Brush.verticalGradient(listOf(bottomColor, topColor))
    } else {
        // Classic Aero / Frutiger glass split-reflection specular shine
        Brush.verticalGradient(
            0.00f to topColor,
            0.48f to topColor.copy(alpha = 0.95f),
            0.49f to Color(0x40FFFFFF),
            0.50f to bottomColor,
            1.00f to bottomColor,
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (enabled) elevation.dp else 0.dp,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor,
            )
            .clip(shape)
            .background(if (enabled) backgroundBrush else Brush.verticalGradient(listOf(Color(0xFFE0E0E0), Color(0xFFBDBDBD))))
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.5f),
                    )
                ),
                shape = shape,
            )
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier
            )
            .drawWithContent {
                drawContent()
                if (enabled && !isPressed) {
                    // Top crescent reflection
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x99FFFFFF),
                                Color(0x00FFFFFF),
                            )
                        ),
                        topLeft = Offset(4f, 2f),
                        size = Size(size.width - 8f, size.height * 0.45f),
                    )
                }
            }
            .padding(contentPadding)
            .defaultMinSize(minHeight = 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) contentColor else Color(0xFF757575)
        ) {
            ProvideTextStyle(
                value = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) contentColor else Color(0xFF757575),
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }
}

// Tuple helper
private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
)
