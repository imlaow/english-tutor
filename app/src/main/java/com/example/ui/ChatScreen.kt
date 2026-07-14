package com.example.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, navController: NavController) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val speakEvent by viewModel.speakEvent.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle(emptyList())

    val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    var recognizedText by remember { mutableStateOf("") }
    var recognitionError by remember { mutableStateOf<String?>(null) }

    val tts = remember {
        var textToSpeech: TextToSpeech? = null
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
            }
        }
        textToSpeech
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
            tts.stop()
            tts.shutdown()
        }
    }

    LaunchedEffect(speakEvent) {
        speakEvent?.let {
            tts.speak(it, TextToSpeech.QUEUE_FLUSH, null, null)
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
                title = { Text("English Tutor", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate(Route.HISTORY) }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
            val lastMsg = history.lastOrNull()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (history.isEmpty() && recognizedText.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tap the microphone to start speaking.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (recognizedText.isNotEmpty()) {
                            ChatBubble(text = recognizedText, isUser = true)
                        } else if (lastMsg != null) {
                            ChatBubble(text = lastMsg.userText, isUser = true)
                        }

                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            if (lastMsg != null && recognizedText.isEmpty()) {
                                ChatBubble(
                                    text = lastMsg.aiResponse,
                                    grammarCorrection = lastMsg.grammarCorrection,
                                    isUser = false
                                )
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
