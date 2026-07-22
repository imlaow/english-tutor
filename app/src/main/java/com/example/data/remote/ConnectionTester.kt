package com.example.data.remote

import kotlinx.coroutines.CancellationException

/** How one probe went. [detail] is free text meant to be shown verbatim. */
sealed interface ProbeOutcome {

    data class Success(val detail: String) : ProbeOutcome

    /** [summary] is the one-line cause; [detail] the provider's raw error body. */
    data class Failure(val summary: String, val detail: String?) : ProbeOutcome
}

/**
 * Both probes always run, so a provider that answers normal requests but
 * rejects `stream: true` — common for OpenAI-compatible proxies — reports one
 * success and one failure rather than a single verdict.
 */
data class ConnectionTestResult(
    val nonStreaming: ProbeOutcome,
    val streaming: ProbeOutcome
)

/**
 * Sends a throwaway prompt through a service to check that its base URL, key
 * and model name all work together. Takes an [AiModelService] rather than a
 * profile so it can be exercised without a network or a database.
 */
object ConnectionTester {

    suspend fun test(service: AiModelService): ConnectionTestResult =
        ConnectionTestResult(
            nonStreaming = probe {
                service.generateContent(SYSTEM_PROMPT, USER_PROMPT)
                null
            },
            streaming = probe {
                var chunks = 0
                service.generateContentStream(SYSTEM_PROMPT, USER_PROMPT).collect { chunks++ }
                // A 200 that yields nothing means the endpoint accepted the
                // stream flag and ignored it — not a working stream.
                if (chunks == 0) throw AiApiException("Endpoint returned no stream data.")
                "$chunks chunks"
            }
        )

    /**
     * Runs [block], timing it and converting any failure into a [ProbeOutcome].
     * [block] may return an extra fact to prefix the elapsed time with.
     */
    private suspend fun probe(block: suspend () -> String?): ProbeOutcome {
        val startedAt = System.currentTimeMillis()
        return try {
            val extra = block()
            val elapsed = "${System.currentTimeMillis() - startedAt} ms"
            ProbeOutcome.Success(if (extra == null) elapsed else "$extra · $elapsed")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.toFailure()
        }
    }

    /**
     * The services phrase transport errors as "... HTTP <status>: <body>", so
     * splitting on that colon separates a headline the user can act on from the
     * raw body, which is often a wall of JSON or HTML.
     */
    private fun Exception.toFailure(): ProbeOutcome.Failure {
        val message = message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
        val marker = message.indexOf("HTTP ")
        if (this is AiApiException && marker >= 0) {
            val afterMarker = message.substring(marker)
            val separator = afterMarker.indexOf(':')
            if (separator >= 0) {
                return ProbeOutcome.Failure(
                    summary = afterMarker.substring(0, separator).trim(),
                    detail = afterMarker.substring(separator + 1).trim().takeIf { it.isNotEmpty() }
                )
            }
        }
        return ProbeOutcome.Failure(summary = message, detail = null)
    }

    private const val SYSTEM_PROMPT = "You are a connection test."
    private const val USER_PROMPT = "Reply with exactly: OK"
}
