package com.example

import com.example.data.remote.MissingApiKeyException
import com.example.data.repository.TopicGenerator
import com.example.viewmodel.TopicsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The view model generates a set on creation and maps failures to a friendly
 * message. A fake [TopicGenerator] stands in for the live network call.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TopicsViewModelTest {

    @Before
    fun setUp() {
        // Unconfined so init's refresh() runs eagerly, like ChatSessionTest.
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun generatorReturning(topics: List<String>) = object : TopicGenerator {
        override suspend fun generateTopics(count: Int): List<String> = topics
    }

    private fun generatorThrowing(error: Throwable) = object : TopicGenerator {
        override suspend fun generateTopics(count: Int): List<String> = throw error
    }

    @Test
    fun `generates topics on creation`() = runBlocking {
        val viewModel = TopicsViewModel(
            generatorReturning(listOf("Your weekend plans", "A movie you loved", "Favorite food"))
        )

        val topics = withTimeout(5_000L) { viewModel.topics.first { it.isNotEmpty() } }
        assertEquals(listOf("Your weekend plans", "A movie you loved", "Favorite food"), topics)
        assertFalse(viewModel.isLoading.value)
        assertEquals(null, viewModel.error.value)
    }

    @Test
    fun `a missing key surfaces the settings hint`() = runBlocking {
        val viewModel = TopicsViewModel(generatorThrowing(MissingApiKeyException("no key")))

        val error = withTimeout(5_000L) { viewModel.error.first { it != null } }
        assertEquals("Add an API provider in Settings to see topic suggestions.", error)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `a generic failure surfaces a retry hint`() = runBlocking {
        val viewModel = TopicsViewModel(generatorThrowing(RuntimeException("boom")))

        val error = withTimeout(5_000L) { viewModel.error.first { it != null } }
        assertEquals("Couldn't load topics. Tap refresh to try again.", error)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `refresh replaces the previous set`() = runBlocking {
        val viewModel = TopicsViewModel(
            object : TopicGenerator {
                private var call = 0
                override suspend fun generateTopics(count: Int): List<String> =
                    if (call++ == 0) listOf("First set") else listOf("Second set")
            }
        )

        withTimeout(5_000L) { viewModel.topics.first { it == listOf("First set") } }
        viewModel.refresh()
        val refreshed = withTimeout(5_000L) { viewModel.topics.first { it == listOf("Second set") } }
        assertEquals(listOf("Second set"), refreshed)
    }
}
