package com.example.data.repository

import com.example.data.local.ChatMessageDao
import com.example.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val chatMessageDao: ChatMessageDao
) {

    fun observeHistory(): Flow<List<ChatMessageEntity>> =
        chatMessageDao.observeAll()

    suspend fun saveMessage(message: ChatMessageEntity) =
        chatMessageDao.insert(message)

    suspend fun deleteSession(sessionId: String) =
        chatMessageDao.deleteBySessionId(sessionId)

    suspend fun clearHistory() =
        chatMessageDao.deleteAll()
}
