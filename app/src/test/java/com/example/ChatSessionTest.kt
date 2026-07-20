package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.remote.GeminiApiService
import com.example.data.repository.ChatRepository
import com.example.data.repository.ProfileRepository
import com.example.ui.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the fix for "tapping a past conversation on the history screen doesn't
 * switch to it": messages are tagged with a session id and the ViewModel can
 * resume any stored session.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatSessionTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        // Unconfined so viewModelScope coroutines (stateIn collectors) run without
        // needing the Android main looper to be idled manually.
        Dispatchers.setMain(Dispatchers.Unconfined)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun message(id: String, sessionId: String, timestamp: Long) = ChatMessageEntity(
        id = id,
        sessionId = sessionId,
        userText = "user $id",
        aiResponse = "tutor $id",
        grammarCorrection = null,
        timestamp = timestamp
    )

    private fun createViewModel() = ChatViewModel(
        profileRepository = ProfileRepository(
            userDao = db.userDao(),
            apiService = GeminiApiService(apiKey = "test-key")
        ),
        chatRepository = ChatRepository(chatMessageDao = db.chatMessageDao()),
        apiService = GeminiApiService(apiKey = "test-key")
    )

    @Test
    fun `resuming a past session shows exactly that conversation`() = runBlocking {
        val dao = db.chatMessageDao()
        dao.insert(message("a1", sessionId = "session-a", timestamp = 1_000L))
        dao.insert(message("a2", sessionId = "session-a", timestamp = 2_000L))
        dao.insert(message("b1", sessionId = "session-b", timestamp = 3_000L))

        val viewModel = createViewModel()

        // With no explicit selection, the most recent conversation is current.
        val current = withTimeout(5_000L) {
            viewModel.sessionHistory.first { it.isNotEmpty() }
        }
        assertEquals(listOf("b1"), current.map { it.id })

        // Tapping a past conversation on the history screen resumes it.
        viewModel.resumeSession("session-a")
        val resumed = withTimeout(5_000L) {
            viewModel.sessionHistory.first { messages -> messages.all { it.sessionId == "session-a" } && messages.isNotEmpty() }
        }
        assertEquals(listOf("a1", "a2"), resumed.map { it.id })
    }

    @Test
    fun `deleting a session removes only that conversation from history`() = runBlocking {
        val dao = db.chatMessageDao()
        dao.insert(message("a1", sessionId = "session-a", timestamp = 1_000L))
        dao.insert(message("a2", sessionId = "session-a", timestamp = 2_000L))
        dao.insert(message("b1", sessionId = "session-b", timestamp = 3_000L))

        val viewModel = createViewModel()
        withTimeout(5_000L) { viewModel.history.first { it.size == 3 } }

        viewModel.deleteSession("session-a")
        val remaining = withTimeout(5_000L) { viewModel.history.first { it.size == 1 } }
        assertEquals(listOf("b1"), remaining.map { it.id })
    }

    @Test
    fun `deleting the current session starts a fresh conversation`() = runBlocking {
        val dao = db.chatMessageDao()
        dao.insert(message("a1", sessionId = "session-a", timestamp = 1_000L))
        dao.insert(message("b1", sessionId = "session-b", timestamp = 2_000L))

        val viewModel = createViewModel()
        // session-b is the implicit current conversation (most recent).
        withTimeout(5_000L) { viewModel.sessionHistory.first { it.map { m -> m.id } == listOf("b1") } }

        viewModel.deleteSession("session-b")

        // The chat screen must not silently fall back to session-a.
        val fresh = withTimeout(5_000L) { viewModel.sessionHistory.first { it.isEmpty() } }
        assertEquals(emptyList<Any>(), fresh)
        withTimeout(5_000L) { viewModel.history.first { it.size == 1 } }
        Unit
    }

    @Test
    fun `clearing all history empties every conversation`() = runBlocking {
        val dao = db.chatMessageDao()
        dao.insert(message("a1", sessionId = "session-a", timestamp = 1_000L))
        dao.insert(message("b1", sessionId = "session-b", timestamp = 2_000L))

        val viewModel = createViewModel()
        withTimeout(5_000L) { viewModel.history.first { it.size == 2 } }

        viewModel.clearAllHistory()
        val cleared = withTimeout(5_000L) { viewModel.history.first { it.isEmpty() } }
        assertEquals(emptyList<Any>(), cleared)
    }

    @Test
    fun `starting a new session clears the on-screen conversation`() = runBlocking {
        val dao = db.chatMessageDao()
        dao.insert(message("a1", sessionId = "session-a", timestamp = 1_000L))

        val viewModel = createViewModel()
        withTimeout(5_000L) { viewModel.sessionHistory.first { it.isNotEmpty() } }

        viewModel.startNewSession()
        val fresh = withTimeout(5_000L) { viewModel.sessionHistory.first { it.isEmpty() } }
        assertEquals(emptyList<Any>(), fresh)
    }
}
