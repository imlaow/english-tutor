package com.example.data.remote

import java.net.HttpURLConnection

/**
 * Request/response plumbing shared by the HttpURLConnection-based clients. Each
 * helper takes the [provider] label so a failure names the backend the user
 * actually configured.
 */

internal fun HttpURLConnection.writeBody(body: String) {
    outputStream.use { it.write(body.toByteArray()) }
}

/** Reads the whole response, turning a non-2xx status into an [AiApiException]. */
internal fun HttpURLConnection.readBodyOrThrow(provider: String): String {
    val status = responseCode
    val stream = if (status in 200..299) inputStream else errorStream
    val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    if (status !in 200..299) {
        throw AiApiException("$provider request failed with HTTP $status: $body")
    }
    return body
}

/**
 * Reads a server-sent event stream, invoking [onData] with the payload of each
 * `data:` line. Comments, blank keep-alive lines and the `[DONE]` sentinel are
 * dropped, so callers only see frames that should carry content.
 *
 * Inline so [onData] may suspend — callers emit into a flow from it.
 */
internal inline fun HttpURLConnection.streamSseData(
    provider: String,
    onData: (String) -> Unit
) {
    val status = responseCode
    if (status !in 200..299) {
        val body = errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        throw AiApiException("$provider stream request failed with HTTP $status: $body")
    }
    inputStream.bufferedReader().use { reader ->
        reader.lineSequence().forEach { line ->
            val data = line.removePrefixOrNull("data:")?.trim() ?: return@forEach
            if (data.isEmpty() || data == "[DONE]") return@forEach
            onData(data)
        }
    }
}

internal fun String.removePrefixOrNull(prefix: String): String? =
    if (startsWith(prefix)) substring(prefix.length) else null
