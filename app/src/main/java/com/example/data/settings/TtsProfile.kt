package com.example.data.settings

import com.example.data.local.TtsProfileEntity
import java.util.UUID

/**
 * The per-utterance knobs — they live in the SSML, not in the SpeechConfig.
 *
 * Every field blank is today's behaviour: no style element, no prosody element,
 * just the voice speaking the text as it always did.
 */
data class VoiceExpression(
    val style: String = "",
    val styleDegree: String = "",
    val pitch: String = "",
    val rate: String = ""
)

/**
 * The part a SpeechSynthesizer is built from; a change here forces a rebuild.
 * Anything later added to [TtsConfig] that affects SpeechConfig must be added
 * here too, or the manager will keep speaking through a stale synthesizer.
 */
data class SynthesizerKey(val speechKey: String, val region: String, val voice: String)

/**
 * The credentials and voice a synthesizer is built from — a profile with the
 * blanks already resolved, so the manager never has to know what a default is —
 * plus the [expression] that is re-read on every utterance.
 */
data class TtsConfig(
    val speechKey: String,
    val region: String,
    val voice: String,
    val expression: VoiceExpression = VoiceExpression()
) {
    val synthesizerKey: SynthesizerKey get() = SynthesizerKey(speechKey, region, voice)
}

/**
 * One named Azure Speech configuration. A blank [voice] means "use
 * [DEFAULT_VOICE]"; [speechKey] and [region] have no fallback and must both be
 * entered before the profile can speak.
 *
 * [style], [styleDegree], [pitch] and [rate] are the SSML expression knobs, all
 * optional: blank means the field is left out of the SSML entirely, so a profile
 * with all four blank sounds exactly like one saved before they existed.
 */
data class TtsProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val speechKey: String = "",
    val region: String = "",
    val voice: String = "",
    val style: String = "",
    val styleDegree: String = "",
    val pitch: String = "",
    val rate: String = "",
    val enabled: Boolean = true,
    val sortOrder: Int = 0
) {
    val effectiveVoice: String get() = voice.ifBlank { DEFAULT_VOICE }

    fun toConfig(): TtsConfig = TtsConfig(
        speechKey = speechKey,
        region = region,
        voice = effectiveVoice,
        expression = VoiceExpression(
            style = style,
            styleDegree = styleDegree,
            pitch = pitch,
            rate = rate
        )
    )

    companion object {
        /** The voice the app spoke with before any of this was configurable. */
        const val DEFAULT_VOICE = "en-US-JennyNeural"

        /** Azure's own style degree when none is given — the middle of 0.01..2. */
        const val DEFAULT_STYLE_DEGREE = "1.0"

        /** The SSML for "no change", which is what a blank pitch or rate means. */
        const val NEUTRAL_RATE = "+0%"
    }
}

fun TtsProfileEntity.toDomain(): TtsProfile = TtsProfile(
    id = id,
    name = name,
    speechKey = speechKey,
    region = region,
    voice = voice,
    style = style,
    styleDegree = styleDegree,
    pitch = pitch,
    rate = rate,
    enabled = enabled,
    sortOrder = sortOrder
)

fun TtsProfile.toEntity(): TtsProfileEntity = TtsProfileEntity(
    id = id,
    name = name,
    speechKey = speechKey,
    region = region,
    voice = voice,
    style = style,
    styleDegree = styleDegree,
    pitch = pitch,
    rate = rate,
    enabled = enabled,
    sortOrder = sortOrder
)
