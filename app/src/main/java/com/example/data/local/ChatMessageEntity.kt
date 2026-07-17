package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_message")
data class ChatMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    // Messages saved before schema v3 all carry the "legacy" session id.
    @ColumnInfo(name = "session_id", defaultValue = "legacy")
    val sessionId: String,

    @ColumnInfo(name = "user_text")
    val userText: String,

    @ColumnInfo(name = "ai_response")
    val aiResponse: String,

    @ColumnInfo(name = "grammar_correction")
    val grammarCorrection: String?,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)
