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
import com.example.data.remote.ConnectionTestResult
import com.example.data.remote.ProbeOutcome
import com.example.data.settings.ApiProfile
import com.example.data.settings.ApiSpec
import com.example.data.settings.TtsProfile
import com.example.ui.ApiProfileEditContent
import com.example.ui.ApiProfileListContent
import com.example.ui.ChatMessage
import com.example.ui.ChatTopBar
import com.example.ui.HistoryContent
import com.example.ui.IconButton44
import com.example.ui.MessageStream
import com.example.ui.MicDock
import com.example.ui.OnboardingContent
import com.example.ui.ProviderPill
import com.example.ui.SettingsContent
import com.example.ui.TopicProviderContent
import com.example.ui.TopicSuggestions
import com.example.ui.TtsProfileEditContent
import com.example.ui.TtsProfileListContent
import com.example.ui.WarmTopBar
import com.example.viewmodel.ConnectionTestState
import com.example.viewmodel.OnboardingAnswers
import com.example.viewmodel.OnboardingUiState
import java.util.GregorianCalendar

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
 * A second profile for the list and picker specimens, disabled so a capture shows
 * both row states at once. Its model is left blank on purpose: the row then prints
 * `effectiveModel`, which is the spec's own default constant rather than a name
 * typed out here.
 */
private val SpecimenSecondProfile =
    ApiProfile(
        id = "specimen-2",
        name = "Work account",
        apiSpec = ApiSpec.OPENAI,
        enabled = false,
    )

/**
 * A saved Azure Speech profile. Its voice is left blank on purpose: the row then
 * prints `effectiveVoice`, which is [TtsProfile.DEFAULT_VOICE] rather than a
 * voice name typed out here.
 */
private val SpecimenVoice =
    TtsProfile(id = "specimen-voice", name = "Azure personal", region = "eastus")

/** A second voice, disabled so a capture shows both row states at once. */
private val SpecimenSecondVoice =
    TtsProfile(
        id = "specimen-voice-2",
        name = "Work subscription",
        region = "westeurope",
        voice = "en-GB-RyanNeural",
        enabled = false,
    )

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
 * The saved-conversation list, or its empty state when [conversations] is empty.
 *
 * [HistoryContent] draws its own top bar, so nothing is wrapped around it here.
 */
@Composable
internal fun HistorySpecimen(
    conversations: List<List<ChatMessage>> = SpecimenConversations,
) {
    HistoryContent(
        conversations = conversations,
        onBack = {},
        onClearAll = {},
        onDeleteSession = {},
        onOpenSession = {},
    )
}

/**
 * The settings hub, with a provider chosen and topic generation left on its
 * default. [SettingsContent] draws its own top bar, so nothing wraps it here.
 *
 * Both subtitles are values the screen really shows: the active profile's name, and
 * the wording used when no topic override is set. The version is the build's own,
 * not the handoff's mock "v1.0".
 */
@Composable
internal fun SettingsSpecimen(
    apiSubtitle: String = SpecimenProfile.name,
    topicSubtitle: String = "Chat provider (default)",
    ttsSubtitle: String = SpecimenVoice.name,
) {
    SettingsContent(
        apiSubtitle = apiSubtitle,
        topicSubtitle = topicSubtitle,
        ttsSubtitle = ttsSubtitle,
        versionName = BuildConfig.VERSION_NAME,
        onBack = {},
        onApiConfiguration = {},
        onTopicGeneration = {},
        onTextToSpeech = {},
    )
}

// ------------------------------------------------------------------------
// The four screens the handoff never drew. Only their shell follows the design
// — the warm top bar and its 44dp buttons — so these captures exist to pin that
// shell and the Material form controls under it, not to be laid over an export.
// They use the class-level Pixel 8 canvas rather than HandoffCanvasQualifier,
// since there is no 393dp drawing to align with.

/**
 * The onboarding questionnaire, part-way through: a level picked and the first
 * answer typed, so both the selected and unselected chip states are visible along
 * with a filled and an empty text field.
 *
 * The answers are fixtures — the real ones are whatever the learner types into
 * [com.example.viewmodel.OnboardingViewModel].
 */
@Composable
internal fun OnboardingSpecimen(
    uiState: OnboardingUiState =
        OnboardingUiState(
            answers =
                OnboardingAnswers(
                    selfAssessedLevel = "Intermediate",
                    learningGoal = "Handle stand-ups without rehearsing first",
                ),
        ),
) {
    OnboardingContent(
        uiState = uiState,
        onOpenSettings = {},
        onLevelChanged = {},
        onGoalChanged = {},
        onInterestsChanged = {},
        onPracticeMinutesChanged = {},
        onSubmit = {},
    )
}

/**
 * The saved API profiles, with the first one active and the second disabled.
 * [ApiProfileListContent] draws its own top bar, so nothing wraps it here.
 */
@Composable
internal fun ApiProfileListSpecimen(
    profiles: List<ApiProfile> = listOf(SpecimenProfile, SpecimenSecondProfile),
) {
    ApiProfileListContent(
        profiles = profiles,
        activeProfileId = profiles.firstOrNull()?.id,
        onBack = {},
        onAdd = {},
        onSelect = {},
        onEdit = {},
        onToggleEnabled = {},
        onDelete = {},
    )
}

/**
 * The edit form for an existing profile, showing a finished connection test with
 * one probe passed and one failed — which is also what puts `ic_check` and
 * `ic_close` in a capture.
 *
 * The key is a placeholder of the right shape, not a credential; the probe
 * wording is a fixture, since the real text is whatever the provider answers.
 */
