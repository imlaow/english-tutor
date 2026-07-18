package com.example.manager

import android.util.Log
import com.example.BuildConfig
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Singleton wrapper around the Azure Cognitive Services Speech SDK.
 *
 * All TTS playback control must go through this manager; instantiating the
 * SDK directly inside Compose UI is prohibited (see ARCHITECTURE.md). The
 * synthesizer speaks through the device's default audio output.
 */
object TtsManager {

    private const val TAG = "TtsManager"
    private const val VOICE_NAME = "en-US-JennyNeural"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var synthesizer: SpeechSynthesizer? = null

    private fun getOrCreateSynthesizer(): SpeechSynthesizer =
        synthesizer ?: synchronized(this) {
            synthesizer ?: SpeechSynthesizer(
                SpeechConfig.fromSubscription(
                    BuildConfig.AZURE_SPEECH_KEY,
                    BuildConfig.AZURE_SPEECH_REGION
                ).apply { speechSynthesisVoiceName = VOICE_NAME }
            ).also { synthesizer = it }
        }

    /** Speaks [text] out loud, interrupting any utterance already playing. */
    fun speak(text: String) {
        if (text.isBlank()) return
        scope.launch {
            try {
                val synth = getOrCreateSynthesizer()
                synth.StopSpeakingAsync().get()
                val result = synth.SpeakTextAsync(text).get()
                try {
                    if (result.reason == ResultReason.Canceled) {
                        val details = SpeechSynthesisCancellationDetails.fromResult(result)
                        Log.e(TAG, "Azure TTS canceled: $details")
                    }
                } finally {
                    result.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Azure TTS synthesis failed", e)
            }
        }
    }

    /** Stops the current utterance without releasing the synthesizer. */
    fun stop() {
        val synth = synthesizer ?: return
        scope.launch {
            try {
                synth.StopSpeakingAsync().get()
            } catch (e: Exception) {
                Log.e(TAG, "Azure TTS stop failed", e)
            }
        }
    }

    /** Releases the underlying synthesizer; it is recreated on the next [speak]. */
    fun shutdown() {
        synchronized(this) {
            synthesizer?.close()
            synthesizer = null
        }
    }
}
