package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * `--radius-sm/md/lg` = 8/16/28. The handoff's rounded-frame override pushes cards
 * to 32, which lands on [Shapes.extraLarge]; buttons, tags and inputs are pills and
 * ask for [androidx.compose.foundation.shape.CircleShape] at the call site instead.
 */
val Shapes =
  Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
  )
