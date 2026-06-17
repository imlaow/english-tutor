package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userText: String,
    val aiResponse: String,
    val grammarCorrection: String?,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel : ViewModel() {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _speakEvent = MutableStateFlow<String?>(null)
    val speakEvent: StateFlow<String?> = _speakEvent

    private val _history = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            userText = "Hello, I wants to learn English.",
            aiResponse = "Hello! It's great that you want to learn English. I can help you with that.",
            grammarCorrection = "I want to learn English."
        )
    ))
    val history: StateFlow<List<ChatMessage>> = _history

    fun setRecordingState(isRecording: Boolean) {
        _isRecording.value = isRecording
    }

    fun clearSpeakEvent() {
        _speakEvent.value = null
    }

    fun onSpeechResult(userText: String) {
        if (userText.isBlank()) return

        _isProcessing.value = true
        _error.value = null

        viewModelScope.launch {
            // Mock network delay
            delay(1500)
            
            val aiResponse = "That's wonderful! Keep practicing your spoken English."
            val grammarCorrection = if (userText.length % 2 == 0) "Consider reviewing the grammar structure." else null
            
            val message = ChatMessage(
                userText = userText,
                aiResponse = aiResponse,
                grammarCorrection = grammarCorrection
            )
            
            _history.value = _history.value + message
            _speakEvent.value = aiResponse
            _isProcessing.value = false
        }
    }
}

