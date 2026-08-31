package com.example.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TtsProfileDao {

    @Upsert
    suspend fun upsert(profile: TtsProfileEntity)

    // Sorted by the order profiles were created, matching what the list shows.
    @Query("SELECT * FROM tts_profile ORDER BY sort_order ASC")
    fun observeAll(): Flow<List<TtsProfileEntity>>

    // One-shot read for playback time, so speaking never races the Flow's first emission.
    @Query("SELECT * FROM tts_profile ORDER BY sort_order ASC")
    suspend fun getAll(): List<TtsProfileEntity>

    @Query("SELECT * FROM tts_profile WHERE id = :id")
    suspend fun getById(id: String): TtsProfileEntity?

    @Query("DELETE FROM tts_profile WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE tts_profile SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    // -1 when the table is empty, so the first profile lands at sort order 0.
    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM tts_profile")
    suspend fun maxSortOrder(): Int
}
