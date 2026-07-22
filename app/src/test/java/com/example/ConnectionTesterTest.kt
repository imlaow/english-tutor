package com.example

import com.example.data.remote.AiApiException
import com.example.data.remote.AiModelService
import com.example.data.remote.ConnectionTester
import com.example.data.remote.ProbeOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Drives [ConnectionTester] through a fake service, so the probe logic is
 * covered without a network or a configured provider.
 */
class ConnectionTesterTest {

    /** [streamChunks] is consulted only when [streamError] is null. */
    private class FakeAiService(
        private val reply: String = "OK",
        private val replyError: Exception? = null,
        private val streamChunks: List<String> = listOf("O", "K"),
        private val streamError: Exception? = null
    ) : AiModelService {

        override suspend fun generateContent(
            systemPrompt: String,
            userPrompt: String,
            responseMimeType: String?
        ): String = replyError?.let { throw it } ?: reply

        override fun generateContentStream(
            systemPrompt: String,
            userPrompt: String,
            responseMimeType: String?
        ): Flow<String> = flow {
            streamError?.let { throw it }
            streamChunks.forEach { emit(it) }
        }
    }

    @Test
    fun `both probes succeed and streaming reports its chunk count`() = runTest {
        val result = ConnectionTester.test(
            FakeAiService(streamChunks = listOf("a", "b", "c"))
        )

        assertTrue(result.nonStreaming is ProbeOutcome.Success)
        val streaming = result.streaming as ProbeOutcome.Success
        assertTrue(
            "expected the chunk count in \"${streaming.detail}\"",
            streaming.detail.startsWith("3 chunks")
        )
    }

    @Test
    fun `a streaming failure still reports the non-streaming success`() = runTest {
        val result = ConnectionTester.test(
            FakeAiService(
                streamError = AiApiException(
                    "OpenAI stream request failed with HTTP 400: {\"error\":\"stream unsupported\"}"
                )
            )
        )

        assertTrue(result.nonStreaming is ProbeOutcome.Success)
        val failure = result.streaming as ProbeOutcome.Failure
        assertEquals("HTTP 400", failure.summary)
        assertEquals("{\"error\":\"stream unsupported\"}", failure.detail)
    }

    @Test
    fun `a stream that yields no chunks counts as a failure`() = runTest {
        val result = ConnectionTester.test(FakeAiService(streamChunks = emptyList()))

        assertTrue(result.nonStreaming is ProbeOutcome.Success)
        val failure = result.streaming as ProbeOutcome.Failure
        assertEquals("Endpoint returned no stream data.", failure.summary)
    }

    @Test
    fun `a non-streaming failure does not skip the streaming probe`() = runTest {
        val result = ConnectionTester.test(
            FakeAiService(
                replyError = AiApiException(
                    "Gemini request failed with HTTP 401: {\"error\":\"bad key\"}"
                )
            )
        )

        val failure = result.nonStreaming as ProbeOutcome.Failure
        assertEquals("HTTP 401", failure.summary)
        assertEquals("{\"error\":\"bad key\"}", failure.detail)
        assertTrue(result.streaming is ProbeOutcome.Success)
    }

    @Test
    fun `a non-HTTP failure falls back to the exception message`() = runTest {
        val result = ConnectionTester.test(
            FakeAiService(replyError = java.net.UnknownHostException("api.example.com"))
        )

        val failure = result.nonStreaming as ProbeOutcome.Failure
        assertEquals("api.example.com", failure.summary)
        assertEquals(null, failure.detail)
    }
}
