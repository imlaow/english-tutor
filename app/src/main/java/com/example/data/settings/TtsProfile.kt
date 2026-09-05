package com.example.data.settings

import com.example.data.local.TtsProfileEntity
import java.util.UUID

/**
 * The credentials and voice a synthesizer is built from — a profile with the
 * blanks already resolved, so the manager never has to know what a default is.
 */
data class TtsConfig(
    val speechKey: String,
    val region: String,
    val voice: String
)

/**
 * One named Azure Speech configuration. A blank [voice] means "use
 * [DEFAULT_VOICE]"; [speechKey] and [region] have no fallback and must both be
 * entered before the profile can speak.
 */
data class TtsProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val speechKey: String = "",
    val region: String = "",
    val voice: String = "",
    val enabled: Boolean = true,
    val sortOrder: Int = 0
) {
    val effectiveVoice: String get() = voice.ifBlank { DEFAULT_VOICE }

    fun toConfig(): TtsConfig = TtsConfig(
        speechKey = speechKey,
        region = region,
        voice = effectiveVoice
    )

    companion object {
        /** The voice the app spoke with before any of this was configurable. */
        const val DEFAULT_VOICE = "en-US-JennyNeural"
    }
}

fun TtsProfileEntity.toDomain(): TtsProfile = TtsProfile(
    id = id,
    name = name,
    speechKey = speechKey,
    region = region,
    voice = voice,
    enabled = enabled,
    sortOrder = sortOrder
)

fun TtsProfile.toEntity(): TtsProfileEntity = TtsProfileEntity(
    id = id,
    name = name,
    speechKey = speechKey,
    region = region,
    voice = voice,
    enabled = enabled,
    sortOrder = sortOrder
)
