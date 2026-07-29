package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The redesign's serif/sans split.
 *
 * `displayLarge` → `titleLarge` are [FontFamily.Serif]; `titleMedium` →
 * `labelSmall` are [FontFamily.Default]. Serif is chrome — screen titles,
 * headings, section headers — never conversation: `bodyLarge` carries chat
 * message text and deliberately stays sans at 16sp/24sp.
 *
 * [FontFamily.Serif] resolves to the platform serif (Noto Serif on every device
 * at `minSdk 24`), so this costs zero font assets and zero dependencies —
 * `res/font/` stays empty.
 *
 * Headings tighten `letterSpacing` to -0.3.sp and carry their own
 * [FontWeight.SemiBold], which is why call sites no longer pass an ad-hoc
 * `fontWeight = FontWeight.Bold`. Body styles relax `lineHeight` to ~1.55x the
 * font size for longer-form reading.
 */
val Typography =
  Typography(
    // ----- Serif: display -----
    displayLarge =
      TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.3).sp,
      ),
    displayMedium =
      TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.3).sp,
      ),
    displaySmall =
      TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.3).sp,
      ),
    // ----- Serif: headline -----
    headlineLarge =
      TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.3).sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
      ),
    // ----- Serif: the boundary style. TopAppBar titles land here. -----
    titleLarge =
      TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
      ),
    // ----- Sans: titles -----
    titleMedium =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
      ),
    // ----- Sans: body. bodyLarge is chat message text. -----
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.4.sp,
      ),
    // ----- Sans: labels. labelLarge is button text. -----
    labelLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
      ),
    // SemiBold rather than M3's Medium: this style carries the "Grammar
    // Correction" header in ChatBubble, which lost its ad-hoc Bold to the scale.
    labelMedium =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
      ),
  )
