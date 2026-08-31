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

    @ColumnInfo(name = "enabled")
    val enabled: Boolean,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int
)
