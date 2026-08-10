package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.data.settings.ApiProfile
import com.example.data.settings.ApiSpec
import com.example.ui.ChatMessage
import com.example.ui.ChatTopBar
import com.example.ui.IconButton44
import com.example.ui.MessageStream
import com.example.ui.MicDock
import com.example.ui.ProviderPill
import com.example.ui.TopicSuggestions
import com.example.ui.WarmTopBar

/**
 * Test-only specimens of the real screens, as opposed to [ColorSwatches] and its
 * neighbours in ThemeGallery, which render the theme itself.
 *
 * These exist so a screen can be reviewed against the design export as an image.
 * They deliberately depend on production composables and will move whenever a
 * screen moves — that is the point. Compare a capture with the matching
 * `export_N-*.png` from the handoff, scaled to the same width.
 *
 * The strings here are layout fixtures, not app copy: real topics come from
 * [com.example.viewmodel.TopicsViewModel] and real errors from its `error` flow.
 * They are chosen to exercise wrapping and count, not to mirror the handoff's
 * mock text.
 */

/**
 * The handoff's canvas is 393px at 1x, which is the dp width it was drawn for.
 *
 * This has to be pinned by the Robolectric device qualifier
 * (`@Config(qualifiers = "+w393dp-h873dp")`), not by a `Modifier.width` here:
 * the capture is taken from `onRoot()`, which is the whole window, so a narrower
 * composable inside it would still be photographed on a window-width canvas.
 */
internal const val HandoffCanvasQualifier = "+w393dp-h873dp"

private val SpecimenProfile =
    ApiProfile(id = "specimen", name = "Gemini Flash", apiSpec = ApiSpec.GEMINI)

/**
 * The chat screen's empty state: the warm top bar over [TopicSuggestions].
 *
 * Parameters mirror that composable's four mutually exclusive branches — pass
 * topics, or `isLoading`, or an `error`, or none of them for the initial state.
 */
@Composable
internal fun HomeSpecimen(
    topics: List<String> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        WarmTopBar(
            title = "English Tutor",
            navigation = {
                IconButton44(
                    icon = painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings",
                    onClick = {},
                )
            },
            subtitle = {
                ProviderPill(
                    activeProfile = SpecimenProfile,
                    enabledProfiles = listOf(SpecimenProfile),
                    onSelect = {},
                    onManage = {},
                )
            },
            actions = {
                IconButton44(
                    icon = painterResource(R.drawable.ic_plus),
                    contentDescription = "New session",
                    onClick = {},
                    iconSize = 22.dp,
                )
                IconButton44(
                    icon = painterResource(R.drawable.ic_history),
                    contentDescription = "History",
                    onClick = {},
                )
            },
        )
        TopicSuggestions(
            topics = topics,
            isLoading = isLoading,
            error = error,
            onTopicClick = {},
            onRefresh = {},
        )
    }
}

/** Varying lengths so the card's text wrapping is visible, not just its height. */
internal val SpecimenTopics =
    listOf(
        "Your weekend plans",
        "A skill you are learning right now",
        "Street food you miss",
    )

/**
 * The chat screen mid-conversation: the chat top bar over [MessageStream], with the
 * dock underneath at its conversation size.
 *
 * The mic dock is drawn idle on purpose. Both the recording pulse and the thinking
 * indicator are infinite animations, which never let the compose rule go idle, so a
 * capture of either would hang rather than settle.
 */
@Composable
internal fun ChatSpecimen(messages: List<ChatMessage> = SpecimenTurns) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ChatTopBar(
            exchangeCount = messages.size,
            onBack = {},
            onNewSession = {},
            onHistory = {},
        )
        MessageStream(
            messages = messages,
            interimUserText = "",
            isProcessing = false,
            modifier = Modifier.weight(1f),
        )
        MicDock(
            size = 76.dp,
            iconSize = 30.dp,
            elevation = 6.dp,
            isRecording = false,
            label = "Tap to speak",
            onClick = {},
        )
    }
}

/**
 * Three turns, shaped like the ones the tutor actually produces: an opener with no
 * learner utterance and no correction, then a turn with both halves and no
 * correction, then one carrying a correction. Sentences are fixtures chosen to
 * exercise wrapping — real text comes from the model via [com.example.ui.ChatViewModel].
 */
internal val SpecimenTurns =
    listOf(
        ChatMessage(
            id = "turn-1",
            sessionId = "specimen",
            userText = "",
            aiResponse =
                "Let's practise together. What is one thing you did today that you enjoyed?",
            grammarCorrection = null,
        ),
        ChatMessage(
            id = "turn-2",
            sessionId = "specimen",
            userText = "I cooked lunch for my family.",
            aiResponse = "That sounds lovely. What did you make for them?",
            grammarCorrection = null,
        ),
        ChatMessage(
            id = "turn-3",
            sessionId = "specimen",
            userText = "We eat noodles and after we watch a film together.",
            aiResponse =
                "Noodles and a film is a good combination. Which film did you choose?",
            grammarCorrection =
                "Past tense here: \"we ate noodles, and afterwards we watched a film.\"",
        ),
    )
