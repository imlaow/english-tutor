package com.example.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiProfileDao {

    @Upsert
    suspend fun upsert(profile: ApiProfileEntity)

    // Sorted by the order profiles were created, matching what the list shows.
    @Query("SELECT * FROM api_profile ORDER BY sort_order ASC")
    fun observeAll(): Flow<List<ApiProfileEntity>>

    // One-shot read for request time, so a request never races the Flow's first emission.
    @Query("SELECT * FROM api_profile ORDER BY sort_order ASC")
    suspend fun getAll(): List<ApiProfileEntity>

    @Query("SELECT * FROM api_profile WHERE id = :id")
    suspend fun getById(id: String): ApiProfileEntity?

    @Query("DELETE FROM api_profile WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE api_profile SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    // -1 when the table is empty, so the first profile lands at sort order 0.
    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM api_profile")
    suspend fun maxSortOrder(): Int
}
