package com.trungld.studyforielts.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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
    val isDark = isSystemInDarkTheme()
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

    val defaultGlow = if (isDark) Color(0x6600E5FF) else Color(0x330288D1)
    val cardBackgroundBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xF0183144),
                Color(0xEB112433),
                Color(0xE60C1B27),
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xF5FFFFFF),
                Color(0xE8EDF7FD),
                Color(0xDBE3F2FC),
            )
        )
    }

    val cardBorderBrush = if (isDark) {
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
                Color(0xEEFFFFFF),
                Color(0x99B3E5FC),
                Color(0x4D81D4FA),
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation.dp,
                shape = shape,
                ambientColor = accentGlow ?: defaultGlow,
                spotColor = accentGlow ?: defaultGlow,
            )
            .clip(shape)
            .then(
                if (isGlass) {
                    Modifier
                        .background(cardBackgroundBrush)
                        .drawWithContent {
                            drawContent()
                            // Top glass specular reflection band (characteristic of Windows 7 Aero window headers)
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to if (isDark) Color(0x33FFFFFF) else Color(0x66FFFFFF),
                                    0.45f to if (isDark) Color(0x11FFFFFF) else Color(0x22FFFFFF),
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
                brush = cardBorderBrush,
                shape = shape,
            )
            .then(clickableMod),
        content = {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            ) {
                content()
            }
        },
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
    shape: Shape = RoundedCornerShape(14.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val (topColor, bottomColor, contentColor, borderColor, shadowColor) = when (style) {
        AeroButtonStyle.AERO_BLUE -> Quintuple(
            if (isPressed) Color(0xFF0277BD) else (if (isDark) Color(0xFF0288D1) else Color(0xFF1E88E5)),
            if (isPressed) Color(0xFF01579B) else (if (isDark) Color(0xFF01579B) else Color(0xFF1565C0)),
            Color.White,
            if (isDark) Color(0xFF4FC3F7) else Color(0xFF90CAF9),
            if (isDark) Color(0x660288D1) else Color(0x331976D2),
        )
        AeroButtonStyle.NATURE_EMERALD -> Quintuple(
            if (isPressed) Color(0xFF2E7D32) else (if (isDark) Color(0xFF388E3C) else Color(0xFF43A047)),
            if (isPressed) Color(0xFF1B5E20) else (if (isDark) Color(0xFF1B5E20) else Color(0xFF2E7D32)),
            Color.White,
            if (isDark) Color(0xFF81C784) else Color(0xFFA5D6A7),
            if (isDark) Color(0x662E7D32) else Color(0x332E7D32),
        )
        AeroButtonStyle.WARM_AMBER -> Quintuple(
            if (isPressed) Color(0xFFE65100) else (if (isDark) Color(0xFFF57C00) else Color(0xFFFB8C00)),
            if (isPressed) Color(0xFFBF360C) else (if (isDark) Color(0xFFE65100) else Color(0xFFEF6C00)),
            Color.White,
            if (isDark) Color(0xFFFFB74D) else Color(0xFFFFCC80),
            if (isDark) Color(0x66F57C00) else Color(0x33E65100),
        )
        AeroButtonStyle.FROSTED_GLASS -> Quintuple(
            if (isPressed) {
                if (isDark) Color(0xFF1A384D) else Color(0xFFE3F2FD)
            } else {
                if (isDark) Color(0xFF152E40) else Color(0xFFF0F7FD)
            },
            if (isPressed) {
                if (isDark) Color(0xFF102534) else Color(0xFFD6E9F8)
            } else {
                if (isDark) Color(0xFF0F2231) else Color(0xFFE3F0FB)
            },
            if (isDark) Color(0xFF81D4FA) else Color(0xFF0277BD),
            if (isDark) Color(0x4D81D4FA) else Color(0x8090CAF9),
            if (isDark) Color(0x3300E5FF) else Color(0x1A0288D1),
        )
        AeroButtonStyle.RUBY_DANGER -> Quintuple(
            if (isPressed) Color(0xFFC2185B) else (if (isDark) Color(0xFFD81B60) else Color(0xFFE53935)),
            if (isPressed) Color(0xFF880E4F) else (if (isDark) Color(0xFFAD1457) else Color(0xFFC62828)),
            Color.White,
            if (isDark) Color(0xFFF48FB1) else Color(0xFFEF9A9A),
            if (isDark) Color(0x66C2185B) else Color(0x33C62828),
        )
    }

    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 1f else 3f,
        label = "aero_button_elevation",
    )

    val backgroundBrush = if (isPressed) {
        Brush.verticalGradient(listOf(bottomColor, topColor))
    } else {
        // Refined smooth gloss gradient with subtle top glow
        Brush.verticalGradient(
            0.00f to topColor,
            0.45f to topColor,
            0.55f to bottomColor,
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
            .background(
                if (enabled) backgroundBrush 
                else if (isDark) Brush.verticalGradient(listOf(Color(0xFF263238), Color(0xFF1E272C)))
                else Brush.verticalGradient(listOf(Color(0xFFEEEEEE), Color(0xFFE0E0E0)))
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        borderColor.copy(alpha = if (isPressed) 0.5f else 0.9f),
                        borderColor.copy(alpha = if (isPressed) 0.2f else 0.4f),
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
                if (enabled && !isPressed && style != AeroButtonStyle.FROSTED_GLASS) {
                    // Refined subtle top specular sheen (soft linear curve rather than harsh cutout)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x40FFFFFF),
                                Color(0x05FFFFFF),
                            )
                        ),
                        topLeft = Offset(2f, 1.5f),
                        size = Size(size.width - 4f, size.height * 0.46f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                    )
                }
            }
            .padding(contentPadding)
            .defaultMinSize(minHeight = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) contentColor else if (isDark) Color(0xFF78909C) else Color(0xFF9E9E9E)
        ) {
            ProvideTextStyle(
                value = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) contentColor else if (isDark) Color(0xFF78909C) else Color(0xFF9E9E9E),
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
