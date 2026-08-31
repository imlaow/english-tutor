package com.example.data.settings

import com.example.BuildConfig
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

        /** Id of [buildConfigProfile]; fixed so it can never collide with a saved row. */
        const val BUILD_CONFIG_ID = "build-config"

        /**
         * The keys baked in at build time from `.env`, as a profile.
         *
         * This is what the app used before Settings could hold a profile, and it
         * stays the fallback so an existing install — whose user has never seen
         * that key and could not re-enter it — keeps speaking after the upgrade.
         * Null when the build carries no key, which is the case for CI and for
         * anyone building from `.env.example`.
         *
         * It is not stored, cannot be edited or deleted, and any saved profile
         * takes precedence over it.
         */
        val buildConfigProfile: TtsProfile? =
            if (BuildConfig.AZURE_SPEECH_KEY.isRealSecret() &&
                BuildConfig.AZURE_SPEECH_REGION.isRealSecret()
            ) {
                TtsProfile(
                    id = BUILD_CONFIG_ID,
                    name = "Built-in key",
                    speechKey = BuildConfig.AZURE_SPEECH_KEY,
                    region = BuildConfig.AZURE_SPEECH_REGION
                )
            } else {
                null
            }
    }
}

/**
 * False for a value the build never actually got: blank, or one of the
 * `YOUR_*` placeholders `.env.example` supplies when no `.env` is present.
 * Treating a placeholder as a real key would tell the user Settings that a
 * built-in voice is available and then fail on the first utterance.
 */
private fun String.isRealSecret(): Boolean =
    isNotBlank() && !startsWith("YOUR_")

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
