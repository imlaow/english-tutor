package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.example.R

/** Display face. Caprasimo ships a single weight (400). */
val Caprasimo = FontFamily(Font(R.font.caprasimo_regular, FontWeight.Normal))

/** Text face. */
val Figtree =
  FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_semibold, FontWeight.SemiBold),
    Font(R.font.figtree_bold, FontWeight.Bold),
  )

/**
 * The handoff is a web design, and the browser box model is what the spacing was
 * eyeballed against. Compose defaults to `includeFontPadding = true`, which adds
 * uneven slack above and below every line that CSS has no equivalent for, so each
 * gap would land a few dp larger than drawn. Trimming it plus centering within the
 * line box makes `lineHeight` behave like CSS `line-height`.
 */
private val WebLikeMetrics =
  TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle =
      LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
      ),
  )

/**
 * Headings: Caprasimo, `line-height: 1.12`, `letter-spacing: -0.015em`.
 * `em` is relative to font size, so the sp value differs per step.
 */
private fun heading(sizeSp: Float) =
  WebLikeMetrics.copy(
    fontFamily = Caprasimo,
    fontWeight = FontWeight.Normal,
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp * 1.12f).sp,
    letterSpacing = (sizeSp * -0.015f).sp,
  )

/** Body: Figtree, `line-height: 1.55`, no tracking. */
private fun body(sizeSp: Float, weight: FontWeight = FontWeight.Normal, letterSpacingSp: Float = 0f) =
  WebLikeMetrics.copy(
    fontFamily = Figtree,
    fontWeight = weight,
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp * 1.55f).sp,
    letterSpacing = letterSpacingSp.sp,
  )

/**
 * Handoff scale: h1 42 / h2 32 / h3 25 / h4 20 / h5 16 / h6 13, body base 15.
 * Mapped onto the M3 roles the screens actually reach for — the two top-bar
 * title sizes (23 for Home/History/Settings, 20 for Chat) live in title*.
 */
val Typography =
  Typography(
    displayLarge = heading(42f),
    displayMedium = heading(32f),
    displaySmall = heading(25f),
    headlineLarge = heading(32f),
    headlineMedium = heading(25f),
    headlineSmall = heading(20f),
    titleLarge = heading(23f),
    titleMedium = heading(20f),
    // Not a heading: every 16px in the handoff (topic card title, Settings row
    // title) is 600 with no font-family override, so it inherits --font-body.
    titleSmall = body(16f, FontWeight.SemiBold),
    bodyLarge = body(15f),
    bodyMedium = body(14f),
    bodySmall = body(13f),
    labelLarge = body(13f, FontWeight.SemiBold),
    labelMedium = body(11f, FontWeight.SemiBold),
    // `.08em @10sp = 0.8sp` — the uppercase micro-labels ("TUTOR", "TRY SAYING").
    labelSmall = body(10f, FontWeight.SemiBold, letterSpacingSp = 0.8f),
  )

/**
 * The uppercase section kickers ("SUGGESTED FOR YOU", "MODEL"): 11sp/600 tracked
 * out to `.14em` = 1.54sp. Kept off [Typography.labelMedium] because the pills
 * that share its size and weight — provider, exchange count, history date — are
 * sentence case with no tracking at all.
 */
val SectionKicker = Typography.labelMedium.copy(letterSpacing = 1.54.sp)
