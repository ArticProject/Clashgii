package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val GraphiteColorScheme = darkColorScheme(
    primary = ActionLight,
    onPrimary = ActionLightInk,
    primaryContainer = GraphiteActive,
    onPrimaryContainer = InkPrimary,
    secondary = ElixirAccent,
    onSecondary = ActionLightInk,
    secondaryContainer = ElixirAccentMuted,
    onSecondaryContainer = InkPrimary,
    tertiary = InkSecondary,
    onTertiary = GraphiteBg,
    background = GraphiteBg,
    onBackground = InkPrimary,
    surface = GraphiteSurface,
    onSurface = InkPrimary,
    surfaceVariant = GraphiteRaised,
    onSurfaceVariant = InkSecondary,
    outline = GraphiteHairline,
    outlineVariant = GraphiteHairlineStrong,
    error = StatusDanger,
    onError = GraphiteBg,
    errorContainer = StatusDangerSurface,
    onErrorContainer = StatusDanger
)

// unslop Pass 7: Nested radii (controls 8dp, cards 12dp, panels 16dp)
val TacticalShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun RoyalePulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GraphiteColorScheme,
        typography = AppTypography,
        shapes = TacticalShapes,
        content = content
    )
}
