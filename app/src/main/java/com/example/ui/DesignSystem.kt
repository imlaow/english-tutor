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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.Accent100
import com.example.ui.theme.Accent200
import com.example.ui.theme.Accent2700
import com.example.ui.theme.Accent300
import com.example.ui.theme.Accent500
import com.example.ui.theme.Accent700
import com.example.ui.theme.FieldLabel
import com.example.ui.theme.Neutral100
import com.example.ui.theme.Neutral600
import com.example.ui.theme.Neutral700
import com.example.ui.theme.Neutral800
import com.example.ui.theme.SectionKicker
import java.util.Locale

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

// ------------------------------------------------------------- form controls

/**
 * `.field > label` — the caption above a control: 12sp at 70% of the text color.
 *
 * Used by [WarmTextField] and, on its own, above the controls that have no label
 * slot of their own (the spec picker, the level chips).
 */
@Composable
fun WarmFieldLabel(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    style = FieldLabel,
    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    modifier = modifier,
  )
}

/**
 * `.field` — a labelled text input drawn as `.input`: a pill (the rounded-frame
 * override turns every `.input` into `border-radius: 999px`), filled with
 * `--color-surface`, hairlined with `--color-divider`, and switching that
 * hairline to the accent while focused.
 *
 * The label is a block above the box rather than Material's floating one: the
 * handoff puts it there, and a notch cut into a pill's border has nowhere to sit.
 * It is repeated as the field's content description so the input still announces
 * itself, which the floating label used to do.
 *
 * [modifier] lands on the input itself, not on the label column, so a caller's
 * test tag stays on the thing that is typed into.
 */
@Composable
fun WarmTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isError: Boolean = false,
  singleLine: Boolean = false,
  placeholder: String? = null,
  supportingText: String? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    WarmFieldLabel(label)
    Spacer(Modifier.height(5.dp))
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      enabled = enabled,
      isError = isError,
      singleLine = singleLine,
      keyboardOptions = keyboardOptions,
      // `.input` is 14px, which is bodyMedium; the default would be bodyLarge.
      textStyle = MaterialTheme.typography.bodyMedium,
      placeholder =
        placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
      supportingText = supportingText?.let { { Text(it) } },
      shape = CircleShape,
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedTextColor = MaterialTheme.colorScheme.onBackground,
          unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
          // `background: var(--color-surface)` — the top bar's fill, not the card's.
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          cursorColor = MaterialTheme.colorScheme.primary,
          // The handoff draws this hairline in `--color-divider`, which is ink at
          // 16% and lands at 1.37:1 on the fill — a border you have to hunt for.
          // `outline` is the same ramp two steps darker and clears 3:1, so the
          // control's own edge uses it and `outlineVariant` stays for the rules
          // that separate content. Accent on focus, as drawn.
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline,
          disabledBorderColor = MaterialTheme.colorScheme.outline,
        ),
      modifier = modifier.fillMaxWidth().semantics { contentDescription = label },
    )
  }
}

/**
 * `.radio .dot` — a 16dp circle, hairlined while unselected and filled with the
 * accent while selected.
 *
 * The selected state is CSS's `box-shadow: inset 0 0 0 4px var(--color-bg)` over
 * an accent fill: a 1.5dp accent rim, a 4dp ring of the page color, then a 5dp
 * accent center. Drawn as three nested circles, since an inset shadow has no
 * Compose equivalent.
 *
 * The signature mirrors [androidx.compose.material3.RadioButton] — including the
 * 48dp interactive footprint — so the rows it sits in keep their layout and their
 * selectable semantics. Pass a null [onClick] when the row itself handles the tap.
 */
@Composable
fun WarmRadio(
  selected: Boolean,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val accent = MaterialTheme.colorScheme.primary
  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .then(
          if (onClick != null) {
            Modifier.selectable(
              selected = selected,
              enabled = enabled,
              role = Role.RadioButton,
              onClick = onClick,
            )
          } else {
            Modifier
          }
        )
        // `.btn:disabled { opacity: 0.45 }` — the only disabled treatment the
        // handoff defines, reused here since `.radio` has none of its own.
        .alpha(if (enabled) 1f else 0.45f),
    contentAlignment = Alignment.Center,
  ) {
    if (selected) {
      Box(
        modifier = Modifier.size(16.dp).background(accent, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier
              .size(13.dp)
              .background(MaterialTheme.colorScheme.background, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Box(modifier = Modifier.size(5.dp).background(accent, CircleShape))
        }
      }
    } else {
      Box(
        modifier =
          Modifier
            .size(16.dp)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
      )
    }
  }
}

