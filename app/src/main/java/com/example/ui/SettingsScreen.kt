package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.BuildConfig
import com.example.R
import com.example.data.repository.ApiProfileRepository
import com.example.data.repository.TtsProfileRepository
import com.example.ui.theme.Neutral500
import com.example.ui.theme.Neutral600

/**
 * Indent of the hairline between two rows, so it starts under the text rather
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
    val ttsProfileRepository = remember { TtsProfileRepository.getInstance(context) }
    // The effective one, not the active one: with no profile saved the app still
    // speaks with the keys baked in at build time, and the row has to say so.
    val ttsProfile by ttsProfileRepository.effectiveProfile.collectAsState()

    SettingsContent(
        apiSubtitle = activeProfile?.name ?: "Not configured",
        // No override means topics go through whichever provider the chat uses; a
        // stale override that no longer resolves falls back to the same wording.
        topicSubtitle = if (topicProfileId == null) {
            "Chat provider (default)"
        } else {
            topicProfile?.name ?: "Chat provider (default)"
        },
        ttsSubtitle = ttsProfile?.name ?: "Not configured",
        versionName = BuildConfig.VERSION_NAME,
        onBack = { navController.safePopBackStack() },
        onApiConfiguration = {
            navController.navigate(Route.API_PROFILES) { launchSingleTop = true }
        },
        onTopicGeneration = {
            navController.navigate(Route.TOPIC_PROVIDER) { launchSingleTop = true }
        },
        onTextToSpeech = {
            navController.navigate(Route.TTS_PROFILES) { launchSingleTop = true }
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
    ttsSubtitle: String,
    versionName: String,
    onBack: () -> Unit,
    onApiConfiguration: () -> Unit,
    onTopicGeneration: () -> Unit,
    onTextToSpeech: () -> Unit
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

        SettingsCardColumn(modifier = Modifier.weight(1f)) {
            SectionLabel("Model")
            Spacer(modifier = Modifier.height(10.dp))

            SettingsCard {
                SettingsEntry(
                    title = "API configuration",
                    subtitle = apiSubtitle,
                    icon = painterResource(R.drawable.ic_cloud),
                    iconBackground = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onApiConfiguration,
                    modifier = Modifier.testTag("settings_api_configuration")
                )
                SettingsCardDivider(RowDividerIndent)
                SettingsEntry(
                    title = "Topic generation",
                    subtitle = topicSubtitle,
                    icon = painterResource(R.drawable.ic_lightbulb),
                    iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onTopicGeneration,
                    modifier = Modifier.testTag("settings_topic_generation")
                )
                SettingsCardDivider(RowDividerIndent)
                SettingsEntry(
                    title = "Text to speech",
                    subtitle = ttsSubtitle,
                    icon = painterResource(R.drawable.ic_volume),
                    // Peach again rather than `tertiaryContainer`: that token is
                    // Accent100, the same tint a pressed row paints itself, so the
                    // circle would vanish while the row is held. The two rows that
                    // hold a key sharing a colour is not an accident either.
                    iconBackground = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onTextToSpeech,
                    modifier = Modifier.testTag("settings_text_to_speech")
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
    SettingsRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
        leading = {
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
        },
        trailing = {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = Neutral500,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}
