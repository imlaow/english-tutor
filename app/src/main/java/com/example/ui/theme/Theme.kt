package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * The handoff ships one palette, so the theme is pinned to it: no `darkTheme`
 * branch and no dynamic color, or the wallpaper would overwrite the design.
 */
private val WarmColorScheme =
  lightColorScheme(
    // mic FAB
    primary = Accent500,
    onPrimary = Neutral100,
    // topic number badges, the "Y" learner avatar
    primaryContainer = Accent200,
    onPrimaryContainer = Accent700,
    inversePrimary = Accent300,

    secondary = Accent2500,
    onSecondary = Neutral100,
    // the "T" tutor avatar, exchange-count pill, "TRY SAYING" card
    secondaryContainer = Accent2200,
    onSecondaryContainer = Accent2700,

    tertiary = Accent600,
    onTertiary = Neutral100,
    tertiaryContainer = Accent100,
    onTertiaryContainer = Accent800,

    background = WarmBackground,
    onBackground = WarmText,

    // cards and chat bubbles
    surface = Neutral100,
    onSurface = WarmText,
    // top bar and mic dock
    surfaceVariant = WarmSurface,
    onSurfaceVariant = Neutral700,
    surfaceTint = Accent500,
    surfaceBright = Neutral100,
    surfaceDim = Neutral300,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral100,
    surfaceContainer = Neutral200,
    surfaceContainerHigh = Neutral200,
    surfaceContainerHighest = Neutral300,

    inverseSurface = Neutral900,
    inverseOnSurface = Neutral100,

    // The design has no error color; accent-700 is a scorched orange that still
    // reads as a warning without dropping a foreign red into the palette.
    error = Accent700,
    onError = Neutral100,
    errorContainer = Accent200,
    onErrorContainer = Accent800,

    // Neutral-600, not the neutral-400 the handoff draws its hairlines with.
    // `outline` is the unfocused border of every OutlinedTextField, and the four
    // screens with no design are nothing but forms, so it has to clear the 3:1
    // WCAG floor for a UI component boundary. Measured against the surfaces it is
    // actually drawn on: neutral-400 gives 1.68:1 on the background (#f5ead8),
    // 1.83:1 on a card (#f9f4ed); neutral-500 only reaches 2.42:1 / 2.63:1;
    // neutral-600 is the first step on the ramp that passes — 3.61:1 and 3.92:1
    // (3.21:1 on the top bar's #ebddc5). The hairlines the design does specify
    // are `outlineVariant`, which keeps the handoff's own divider value.
    outline = Neutral600,
    outlineVariant = WarmDivider,
  )

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = WarmColorScheme,
    typography = Typography,
    shapes = Shapes,
    content = content,
  )
}
