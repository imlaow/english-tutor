package com.example.manager

import com.example.data.settings.VoiceExpression

/**
 * Builds the SSML document one utterance is spoken from.
 *
 * This lives beside [TtsManager] because it encodes Azure's wire format, which
 * is this package's business (ARCHITECTURE.md §2). It imports nothing from
 * Android or from the Speech SDK, so it is testable as plain Kotlin.
 *
 * The nesting is fixed — `voice > mstts:express-as > prosody > text` — and each
 * of the two inner elements appears only when it has something to say:
 *
 * - `<mstts:express-as>` whenever [VoiceExpression.style] is non-blank. Its
 *   `styledegree` rides along only when a degree is given too; a degree with no
 *   style has nowhere to live.
 * - `<prosody>` whenever a rate is given, and additionally carrying the pitch
 *   when the voice is one that honours it — see [supportsPitch], which is not
 *   the same question for both attributes.
 *
 * All blank gives a bare `<voice>` around the text, which is byte for byte what
 * the service received before any of this was configurable.
 *
 * A note on failure: Azure discards an `express-as` whose style the voice does
 * not support and speaks neutrally instead, and ignores an out-of-range
 * `styledegree` — in both cases with no error, no cancellation and no log line.
 * Malformed XML is the one loud failure, which is what makes [escapeXml]
 * load-bearing rather than decorative.
 */
fun buildSsml(voice: String, expression: VoiceExpression, text: String): String {
    val body = StringBuilder()

    val prosody = attributes(
        "pitch" to if (supportsPitch(voice)) expression.pitch else "",
        "rate" to expression.rate
    )
    val expressAs = if (expression.style.isNotBlank()) {
        // Azure spells it `styledegree`: all lowercase, one word.
        attributes("style" to expression.style, "styledegree" to expression.styleDegree)
    } else {
        ""
    }

    if (expressAs.isNotEmpty()) body.append("<mstts:express-as$expressAs>")
    if (prosody.isNotEmpty()) body.append("<prosody$prosody>")
    body.append(escapeXml(text))
    if (prosody.isNotEmpty()) body.append("</prosody>")
    if (expressAs.isNotEmpty()) body.append("</mstts:express-as>")

    return "<speak version=\"1.0\" " +
        "xmlns=\"http://www.w3.org/2001/10/synthesis\" " +
        "xmlns:mstts=\"http://www.w3.org/2001/mstts\" " +
        "xml:lang=\"${escapeXml(languageOf(voice))}\">" +
        "<voice name=\"${escapeXml(voice)}\">" +
        body +
        "</voice></speak>"
}

/**
 * Whether `<prosody pitch>` reaches [voice] — only a standard voice, which is
 * every name without a colon.
 *
 * `rate` is deliberately not asked the same question: it is emitted for every
 * voice. The two attributes sit in one element but are honoured separately, and
 * this was measured rather than read off the documentation, which says only that
 * `<prosody>` is unsupported on HD — true of pitch, false of rate:
 *
 * | voice                                  | `rate="-50%"`      | `pitch="+30%"`            |
 * |----------------------------------------|--------------------|---------------------------|
 * | `en-US-SaraNeural` (standard)          | 2.00x duration     | 222.2Hz -> 290.9Hz, x1.309 |
 * | `en-US-Harper:MAI-Voice-2`             | 2.04x duration     | 212Hz -> 172.7/190.2/202.6 |
 * | `en-US-Ava:DragonHDLatestNeural`       | 1.75x duration     | lost in run-to-run spread |
 * | `en-us-jelly:DragonHDOmniLatestNeural` | 1.68x duration     | lost in run-to-run spread |
 *
 * The standard voice applies both exactly and reproducibly — three renders of
 * the same request gave 222.2Hz every time. The others slow down when asked to
 * but do not move their fundamental: MAI scatters it *downward* under a raised
 * pitch, and the Dragon models vary more between two identical renders (jelly:
 * 333.4Hz then 258.2Hz) than the request could account for. So pitch is dropped
 * for them, and rate is not: no tested model rejects the element, so keeping the
 * attribute that works costs nothing.
 */
private fun supportsPitch(voice: String): Boolean = !voice.contains(':')

/**
 * The non-blank [pairs] as ` name="value"`, or an empty string when none are —
 * which is how a caller decides whether the element is worth emitting at all.
 */
private fun attributes(vararg pairs: Pair<String, String>): String =
    pairs.filter { it.second.isNotBlank() }
        .joinToString("") { (name, value) -> " $name=\"${escapeXml(value)}\"" }

/**
 * The `xml:lang` a voice name implies: its first two hyphen segments, so
 * `en-GB-RyanNeural` is `en-GB` and `en-US-Ana:DragonHDOmniLatestNeural` is
 * `en-US`. Anything that does not have two segments falls back to [FALLBACK_LANG].
 */
private fun languageOf(voice: String): String {
    val parts = voice.split('-')
    return if (parts.size >= 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
        "${parts[0]}-${parts[1]}"
    } else {
        FALLBACK_LANG
    }
}

private const val FALLBACK_LANG = "en-US"

/**
 * Escapes [value] for use as either XML text or a double-quoted attribute.
 *
 * `&` goes first, or the ampersands introduced by the later replacements would
 * be escaped a second time. This is not hypothetical prudence: the tutor's reply
 * is model-generated prose, so a literal `&` turns up on its own, and an
 * unescaped one makes the document malformed — whose only symptom is a
 * `ResultReason.Canceled` line in logcat and silence on the device.
 */
internal fun escapeXml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")
