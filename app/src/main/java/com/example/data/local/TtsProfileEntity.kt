package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One saved Azure Speech configuration. Several profiles may exist (a personal
 * and a work subscription, or the same key in two regions with two voices);
 * only the profile the user marked active is used for playback.
 *
 * There is no engine column because Azure is the only engine the app speaks —
 * adding a second one is an `ALTER TABLE` away, in the shape [ApiProfileEntity]
 * already uses for `api_spec`.
 *
 * The four expression columns are text rather than numbers so that blank can
 * mean "leave it to the voice", the same contract `voice` already had — a
 * numeric column would need a sentinel value to say the same thing. They are
 * per-utterance SSML, not part of the subscription.
 */
@Entity(tableName = "tts_profile")
data class TtsProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "speech_key")
    val speechKey: String,

    // The Azure region the subscription lives in, e.g. "eastus". No default:
    // a key is only valid in the region it was issued for.
    @ColumnInfo(name = "region")
    val region: String,

    // Blank means "use the default voice".
    @ColumnInfo(name = "voice")
    val voice: String,

    // The Azure speaking style, e.g. "excited". Blank means no style element is
    // emitted at all; a style the voice does not support is silently ignored by
    // the service, so this is not validated anywhere.
    @ColumnInfo(name = "style")
    val style: String,

    // How strongly the style is applied, 0.01-2. Blank means Azure's own
    // default, and it has no effect without a style to modulate.
    @ColumnInfo(name = "style_degree")
    val styleDegree: String,

    // An SSML prosody pitch, e.g. "+12%", "-2st" or "x-high". Blank means the
    // voice's own pitch.
    @ColumnInfo(name = "pitch")
    val pitch: String,

    // An SSML prosody rate, e.g. "+10%". Blank means the voice's own speed.
    @ColumnInfo(name = "rate")
    val rate: String,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int
)
