package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    // Oldest-first, matching the in-conversation ordering the UI expects.
    @Query("SELECT * FROM chat_message ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("DELETE FROM chat_message WHERE session_id = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)

    @Query("DELETE FROM chat_message")
    suspend fun deleteAll()
}
