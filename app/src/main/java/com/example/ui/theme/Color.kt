package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens from `styles.css` in the `_ds/organic-…` bundle of
 * `design_handoff_english_tutor_ui`.
 *
 * The palette is warm and organic with a single (light) scheme — see [MyApplicationTheme].
 * Names mirror the CSS custom properties so a token in the handoff can be grepped here.
 */

// --color-bg / --color-surface / --color-text / --color-divider
val WarmBackground = Color(0xFFF5EAD8)
val WarmSurface = Color(0xFFEBDDC5)
val WarmText = Color(0xFF201E1D)

/** `color-mix(in srgb, #201e1d 16%, transparent)` — 16% of 255 is 41 (0x29). */
val WarmDivider = Color(0x29201E1D)

// --color-neutral-100 … 900
val Neutral100 = Color(0xFFF9F4ED)
val Neutral200 = Color(0xFFEEE7DB)
val Neutral300 = Color(0xFFDCD3C4)
val Neutral400 = Color(0xFFC0B6A5)
val Neutral500 = Color(0xFFA19786)
val Neutral600 = Color(0xFF82796A)
val Neutral700 = Color(0xFF645C50)
val Neutral800 = Color(0xFF474238)
val Neutral900 = Color(0xFF2E2B25)

// --color-accent-100 … 900 (burnt orange)
val Accent100 = Color(0xFFFFF2EB)
val Accent200 = Color(0xFFFFE1D0)
val Accent300 = Color(0xFFFFC6A5)
val Accent400 = Color(0xFFF6A06B)
val Accent500 = Color(0xFFD67F48)
val Accent600 = Color(0xFFB2622D)
val Accent700 = Color(0xFF8C491A)
val Accent800 = Color(0xFF643312)
val Accent900 = Color(0xFF402310)

// --color-accent-2-100 … 900 (olive)
val Accent2100 = Color(0xFFF0FAE1)
val Accent2200 = Color(0xFFE1EECC)
val Accent2300 = Color(0xFFCCDBB2)
val Accent2400 = Color(0xFFAEBF92)
val Accent2500 = Color(0xFF8FA073)
val Accent2600 = Color(0xFF728157)
val Accent2700 = Color(0xFF56633F)
val Accent2800 = Color(0xFF3D472B)
val Accent2900 = Color(0xFF272E1B)
