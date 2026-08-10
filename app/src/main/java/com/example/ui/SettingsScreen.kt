package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.BuildConfig
import com.example.R
import com.example.data.repository.ApiProfileRepository
import com.example.ui.theme.Accent100
import com.example.ui.theme.Accent2700
import com.example.ui.theme.Neutral500
import com.example.ui.theme.Neutral600
import com.example.ui.theme.SectionKicker
import java.util.Locale

/** `--radius-lg` — the same 28dp the topic and history cards use. */
private val SettingsCardShape = RoundedCornerShape(28.dp)

/** `--shadow-sm` (`0 1 2` @14%) as a Compose elevation. */
private val ShadowSm = 2.dp

/**
 * Indent of the hairline between the two rows, so it starts under the text rather
 * than under the icon circle (`margin-left: 70px` in the handoff).
 */
private val RowDividerIndent = 70.dp

/**
 * Main settings hub: lists setting categories, each navigating to its own
 * sub-screen. New categories (appearance, tutor preferences, ...) should be
 * added as further [SettingsEntry] rows inside the same grouped card.
 */
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val apiProfileRepository = remember { ApiProfileRepository.getInstance(context) }
    val activeProfile by apiProfileRepository.activeProfile.collectAsState()
    val topicProfileId by apiProfileRepository.topicProfileId.collectAsState()
    val topicProfile by apiProfileRepository.topicProfile.collectAsState()

    SettingsContent(
        apiSubtitle = activeProfile?.name ?: "Not configured",
        // No override means topics go through whichever provider the chat uses; a
        // stale override that no longer resolves falls back to the same wording.
        topicSubtitle = if (topicProfileId == null) {
            "Chat provider (default)"
        } else {
            topicProfile?.name ?: "Chat provider (default)"
        },
        versionName = BuildConfig.VERSION_NAME,
        onBack = { navController.safePopBackStack() },
        onApiConfiguration = {
            navController.navigate(Route.API_PROFILES) { launchSingleTop = true }
        },
        onTopicGeneration = {
            navController.navigate(Route.TOPIC_PROVIDER) { launchSingleTop = true }
        }
    )
}

/**
 * The screen without its repository: the warm top bar over the grouped card.
 *
 * Visible to the module (not private) for the same reason [HistoryContent] is —
 * the screenshot specimens render it directly, and it takes plain values and
 * lambdas.
 *
 * @param versionName comes from `BuildConfig`, so the footer tracks the build
 *   rather than restating a number that would drift.
 */
@Composable
internal fun SettingsContent(
    apiSubtitle: String,
    topicSubtitle: String,
    versionName: String,
    onBack: () -> Unit,
    onApiConfiguration: () -> Unit,
    onTopicGeneration: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WarmTopBar(
            title = "Settings",
            navigation = {
                IconButton44(
                    icon = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Back",
                    onClick = onBack
                )
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 26.dp)
        ) {
            Text(
                text = "Model".uppercase(Locale.US),
                style = SectionKicker,
                color = Accent2700
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // `shadow` clips to its shape at any non-zero elevation, which is
                    // the card's `overflow: hidden`: without it a pressed row would
                    // paint its tint square over the rounded corners.
                    .shadow(ShadowSm, SettingsCardShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                SettingsEntry(
                    title = "API configuration",
                    subtitle = apiSubtitle,
                    icon = painterResource(R.drawable.ic_cloud),
                    iconBackground = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onApiConfiguration,
                    modifier = Modifier.testTag("settings_api_configuration")
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = RowDividerIndent),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsEntry(
                    title = "Topic generation",
                    subtitle = topicSubtitle,
                    icon = painterResource(R.drawable.ic_lightbulb),
                    iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onTopicGeneration,
                    modifier = Modifier.testTag("settings_topic_generation")
                )
            }

            Spacer(modifier = Modifier.height(35.dp))
            Text(
                // The app's own name plus the build's version; the handoff's "v1.0"
                // was mock text.
                text = "${stringResource(R.string.app_name)} · v$versionName",
                style = MaterialTheme.typography.bodySmall,
                fontSize = FooterFontSize,
                lineHeight = FooterLineHeight,
                color = Neutral600,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** The footer is the one 12sp step in the handoff, below `bodySmall`'s 13. */
private val FooterFontSize = 12.sp
private val FooterLineHeight = 18.6.sp

/**
 * One row of the grouped card: a tinted icon circle, the category and its current
 * value, and the chevron that says it opens elsewhere.
 *
 * @param subtitle the live setting, not a description — the profile in use, or the
 *   wording for "nothing chosen yet".
 */
@Composable
private fun SettingsEntry(
    title: String,
    subtitle: String,
    icon: Painter,
    iconBackground: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (pressed) Accent100 else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Neutral600
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = Neutral500,
            modifier = Modifier.size(18.dp)
        )
    }
}
