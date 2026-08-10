package com.example.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.R
import com.example.manager.TtsManager
import com.example.ui.theme.Accent100
import com.example.ui.theme.Accent2700
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
            actions = {
                IconButton44(
                    icon = painterResource(R.drawable.ic_plus),
                    contentDescription = "New session",
                    onClick = {
                        recognizedText = ""
                        recognitionError = null
                        viewModel.startNewSession()
                    },
                    modifier = Modifier.testTag("new_session_button"),
                    iconSize = 22.dp
                )
                IconButton44(
                    icon = painterResource(R.drawable.ic_history),
                    contentDescription = "History",
                    onClick = { navController.navigate(Route.HISTORY) { launchSingleTop = true } }
                )
            }
        )

        // The uniform 16dp inset the Scaffold body used to carry now belongs to the
        // content: the topic list owns the design's 26/18/9 padding, and the message
        // stream gets its own in the next pass.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (history.isEmpty() && recognizedText.isEmpty()) {
                    if (isProcessing) {
                        // The tutor is preparing its opening line for a tapped topic.
                        CircularProgressIndicator()
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
                    val listState = rememberLazyListState()
                    val trailingItemCount =
                        (if (recognizedText.isNotEmpty()) 1 else 0) + (if (isProcessing) 1 else 0)
                    val totalItemCount = history.size + trailingItemCount

                    // Keep the newest turn (or the in-flight interim bubble/spinner) in view.
                    LaunchedEffect(totalItemCount) {
                        if (totalItemCount > 0) listState.animateScrollToItem(totalItemCount - 1)
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(history, key = { it.id }) { message ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // A topic-opener turn has no learner utterance, so it
                                // shows only the tutor's bubble.
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
                        if (recognizedText.isNotEmpty()) {
                            item(key = "interim_user_text") {
                                ChatBubble(text = recognizedText, isUser = true)
                            }
                        }
                        if (isProcessing) {
                            item(key = "processing_indicator") {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }

            if (error != null) {
                Text(text = error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }
            if (recognitionError != null) {
                Text(text = recognitionError ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            FloatingActionButton(
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
                modifier = Modifier
                    .size(80.dp)
                    .testTag("microphone_button"),
                shape = CircleShape,
                containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Microphone", modifier = Modifier.size(36.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isRecording) "Listening..." else "Tap to speak",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** `--radius-lg` — the card radius the handoff's rounded-frame override lands on. */
private val TopicCardShape = RoundedCornerShape(28.dp)

/** `--shadow-sm` (`0 1 2` @14%) approximated as a Compose elevation. */
private val TopicCardElevation = 2.dp

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
            .shadow(TopicCardElevation, TopicCardShape)
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

@Composable
fun ChatBubble(text: String, isUser: Boolean, grammarCorrection: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.padding(vertical = 4.dp).widthIn(max = 300.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(16.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (!isUser && !grammarCorrection.isNullOrEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp).widthIn(max = 300.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Grammar Correction",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = grammarCorrection,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
