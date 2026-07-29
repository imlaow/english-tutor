package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme =
  lightColorScheme(
    primary = TerracottaPrimaryLight,
    onPrimary = OnTerracottaPrimaryLight,
    primaryContainer = TerracottaContainerLight,
    onPrimaryContainer = OnTerracottaContainerLight,
    secondary = TaupeSecondaryLight,
    onSecondary = OnTaupeSecondaryLight,
    secondaryContainer = TaupeContainerLight,
    onSecondaryContainer = OnTaupeContainerLight,
    tertiary = ManillaTertiaryLight,
    onTertiary = OnManillaTertiaryLight,
    tertiaryContainer = ManillaContainerLight,
    onTertiaryContainer = OnManillaContainerLight,
    background = PaperBackgroundLight,
    onBackground = OnPaperBackgroundLight,
    surface = PaperBackgroundLight,
    onSurface = OnPaperBackgroundLight,
    surfaceVariant = PaperSurfaceVariantLight,
    onSurfaceVariant = OnPaperSurfaceVariantLight,
    outline = PaperOutlineLight,
    outlineVariant = PaperOutlineVariantLight,
    surfaceContainerLowest = PaperContainerLowestLight,
    surfaceContainerLow = PaperContainerLowLight,
    surfaceContainer = PaperContainerLight,
    surfaceContainerHigh = PaperContainerHighLight,
    surfaceContainerHighest = PaperContainerHighestLight,
    surfaceDim = PaperSurfaceDimLight,
    surfaceBright = PaperSurfaceBrightLight,
    error = CrimsonErrorLight,
    onError = OnCrimsonErrorLight,
    errorContainer = CrimsonErrorContainerLight,
    onErrorContainer = OnCrimsonErrorContainerLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    scrim = Scrim,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = TerracottaPrimaryDark,
    onPrimary = OnTerracottaPrimaryDark,
    primaryContainer = TerracottaContainerDark,
    onPrimaryContainer = OnTerracottaContainerDark,
    secondary = TaupeSecondaryDark,
    onSecondary = OnTaupeSecondaryDark,
    secondaryContainer = TaupeContainerDark,
    onSecondaryContainer = OnTaupeContainerDark,
    tertiary = ManillaTertiaryDark,
    onTertiary = OnManillaTertiaryDark,
    tertiaryContainer = ManillaContainerDark,
    onTertiaryContainer = OnManillaContainerDark,
    background = CharcoalBackgroundDark,
    onBackground = OnCharcoalBackgroundDark,
    surface = CharcoalBackgroundDark,
    onSurface = OnCharcoalBackgroundDark,
    surfaceVariant = CharcoalSurfaceVariantDark,
    onSurfaceVariant = OnCharcoalSurfaceVariantDark,
    outline = CharcoalOutlineDark,
    outlineVariant = CharcoalOutlineVariantDark,
    surfaceContainerLowest = CharcoalContainerLowestDark,
    surfaceContainerLow = CharcoalContainerLowDark,
    surfaceContainer = CharcoalContainerDark,
    surfaceContainerHigh = CharcoalContainerHighDark,
    surfaceContainerHighest = CharcoalContainerHighestDark,
    surfaceDim = CharcoalSurfaceDimDark,
    surfaceBright = CharcoalSurfaceBrightDark,
    error = CrimsonErrorDark,
    onError = OnCrimsonErrorDark,
    errorContainer = CrimsonErrorContainerDark,
    onErrorContainer = OnCrimsonErrorContainerDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    scrim = Scrim,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+, but it would repaint the app from the user's
  // wallpaper and discard the palette above. Off by default.
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }
  val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

  CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
