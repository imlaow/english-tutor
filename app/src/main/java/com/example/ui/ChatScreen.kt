package com.example.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.manager.TtsManager
import com.example.viewmodel.ApiProfileViewModel
import com.example.viewmodel.TopicsViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ProviderSelector(
                        title = "English Tutor",
                        activeProfile = activeProfile,
                        enabledProfiles = enabledProfiles,
                        onSelect = apiProfileViewModel::setActive,
                        onManage = {
                            navController.navigate(Route.API_PROFILES) { launchSingleTop = true }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigate(Route.SETTINGS) { launchSingleTop = true } },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            recognizedText = ""
                            recognitionError = null
                            viewModel.startNewSession()
                        },
                        modifier = Modifier.testTag("new_session_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New session")
                    }
                    IconButton(onClick = { navController.navigate(Route.HISTORY) { launchSingleTop = true } }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
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

@Composable
private fun TopicSuggestions(
    topics: List<String>,
    isLoading: Boolean,
    error: String?,
    onTopicClick: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            topics.isNotEmpty() -> {
                Text(
                    text = "Try one of these topics:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                topics.forEachIndexed { index, topic ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("topic_card_$index")
                            .clickable { onTopicClick(topic) }
                    ) {
                        Text(
                            text = topic,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            isLoading -> {
                CircularProgressIndicator()
                Text(
                    text = "Finding topics for you…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            error != null -> {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                Text(
                    text = "Tap the microphone to start speaking.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Available after the first load so the learner can retry an error or ask
        // for a different set; disabled mid-request to avoid overlapping calls.
        TextButton(
            onClick = onRefresh,
            enabled = !isLoading,
            modifier = Modifier.testTag("refresh_topics_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh")
        }
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
