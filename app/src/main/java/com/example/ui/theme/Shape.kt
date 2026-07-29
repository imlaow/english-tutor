package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii for the redesign. Moderate rounding reads classic; nothing is pill-shaped except the
 * components Material shapes as `CornerFull` (buttons, the mic FAB), which do not read from here.
 *
 * Named `AppShapes` rather than `Shapes` so it never shadows [androidx.compose.material3.Shapes] in
 * a file that imports both — the same collision `Typography` in `Type.kt` already lives with.
 */
val AppShapes =
  Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
  )
