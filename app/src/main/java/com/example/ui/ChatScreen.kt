package com.example.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.R
import com.example.manager.TtsManager
import com.example.ui.theme.Accent100
import com.example.ui.theme.Accent2100
import com.example.ui.theme.Accent2500
import com.example.ui.theme.Accent2600
import com.example.ui.theme.Accent2700
import com.example.ui.theme.Accent2900
import com.example.ui.theme.Accent400
import com.example.ui.theme.Accent700
import com.example.ui.theme.Accent800
import com.example.ui.theme.Neutral200
import com.example.ui.theme.Neutral500
import com.example.ui.theme.Neutral600
import com.example.ui.theme.SectionKicker
import com.example.viewmodel.ApiProfileViewModel
import com.example.viewmodel.TopicsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    apiProfileViewModel: ApiProfileViewModel,
    topicsViewModel: TopicsViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val activeProfile by apiProfileViewModel.activeProfile.collectAsStateWithLifecycle()
    val enabledProfiles by apiProfileViewModel.enabledProfiles.collectAsStateWithLifecycle()
    val topics by topicsViewModel.topics.collectAsStateWithLifecycle()
    val topicsLoading by topicsViewModel.isLoading.collectAsStateWithLifecycle()
    val topicsError by topicsViewModel.error.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val speakEvent by viewModel.speakEvent.collectAsStateWithLifecycle()
    val history by viewModel.sessionHistory.collectAsStateWithLifecycle(emptyList())

    val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    var recognizedText by remember { mutableStateOf("") }
    var recognitionError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
            TtsManager.stop()
        }
    }

    LaunchedEffect(speakEvent) {
        speakEvent?.let {
            TtsManager.speak(it)
            viewModel.clearSpeakEvent()
        }
    }

    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
        }
    }

    val recognitionListener = remember(viewModel) {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { viewModel.setRecordingState(false) }
            override fun onError(error: Int) {
                viewModel.setRecordingState(false)
                recognitionError = "Recognition error code: $error"
            }
            override fun onResults(results: Bundle?) {
                viewModel.setRecordingState(false)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    recognizedText = text
                    viewModel.onSpeechResult(text)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    LaunchedEffect(recognitionListener) {
        speechRecognizer.setRecognitionListener(recognitionListener)
    }

    // Once the processed message lands in history, hand rendering over from the
    // interim recognized text to the message (user bubble + AI response).
    LaunchedEffect(history) {
        if (history.isNotEmpty()) recognizedText = ""
    }

    // The handoff draws the topic list and the conversation as two screens, but they
    // are one destination here, so the top bar, the body and the dock all switch on
    // the same flag. Interim recognized text counts: the bar must not fall back to
    // the home layout between the learner speaking and the turn being stored.
    val inConversation = history.isNotEmpty() || recognizedText.isNotEmpty()

    // The back arrow and the ＋ have the same destination: this screen has no back
    // stack under it (chat is the start destination), and an empty session is what
    // the design draws as "Home".
    val openEmptySession: () -> Unit = {
        recognizedText = ""
        recognitionError = null
        viewModel.startNewSession()
    }

    val openHistory: () -> Unit = {
        navController.navigate(Route.HISTORY) { launchSingleTop = true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (inConversation) {
            ChatTopBar(
                exchangeCount = history.size,
                onBack = openEmptySession,
                onNewSession = openEmptySession,
                onHistory = openHistory
            )
        } else {
            WarmTopBar(
                title = "English Tutor",
                navigation = {
                    IconButton44(
                        icon = painterResource(R.drawable.ic_settings),
                        contentDescription = "Settings",
                        onClick = { navController.navigate(Route.SETTINGS) { launchSingleTop = true } },
                        modifier = Modifier.testTag("settings_button")
                    )
                },
                subtitle = {
                    ProviderPill(
                        activeProfile = activeProfile,
                        enabledProfiles = enabledProfiles,
                        onSelect = apiProfileViewModel::setActive,
                        onManage = {
                            navController.navigate(Route.API_PROFILES) { launchSingleTop = true }
                        }
                    )
                },
                actions = { ChatBarActions(onNewSession = openEmptySession, onHistory = openHistory) }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (!inConversation) {
                if (isProcessing) {
                    // The tutor is preparing its opening line for a tapped topic.
                    ThinkingIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    TopicSuggestions(
                        topics = topics,
                        isLoading = topicsLoading,
                        error = topicsError,
                        onTopicClick = viewModel::startTopic,
                        onRefresh = topicsViewModel::refresh
                    )
                }
            } else {
                MessageStream(
                    messages = history,
                    interimUserText = recognizedText,
                    isProcessing = isProcessing
                )
            }
        }

        if (error != null) {
            ChatErrorText(text = error ?: "")
        }
        if (recognitionError != null) {
            ChatErrorText(text = recognitionError ?: "")
        }

        MicDock(
            size = if (inConversation) 76.dp else 88.dp,
            iconSize = if (inConversation) 30.dp else 34.dp,
            elevation = if (inConversation) ShadowMd else ShadowLg,
            isRecording = isRecording,
            label = if (isRecording) "Listening…" else "Tap to speak",
            onClick = {
                if (recordAudioPermission.status.isGranted) {
                    recognitionError = null
                    if (isRecording) {
                        speechRecognizer.stopListening()
                        viewModel.setRecordingState(false)
                    } else {
                        recognizedText = ""
                        viewModel.setRecordingState(true)
                        speechRecognizer.startListening(intent)
                    }
                } else {
                    recordAudioPermission.launchPermissionRequest()
                }
            },
            buttonModifier = Modifier.testTag("microphone_button")
        )
    }
}

/**
 * The bar shown while a conversation is on screen: back, the session's name, the
 * turn count, and the same two actions the topic list carries.
 *
 * Visible to the module so the screenshot specimens can render it; it holds no
 * state of its own.
 *
 * @param exchangeCount stored turns in the current session, from
 *   `ChatViewModel.sessionHistory`.
 */
@Composable
internal fun ChatTopBar(
    exchangeCount: Int,
    onBack: () -> Unit,
    onNewSession: () -> Unit,
    onHistory: () -> Unit
) {
    WarmTopBar(
        // The design puts the session's topic here, but nothing records it:
        // ChatViewModel.startTopic() passes the topic straight into the prompt and
        // neither ChatMessage nor ChatMessageEntity keeps it. The app's own name
        // stands in until there is a real value to show.
        title = "English Tutor",
        titleStyle = MaterialTheme.typography.titleMedium,
        navigation = {
            IconButton44(
                icon = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "Back to topics",
                onClick = onBack
            )
        },
        subtitle = {
            Pill(
                // A topic opener counts as a turn, the same way the history screen
                // counts it.
                text = if (exchangeCount == 1) "1 exchange" else "$exchangeCount exchanges",
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                leadingDot = true,
                dotColor = Accent2500
            )
        },
        actions = { ChatBarActions(onNewSession = onNewSession, onHistory = onHistory) }
    )
}

/** The trailing pair both bars carry, so it is written once rather than per branch. */
@Composable
private fun RowScope.ChatBarActions(onNewSession: () -> Unit, onHistory: () -> Unit) {
    IconButton44(
        icon = painterResource(R.drawable.ic_plus),
        contentDescription = "New session",
        onClick = onNewSession,
        modifier = Modifier.testTag("new_session_button"),
        iconSize = 22.dp
    )
    IconButton44(
        icon = painterResource(R.drawable.ic_history),
        contentDescription = "History",
        onClick = onHistory
    )
}

/**
 * The conversation itself.
 *
 * Visible to the module (not private) for the same reason [TopicSuggestions] is:
 * the screenshot specimens render it directly, and it takes plain values, so no
 * ViewModel is involved.
 *
 * @param interimUserText what the recognizer has heard but the tutor has not
 *   answered yet; empty when there is nothing in flight.
 */
@Composable
internal fun MessageStream(
    messages: List<ChatMessage>,
    interimUserText: String,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val trailingItemCount =
        (if (interimUserText.isNotEmpty()) 1 else 0) + (if (isProcessing) 1 else 0)
    val totalItemCount = messages.size + trailingItemCount

    // Keep the newest turn (or the in-flight interim bubble/indicator) in view.
    LaunchedEffect(totalItemCount) {
        if (totalItemCount > 0) listState.animateScrollToItem(totalItemCount - 1)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        // `padding: var(--space-4) var(--space-4) var(--space-2)` on the scroller,
        // so the inset scrolls with the messages instead of clipping them.
        contentPadding = PaddingValues(
            start = MessageStreamInset,
            end = MessageStreamInset,
            top = MessageStreamInset,
            bottom = 9.dp
        ),
        verticalArrangement = Arrangement.spacedBy(TurnGap)
    ) {
        items(messages, key = { it.id }) { message ->
            Column(verticalArrangement = Arrangement.spacedBy(WithinTurnGap)) {
                // A topic-opener turn has no learner utterance, so it shows only
                // the tutor's bubble.
                if (message.userText.isNotBlank()) {
                    ChatBubble(text = message.userText, isUser = true)
                }
                ChatBubble(
                    text = message.aiResponse,
                    grammarCorrection = message.grammarCorrection,
                    isUser = false
                )
            }
        }
        if (interimUserText.isNotEmpty()) {
            item(key = "interim_user_text") {
                ChatBubble(text = interimUserText, isUser = true)
            }
        }
        if (isProcessing) {
            item(key = "processing_indicator") { ThinkingIndicator() }
        }
    }
}

@Composable
private fun ChatErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MessageStreamInset)
            .padding(bottom = 8.dp)
    )
}

