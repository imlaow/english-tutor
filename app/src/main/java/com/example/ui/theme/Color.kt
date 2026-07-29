package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Classic / elegant palette: warm paper in light, warm charcoal in dark, terracotta as the single
 * accent. Tokens are named by the role they play, not by hue number.
 *
 * The primary is `#B4552F` rather than the more common `#C15F3C`: white on `#C15F3C` measures
 * 4.24:1, which misses WCAG AA (4.5:1) for the 14sp `labelLarge` used by every filled button.
 * `#B4552F` measures 4.93:1 and is visually the same terracotta. Do not lighten it.
 */

// ── Primary — terracotta ──────────────────────────────────────────────────────
val TerracottaPrimaryLight = Color(0xFFB4552F)
val OnTerracottaPrimaryLight = Color(0xFFFFFFFF)
val TerracottaContainerLight = Color(0xFFF5E5DE)
val OnTerracottaContainerLight = Color(0xFF4A2013)

val TerracottaPrimaryDark = Color(0xFFD97757)
val OnTerracottaPrimaryDark = Color(0xFF3A1B0E)
val TerracottaContainerDark = Color(0xFF4A2A1E)
val OnTerracottaContainerDark = Color(0xFFF5E0D6)

// ── Secondary — warm taupe ────────────────────────────────────────────────────
val TaupeSecondaryLight = Color(0xFF6E5B4E)
val OnTaupeSecondaryLight = Color(0xFFFFFFFF)
val TaupeContainerLight = Color(0xFFF0EEE6)
val OnTaupeContainerLight = Color(0xFF2A2622)

val TaupeSecondaryDark = Color(0xFFD4C4B5)
val OnTaupeSecondaryDark = Color(0xFF3A2E26)
val TaupeContainerDark = Color(0xFF33322F)
val OnTaupeContainerDark = Color(0xFFEDE7DF)

// ── Tertiary — manilla, used for margin-note surfaces such as grammar corrections ─
val ManillaTertiaryLight = Color(0xFF8A6A45)
val OnManillaTertiaryLight = Color(0xFFFFFFFF)
val ManillaContainerLight = Color(0xFFEBDBBC)
val OnManillaContainerLight = Color(0xFF3A2C16)

val ManillaTertiaryDark = Color(0xFFD4A27F)
val OnManillaTertiaryDark = Color(0xFF3A2A18)
val ManillaContainerDark = Color(0xFF4A3A24)
val OnManillaContainerDark = Color(0xFFEDDCC2)

// ── Neutrals — warm paper / warm charcoal ─────────────────────────────────────
val PaperBackgroundLight = Color(0xFFFAF9F5)
val OnPaperBackgroundLight = Color(0xFF1F1E1D)
val PaperSurfaceVariantLight = Color(0xFFE9E6DC)
val OnPaperSurfaceVariantLight = Color(0xFF5C5A54)
val PaperOutlineLight = Color(0xFF8C8880)
val PaperOutlineVariantLight = Color(0xFFDDD9CE)

val CharcoalBackgroundDark = Color(0xFF262624)
val OnCharcoalBackgroundDark = Color(0xFFF5F4EF)
val CharcoalSurfaceVariantDark = Color(0xFF3A3A38)
val OnCharcoalSurfaceVariantDark = Color(0xFFB4B0A6)
val CharcoalOutlineDark = Color(0xFF6E6B64)
val CharcoalOutlineVariantDark = Color(0xFF46453F)

// ── Surface container ramp ────────────────────────────────────────────────────
val PaperContainerLowestLight = Color(0xFFFFFFFF)
val PaperContainerLowLight = Color(0xFFF5F3EC)
val PaperContainerLight = Color(0xFFF0EEE6)
val PaperContainerHighLight = Color(0xFFEAE8DF)
val PaperContainerHighestLight = Color(0xFFE4E2D8)
val PaperSurfaceDimLight = Color(0xFFE8E6DF)
val PaperSurfaceBrightLight = Color(0xFFFAF9F5)

val CharcoalContainerLowestDark = Color(0xFF1A1A19)
val CharcoalContainerLowDark = Color(0xFF222220)
val CharcoalContainerDark = Color(0xFF2B2B29)
val CharcoalContainerHighDark = Color(0xFF30302E)
val CharcoalContainerHighestDark = Color(0xFF3A3A38)
val CharcoalSurfaceDimDark = Color(0xFF1C1B1A)
val CharcoalSurfaceBrightDark = Color(0xFF3A3A38)

/**
 * Error is crimson rather than the M3 default `#B3261E`: the default red sits at hue ~4° and
 * terracotta at ~17°, so side by side (recording mic FAB vs. idle mic FAB) they read as the same
 * color. `#9F2D3A` is pushed toward magenta so the two are unmistakably distinct.
 */
// ── Error — crimson ───────────────────────────────────────────────────────────
val CrimsonErrorLight = Color(0xFF9F2D3A)
val OnCrimsonErrorLight = Color(0xFFFFFFFF)
val CrimsonErrorContainerLight = Color(0xFFFADDDF)
val OnCrimsonErrorContainerLight = Color(0xFF410E15)

val CrimsonErrorDark = Color(0xFFF2A0A8)
val OnCrimsonErrorDark = Color(0xFF5A1620)
val CrimsonErrorContainerDark = Color(0xFF7A2231)
val OnCrimsonErrorContainerDark = Color(0xFFFFDADD)

// ── Inverse + scrim ───────────────────────────────────────────────────────────
val InverseSurfaceLight = Color(0xFF33322F)
val InverseOnSurfaceLight = Color(0xFFF5F3EC)
val InversePrimaryLight = Color(0xFFD97757)

val InverseSurfaceDark = Color(0xFFE4E2D8)
val InverseOnSurfaceDark = Color(0xFF33322F)
val InversePrimaryDark = Color(0xFFB4552F)

val Scrim = Color(0xFF000000)

// ── Extended colors ───────────────────────────────────────────────────────────

/**
 * Material 3 has no `success` slot, but the app needs one for the connection probe. Reach these
 * through [MaterialTheme.extended] rather than scattering raw `Color(0x…)` literals into screens.
 */
@Immutable
data class ExtendedColors(
  val success: Color,
  val onSuccess: Color,
  val successContainer: Color,
  val onSuccessContainer: Color,
)

val LightExtendedColors =
  ExtendedColors(
    success = Color(0xFF3F6B4A),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD8EBDC),
    onSuccessContainer = Color(0xFF0E2A16),
  )

val DarkExtendedColors =
  ExtendedColors(
    success = Color(0xFF8FCB9B),
    onSuccess = Color(0xFF12331C),
    successContainer = Color(0xFF26452D),
    onSuccessContainer = Color(0xFFD8EBDC),
  )

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Companion to [MaterialTheme.colorScheme] for slots Material 3 does not define. */
val MaterialTheme.extended: ExtendedColors
  @Composable @ReadOnlyComposable get() = LocalExtendedColors.current