/**
 * `.seg-opt` — one option of a single-choice control: 13sp, `7px 12px`, filled
 * with the accent and lettered in the page color once picked.
 *
 * [shape] and [border] are open because the handoff uses the same option two
 * ways: standing alone as a pill with its own hairline (the level chips), or
 * butted together inside a `.seg`, where the hairline and the rounding belong to
 * the group and each option is square.
 */
@Composable
fun ChoicePill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  shape: Shape = CircleShape,
  // `outline`, not the handoff's `--color-divider`: standing alone this hairline
  // is the control's own edge, and at 16% ink it is barely findable. See
  // [WarmTextField]. Inside a `.seg` the caller passes null and the group draws it.
  border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
) {
  Box(
    modifier =
      modifier
        .alpha(if (enabled) 1f else 0.45f)
        .clip(shape)
        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
        .then(if (border != null) Modifier.border(border, shape) else Modifier)
        .selectable(
          selected = selected,
          enabled = enabled,
          role = Role.RadioButton,
          onClick = onClick,
        )
        .padding(horizontal = 12.dp, vertical = 7.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color =
        if (selected) MaterialTheme.colorScheme.background
        else MaterialTheme.colorScheme.onBackground,
      maxLines = 1,
    )
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

// ------------------------------------------------------------- grouped card

/** `--radius-lg` — the same 28dp the topic and history cards use. */
val SettingsCardShape = RoundedCornerShape(28.dp)

/** `--shadow-sm` (`0 1 2` @14%) as a Compose elevation. */
private val ShadowSm = 2.dp

/**
 * The scroll container every settings-shaped screen puts its cards in: the
 * handoff's 18dp gutter, and the 26dp that keeps the first card clear of the top
 * bar's rounded corners rather than tucked under them.
 *
 * A plain scroll rather than a `LazyColumn`, because a card has to know where it
 * ends to round itself off — these lists are a handful of saved profiles, not a
 * feed.
 */
@Composable
fun SettingsCardColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(horizontal = 18.dp, vertical = 26.dp),
    content = content,
  )
}

/**
 * The rounded, shadowed group the settings hub and the provider screens hang
 * their rows in.
 */
@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        // `shadow` clips to its shape at any non-zero elevation, which is the
        // card's `overflow: hidden`: without it a pressed row would paint its
        // tint square over the rounded corners.
        .shadow(ShadowSm, SettingsCardShape)
        .background(MaterialTheme.colorScheme.surface),
    content = content,
  )
}

/**
 * The hairline between two rows of a [SettingsCard].
 *
 * @param indent how far in it starts, so it runs under the text rather than under
 *   the row's leading circle or radio (`margin-left: 70px` in the handoff).
 */
@Composable
fun SettingsCardDivider(indent: Dp) {
  HorizontalDivider(
    modifier = Modifier.padding(start = indent),
    thickness = 1.dp,
    color = MaterialTheme.colorScheme.outlineVariant,
  )
}

/** The uppercase label above a [SettingsCard]. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text.uppercase(Locale.US),
    style = SectionKicker,
    color = Accent2700,
    modifier = modifier,
  )
}

/**
 * One row of a [SettingsCard]: an optional leading mark, the label and its current
 * value, and an optional trailing control.
 *
 * The metrics are the settings hub's, so a provider list reads as the same
 * furniture one level down: 18/17 padding, `titleSmall` over `bodySmall`, and
 * accent-100 while held.
 *
 * @param subtitle the live value, not a description. Null collapses to one line.
 * @param onClick null for a row that is only a label — the row then takes no
 *   press tint and no button semantics.
 */
@Composable
fun SettingsRow(
  title: String,
  subtitle: String?,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  titleColor: Color = MaterialTheme.colorScheme.onSurface,
  leading: @Composable (() -> Unit)? = null,
  trailing: @Composable (() -> Unit)? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .background(if (pressed && enabled) Accent100 else Color.Transparent)
        .then(
          if (onClick != null) {
            Modifier.clickable(
              interactionSource = interactionSource,
              indication = null,
              enabled = enabled,
              role = Role.Button,
              onClick = onClick,
            )
          } else {
            Modifier
          }
        )
        .padding(horizontal = 18.dp, vertical = 17.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    leading?.invoke()
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, style = MaterialTheme.typography.titleSmall, color = titleColor)
      if (subtitle != null) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Neutral600)
      }
    }
    trailing?.invoke()
  }
}
