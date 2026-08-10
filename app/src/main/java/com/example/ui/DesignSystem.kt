package com.example.ui

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.Accent200
import com.example.ui.theme.Accent300
import com.example.ui.theme.Accent500
import com.example.ui.theme.Accent700
import com.example.ui.theme.Neutral100
import com.example.ui.theme.Neutral700
import com.example.ui.theme.Neutral800

/**
 * Shared building blocks for the warm/organic re-theme.
 *
 * The top bar and the mic dock appear on more than one screen, and the pills and
 * avatar circles appear on nearly all of them, so they live here rather than being
 * re-typed per screen. Everything is presentation only: no component holds a string
 * of its own, so all copy stays with the screen that owns the data.
 *
 * Sizes come from the handoff's "各屏尺寸规格" and are in dp (the design canvas is
 * 393px wide, which is the dp width it was drawn for).
 */

/** `--radius-lg`, applied to the two corners that face the content. */
private val BarCornerRadius = 28.dp

// ---------------------------------------------------------------- top bar

/**
 * The surface block behind every screen's title, with its lower corners rounded.
 *
 * The background is painted before [statusBarsPadding] so the surface runs up
 * underneath the status bar — the activity is edge-to-edge, and the design has the
 * bar's color reaching the top of the screen.
 *
 * @param titleStyle 23sp for Home/History/Settings, 20sp for Chat — pass
 *   `titleMedium` there. Always one line; long chat titles ellipsize.
 * @param subtitle the pill under the title (provider on Home, exchange count on
 *   Chat). Laid out with a 6dp gap, per the design.
 * @param navigation the leading 44dp button — back, or the gear on Home.
 */
@Composable
fun WarmTopBar(
  title: String,
  modifier: Modifier = Modifier,
  titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
  subtitle: @Composable (() -> Unit)? = null,
  navigation: @Composable (() -> Unit)? = null,
  actions: @Composable RowScope.() -> Unit = {},
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .background(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(bottomStart = BarCornerRadius, bottomEnd = BarCornerRadius),
        )
        .statusBarsPadding()
        .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 18.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    navigation?.invoke()
    Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
      Text(
        text = title,
        style = titleStyle,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (subtitle != null) {
        Spacer(Modifier.height(6.dp))
        subtitle()
      }
    }
    actions()
  }
}

/**
 * The 44dp circular tap target used for every top-bar action.
 *
 * @param iconSize the design varies this per glyph: 21 for the gear, history and
 *   back arrows, 22 for the plus, 20 for the trash.
 * @param size 44 everywhere in the top bars. The design reuses the same circle —
 *   transparent until pressed, then accent-200 — at smaller diameters inside the
 *   content: 24 for the tutor's read-aloud button, 26 for the history card's
 *   delete button.
 */
@Composable
fun IconButton44(
  icon: Painter,
  contentDescription: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  iconSize: Dp = 21.dp,
  tint: Color = Neutral800,
  size: Dp = 44.dp,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  Box(
    modifier =
      modifier
        .size(size)
        .clip(CircleShape)
        .background(if (pressed) Accent200 else Color.Transparent)
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          role = Role.Button,
          onClick = onClick,
        ),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painter = icon,
      contentDescription = contentDescription,
      tint = if (pressed) Accent700 else tint,
      modifier = Modifier.size(iconSize),
    )
  }
}

// ---------------------------------------------------------------- mic dock

/**
 * The bottom dock holding the record button, shared by Home and Chat.
 *
 * @param size 88 on Home, 76 on Chat.
 * @param iconSize 34 on Home, 30 on Chat.
 * @param elevation `--shadow-lg` ≈ 16dp on Home, `--shadow-md` ≈ 6dp on Chat.
 * @param label the visible caption; also the button's accessibility description,
 *   so the copy stays owned by the caller.
 * @param buttonModifier applied to the record button itself rather than the dock,
 *   so a caller's test tag lands on the thing that is tapped.
 */
@Composable
fun MicDock(
  size: Dp,
  iconSize: Dp,
  isRecording: Boolean,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  buttonModifier: Modifier = Modifier,
  elevation: Dp = 16.dp,
) {
  // The design's 26dp bottom gap already stands in for the home indicator, so the
  // window inset replaces it when it is larger instead of stacking on top of it.
  val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .background(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(topStart = BarCornerRadius, topEnd = BarCornerRadius),
        )
        .padding(top = 18.dp, bottom = maxOf(26.dp, navBarInset)),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
      if (isRecording) PulseRing()
      Box(
        modifier =
          buttonModifier
            .fillMaxSize()
            .shadow(elevation, CircleShape)
            .background(if (isRecording) Accent700 else Accent500, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          painter = painterResource(if (isRecording) R.drawable.ic_stop else R.drawable.ic_mic),
          contentDescription = label,
          tint = Neutral100,
          modifier = Modifier.size(iconSize),
        )
      }
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (isRecording) Accent700 else Neutral700,
      textAlign = TextAlign.Center,
    )
  }
}

/**
 * `@keyframes ring { 0% { scale(1); opacity:.5 } 100% { scale(1.6); opacity:0 } }`
 * at `1.6s ease-out infinite`. Drawn under the button and scaled past its own
 * bounds, so it must not be clipped by the dock.
 */
@Composable
private fun PulseRing() {
  val transition = rememberInfiniteTransition(label = "mic-pulse")
  val spec = infiniteRepeatable<Float>(tween(1600, easing = EaseOut), RepeatMode.Restart)
  val scale by transition.animateFloat(1f, 1.6f, spec, label = "scale")
  val alpha by transition.animateFloat(0.5f, 0f, spec, label = "alpha")
  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
        .background(Accent300, CircleShape)
  )
}

// ---------------------------------------------------------------- small parts

/**
 * The rounded tag used for the provider name, the exchange count and the history
 * date. The three differ only in padding and trimming, so those are parameters.
 *
 * @param leadingDot the 6dp status dot in front of the chat exchange count.
 * @param dotColor accent-2-500 there, a shade darker than the label it precedes.
 * @param border the provider tag is the only outlined one.
 * @param trailingIcon the provider tag's 12dp chevron.
 */
@Composable
fun Pill(
  text: String,
  backgroundColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
  leadingDot: Boolean = false,
  dotColor: Color = contentColor,
  border: BorderStroke? = null,
  trailingIcon: Painter? = null,
  contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
) {
  Row(
    modifier =
      modifier
        .clip(CircleShape)
        .background(backgroundColor)
        .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
        .padding(contentPadding),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(if (leadingDot) 6.dp else 5.dp),
  ) {
    if (leadingDot) {
      Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape))
    }
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = contentColor)
    if (trailingIcon != null) {
      Icon(
        painter = trailingIcon,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(12.dp),
      )
    }
  }
}

/**
 * The lettered circle standing in for a speaker — "T" for the tutor, "Y" for the
 * learner — and, at 28dp, the topic card's position number.
 *
 * @param fontSize defaults to half the diameter, which is what the 20dp and 18dp
 *   avatars use; the 28dp number badge passes 12sp explicitly.
 */
@Composable
fun AvatarBadge(
  letter: String,
  backgroundColor: Color,
  contentColor: Color,
  size: Dp,
  modifier: Modifier = Modifier,
  fontSize: TextUnit = (size.value * 0.5f).sp,
) {
  Box(
    modifier = modifier.size(size).background(backgroundColor, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = letter,
      style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
      fontSize = fontSize,
      fontWeight = FontWeight.Bold,
      color = contentColor,
    )
  }
}
