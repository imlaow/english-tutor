package com.example.manager

import android.util.Log
import com.example.data.settings.TtsConfig
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
 *
 * The credentials are not the manager's to choose: they arrive through
 * [configure], which the composition root keeps pointed at the active TTS
 * profile. Until one does, [speak] has nothing to speak with and says so in the
 * log rather than constructing a synthesizer that would fail on every utterance.
 */
object TtsManager {

    private const val TAG = "TtsManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var config: TtsConfig? = null

    @Volatile
    private var synthesizer: SpeechSynthesizer? = null

    // What the cached synthesizer was built from, so a changed key, region or
    // voice is noticed even though the synthesizer itself cannot be re-pointed.
    @Volatile
    private var synthesizerConfig: TtsConfig? = null

    /**
     * Points playback at [config], or at nothing when it is null. Cheap to call
     * repeatedly: an unchanged configuration keeps the synthesizer that is
     * already open, and a changed one closes it so the next [speak] rebuilds.
     */
    fun configure(config: TtsConfig?) {
        synchronized(this) {
            if (this.config == config) return
            this.config = config
            closeSynthesizer()
        }
    }

    private fun getOrCreateSynthesizer(config: TtsConfig): SpeechSynthesizer =
        synchronized(this) {
            synthesizer?.takeIf { synthesizerConfig == config }?.let { return@synchronized it }
            // Either there is none, or the one held was built from credentials the
            // user has since changed; a synthesizer cannot be re-pointed.
            closeSynthesizer()
            SpeechSynthesizer(
                SpeechConfig.fromSubscription(config.speechKey, config.region)
                    .apply { speechSynthesisVoiceName = config.voice }
            ).also {
                synthesizer = it
                synthesizerConfig = config
            }
        }

    /** Speaks [text] out loud, interrupting any utterance already playing. */
    fun speak(text: String) {
        if (text.isBlank()) return
        val config = config
        if (config == null) {
            Log.w(TAG, "No TTS profile configured; nothing to speak with")
            return
        }
        scope.launch {
            try {
                val synth = getOrCreateSynthesizer(config)
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
        synchronized(this) { closeSynthesizer() }
    }

    /** Caller must hold the monitor. */
    private fun closeSynthesizer() {
        synthesizer?.close()
        synthesizer = null
        synthesizerConfig = null
    }
}