@Composable
internal fun ApiProfileEditSpecimen(
    profile: ApiProfile? = SpecimenProfile.copy(apiKey = "sk-not-a-real-key"),
    connectionTest: ConnectionTestState = SpecimenConnectionTest,
) {
    ApiProfileEditContent(
        profile = profile,
        isNewProfile = false,
        formError = null,
        connectionTest = connectionTest,
        onBack = {},
        onNameChange = {},
        onSpecChange = {},
        onBaseUrlChange = {},
        onApiKeyChange = {},
        onModelChange = {},
        onEnabledChange = {},
        onTestConnection = {},
        onSave = {},
    )
}

private val SpecimenConnectionTest =
    ConnectionTestState.Done(
        ConnectionTestResult(
            nonStreaming = ProbeOutcome.Success("replied in 412 ms"),
            streaming =
                ProbeOutcome.Failure(
                    summary = "HTTP 400",
                    detail = "This endpoint does not accept stream: true.",
                ),
        )
    )

/**
 * The topic-generation provider picker, left on its default. The subtitle under
 * that row names what the fallback currently resolves to, exactly as the screen
 * gets it from the repository.
 *
 * Both fixtures are enabled: the screen is fed `enabledProfiles`, so a disabled
 * one can never reach it.
 */
@Composable
internal fun TopicProviderSpecimen(
    profiles: List<ApiProfile> =
        listOf(SpecimenProfile, SpecimenSecondProfile.copy(enabled = true)),
    selectedProfileId: String? = null,
) {
    TopicProviderContent(
        profiles = profiles,
        selectedProfileId = selectedProfileId,
        defaultSubtitle = "Currently: ${SpecimenProfile.name}",
        onBack = {},
        onSelect = {},
    )
}

/**
 * The saved voices, with the first one active and the second disabled — the same
 * two row states [ApiProfileListSpecimen] pins, for the other set of credentials.
 */
@Composable
internal fun TtsProfileListSpecimen(
    profiles: List<TtsProfile> = listOf(SpecimenVoice, SpecimenSecondVoice),
) {
    TtsProfileListContent(
        profiles = profiles,
        activeProfileId = profiles.firstOrNull()?.id,
        onBack = {},
        onAdd = {},
        onSelect = {},
        onEdit = {},
        onToggleEnabled = {},
        onDelete = {},
    )
}

/**
 * The edit form for an existing voice, with the voice field left empty so the
 * capture also pins the placeholder — which is the default voice constant, not a
 * name written out here.
 *
 * The key is a placeholder of the right shape, not a credential.
 */
@Composable
internal fun TtsProfileEditSpecimen(
    profile: TtsProfile? = SpecimenVoice.copy(speechKey = "not-a-real-speech-key"),
) {
    TtsProfileEditContent(
        profile = profile,
        isNewProfile = false,
        formError = null,
        onBack = {},
        onNameChange = {},
        onSpeechKeyChange = {},
        onRegionChange = {},
        onVoiceChange = {},
        onEnabledChange = {},
        onSave = {},
    )
}

/**
 * Fixture sessions in the shape the history screen groups out of Room: newest
 * first, oldest-first inside each session. One topic opener with no learner line
 * (so the `Y` row is absent), one two-turn session, and one whose lines are long
 * enough to hit the 1-line and 2-line clamps.
 *
 * Timestamps are fixed so the formatted date is stable across runs; they are built
 * in the default zone, which is also the zone the screen formats in. Both the dates
 * and the sentences are deliberately unlike the handoff's own mock sessions — real
 * ones come from [com.example.ui.ChatViewModel.history].
 */
internal val SpecimenConversations =
    listOf(
        listOf(
            specimenTurn(
                sessionId = "specimen-c",
                userText = "",
                aiResponse =
                    "Let's talk about the last trip you took. Where did you go, and who was with you?",
                timestamp = specimenTimestamp(2026, 4, 2, 20, 15),
            ),
        ),
        listOf(
            specimenTurn(
                sessionId = "specimen-b",
                userText = "I want to practise ordering food.",
                aiResponse = "Good idea. Imagine we are at a bakery — what would you like?",
                timestamp = specimenTimestamp(2026, 3, 28, 8, 5),
            ),
            specimenTurn(
                sessionId = "specimen-b",
                userText = "Two croissants, please.",
                aiResponse = "Nicely put. Now try asking whether they are still warm.",
                timestamp = specimenTimestamp(2026, 3, 28, 8, 7),
            ),
        ),
        listOf(
            specimenTurn(
                sessionId = "specimen-a",
                userText =
                    "My colleague asked me to explain the new process and I could not find the words for it.",
                aiResponse =
                    "That happens to everyone. Let's build the sentence together, one step at a time, " +
                        "and then you can say the whole thing back to me.",
                timestamp = specimenTimestamp(2026, 3, 14, 19, 40),
            ),
        ),
    )

private fun specimenTurn(
    sessionId: String,
    userText: String,
    aiResponse: String,
    timestamp: Long,
) =
    ChatMessage(
        id = "$sessionId-$timestamp",
        sessionId = sessionId,
        userText = userText,
        aiResponse = aiResponse,
        grammarCorrection = null,
        timestamp = timestamp,
    )

/** [month] is 1-based, unlike [java.util.Calendar]'s. Default zone, as above. */
private fun specimenTimestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
    GregorianCalendar(year, month - 1, day, hour, minute).timeInMillis

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