/** `--radius-lg` — the card radius the handoff's rounded-frame override lands on. */
private val TopicCardShape = RoundedCornerShape(28.dp)

/**
 * The three shadow tokens as Compose elevations: `--shadow-sm` (`0 1 2` @14%),
 * `--shadow-md` (`0 3 10` @16%) and `--shadow-lg` (`0 12 32` @22%). Cards and
 * bubbles all take the small one — without it they read as untinted text on the
 * background, since neutral-100 and the page background are a shade apart.
 */
private val ShadowSm = 2.dp
private val ShadowMd = 6.dp
private val ShadowLg = 16.dp

/** `padding: var(--space-4) …` on the message scroller, and its two gap sizes. */
private val MessageStreamInset = 18.dp
private val TurnGap = 18.dp
private val WithinTurnGap = 8.dp

/** Placeholder cards shown while the request is in flight. */
private const val SkeletonCardCount = 3

/**
 * Visible to the module (not private) so the screenshot specimens can render all
 * four of its states directly, the same way [ChatBubble] is rendered there. It
 * takes plain values and lambdas, so no ViewModel is needed to drive it.
 */
@Composable
internal fun TopicSuggestions(
    topics: List<String>,
    isLoading: Boolean,
    error: String?,
    onTopicClick: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 9.dp)
    ) {
        when {
            topics.isNotEmpty() -> {
                TopicsHeader()
                topics.forEachIndexed { index, topic ->
                    if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                    TopicCard(index = index, topic = topic, onClick = { onTopicClick(topic) })
                }
            }
            // The handoff only draws the loaded state. Skeletons in the shape of the
            // cards keep the layout from jumping once the topics land.
            isLoading -> {
                TopicsHeader()
                repeat(SkeletonCardCount) { index ->
                    if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(Neutral200, TopicCardShape)
                    )
                }
                Spacer(modifier = Modifier.height(13.dp))
                Text(
                    text = "Finding topics for you…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Neutral600,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // The message is whatever the ViewModel reported (a missing key, a failed
            // call); it is shown verbatim rather than restated here. The header stays
            // for the same reason it does while loading: a failed refresh should not
            // shift the rest of the screen up.
            error != null -> {
                TopicsHeader()
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Accent800,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Accent100, TopicCardShape)
                        .padding(18.dp)
                )
            }
            else -> {
                Text(
                    text = "Tap the microphone to start speaking.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        // Available after the first load so the learner can retry an error or ask
        // for a different set; disabled mid-request to avoid overlapping calls.
        RefreshTopicsButton(
            isLoading = isLoading,
            onClick = onRefresh,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun TopicsHeader() {
    Text(
        text = "Suggested for you".uppercase(Locale.US),
        style = SectionKicker,
        color = Accent2700
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Try one of these",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(18.dp))
}

/**
 * One suggestion row. The number badge is the card's position in the list, not part
 * of the topic itself, so it counts from the `forEachIndexed` index.
 */
@Composable
private fun TopicCard(index: Int, topic: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(ShadowSm, TopicCardShape)
            .background(
                color = if (pressed) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = TopicCardShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 16.dp)
            .testTag("topic_card_$index"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        AvatarBadge(
            letter = "${index + 1}",
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            size = 28.dp,
            fontSize = 12.sp
        )
        Text(
            text = topic,
            style = MaterialTheme.typography.titleSmall,
            // `line-height: 1.35` on the 16sp card title, tighter than the 1.55 body default.
            lineHeight = 21.6.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = Neutral500,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun RefreshTopicsButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(enabled = !isLoading, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("refresh_topics_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Accent700
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_refresh),
                contentDescription = null,
                tint = Accent700,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = "Refresh topics",
            style = MaterialTheme.typography.labelLarge,
            color = Accent700
        )
    }
}

/** `border-radius: 22px 22px 6px 22px` — the corner nearest the speaker is clipped. */
private val UserBubbleShape =
    RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomEnd = 6.dp, bottomStart = 22.dp)

/** `border-radius: 6px 22px 22px 22px`. */
private val TutorBubbleShape =
    RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp)

/**
 * One side of a turn.
 *
 * The learner gets a bare right-aligned bubble; the tutor gets a labelled block —
 * avatar, "TUTOR" tag and a read-aloud button above the bubble, with the optional
 * "TRY SAYING" card under it. [grammarCorrection] is the tutor's, so it is ignored
 * when [isUser] is true, and the whole card is dropped when the model returned
 * nothing worth correcting.
 *
 * The signature is fixed: ThemeScreenshotTest renders this directly.
 */
@Composable
fun ChatBubble(text: String, isUser: Boolean, grammarCorrection: String? = null) {
    // `max-width` is a percentage of the stream's width, so it needs the incoming
    // constraint; a fillMaxWidth fraction would stretch short messages instead.
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        if (isUser) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                // `line-height: 1.5` on the 15sp learner text, against 1.55 for the
                // tutor's — the design tightens the shorter of the two.
                lineHeight = 22.5.sp,
                color = Accent100,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .widthIn(max = availableWidth * 0.82f)
                    .shadow(ShadowSm, UserBubbleShape)
                    .background(MaterialTheme.colorScheme.primary, UserBubbleShape)
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .widthIn(max = availableWidth * 0.86f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                TutorLabel(text = text)
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 23.25.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .shadow(ShadowSm, TutorBubbleShape)
                        .background(MaterialTheme.colorScheme.surface, TutorBubbleShape)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
                if (!grammarCorrection.isNullOrEmpty()) {
                    TrySayingCard(correction = grammarCorrection)
                }
            }
        }
    }
}

/** The tutor's byline: who is speaking, and a button to hear it again. */
@Composable
private fun TutorLabel(text: String) {
    Row(
        modifier = Modifier.padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        AvatarBadge(
            letter = "T",
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            size = 20.dp
        )
        Text(
            text = "Tutor".uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall,
            color = Neutral600
        )
        // The reply is spoken once when it arrives (ChatViewModel.speakEvent); this
        // replays it. TtsManager is the single sanctioned entry point for playback,
        // so it can be called without threading a callback through the fixed
        // signature of [ChatBubble].
        IconButton44(
            icon = painterResource(R.drawable.ic_volume),
            contentDescription = "Play again",
            onClick = { TtsManager.speak(text) },
            iconSize = 14.dp,
            tint = Neutral500,
            size = 24.dp
        )
    }
}

/**
 * The grammar note under a tutor reply. The handoff reframes it from a red error
 * ("Grammar Correction") into a green suggestion, so it uses the olive ramp rather
 * than the theme's error colors.
 */
@Composable
private fun TrySayingCard(correction: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Accent2100, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lightbulb),
            contentDescription = null,
            tint = Accent2600,
            // `margin-top: 2px` — optically centres the bulb on the first line.
            modifier = Modifier.padding(top = 2.dp).size(16.dp)
        )
        Column {
            Text(
                text = "Try saying".uppercase(Locale.US),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Accent2700
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = correction,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
                color = Accent2900
            )
        }
    }
}

/** `height: 26px` on the bar track; the bars swing between 22% and 100% of it. */
private val ThinkingTrackHeight = 26.dp

/**
 * The tutor composing a reply: `@keyframes bar` — three 5dp bars easing between 22%
 * and 100% of the track over 0.9s, each starting 150ms after the one before.
 * Replaces the spinner the screen used in both of its waiting states.
 */
@Composable
private fun ThinkingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thinking")
    Row(
        modifier = modifier
            .padding(start = 8.dp)
            .height(ThinkingTrackHeight)
            .semantics { contentDescription = "Tutor is replying" },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(3) { index ->
            // Half the CSS cycle each way: 450ms out, 450ms back.
            val heightFraction by transition.animateFloat(
                initialValue = 0.22f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(450, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 150)
                ),
                label = "thinking_bar_$index"
            )
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(ThinkingTrackHeight * heightFraction)
                    .background(Accent400, CircleShape)
            )
        }
    }
}
