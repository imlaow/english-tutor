package com.example

import com.example.data.settings.VoiceExpression
import com.example.manager.buildSsml
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The SSML wire format, asserted byte for byte.
 *
 * None of this is visible in a screenshot golden — CLAUDE.md's "screenshots do
 * not protect the data path" applies just as much to what goes out over the
 * network. The expected strings are spelled out in full rather than assembled,
 * so a stray attribute or a moved namespace has to be looked at rather than
 * quietly matched: the namespaces in particular are `http://`, which is what
 * every Microsoft example uses, and are pinned here so they cannot drift to
 * `https://`.
 *
 * Plain JUnit, no Robolectric — the builder touches neither Android nor the
 * Speech SDK.
 */
class SsmlTest {

    private val prologue =
        "<speak version=\"1.0\" " +
            "xmlns=\"http://www.w3.org/2001/10/synthesis\" " +
            "xmlns:mstts=\"http://www.w3.org/2001/mstts\" " +
            "xml:lang=\"en-US\">"

    @Test
    fun `all blank produces a bare voice element`() {
        assertEquals(
            prologue +
                "<voice name=\"en-US-JennyNeural\">Hello there.</voice></speak>",
            buildSsml("en-US-JennyNeural", VoiceExpression(), "Hello there.")
        )
    }

    @Test
    fun `a standard voice carries the style, degree, pitch and rate`() {
        assertEquals(
            prologue +
                "<voice name=\"en-US-AshleyNeural\">" +
                "<mstts:express-as style=\"excited\" styledegree=\"1.6\">" +
                "<prosody pitch=\"+12%\" rate=\"+5%\">Hello there.</prosody>" +
                "</mstts:express-as></voice></speak>",
            buildSsml(
                "en-US-AshleyNeural",
                VoiceExpression(style = "excited", styleDegree = "1.6", pitch = "+12%", rate = "+5%"),
                "Hello there."
            )
        )
    }

    /**
     * HD voices accept `mstts:express-as` but reject `<prosody>` outright, so the
     * prosody element is dropped rather than taking the document down with it.
     *
     * This branch cannot be exercised against the subscription this app is built
     * for: HD voices do not exist in `centralus`, so no amount of on-device
     * testing will reach it. That is a reason to keep the branch and this test,
     * not to delete either as dead code — a second profile pointed at an `eastus`
     * resource reaches it with no code change at all.
     */
    @Test
    fun `an HD voice keeps the style and drops the prosody`() {
        assertEquals(
            prologue +
                "<voice name=\"en-US-Ana:DragonHDOmniLatestNeural\">" +
                "<mstts:express-as style=\"excited\" styledegree=\"1.6\">Hello there." +
                "</mstts:express-as></voice></speak>",
            buildSsml(
                "en-US-Ana:DragonHDOmniLatestNeural",
                VoiceExpression(style = "excited", styleDegree = "1.6", pitch = "+12%", rate = "+5%"),
                "Hello there."
            )
        )
    }

    @Test
    fun `an HD voice with only a pitch degrades to a bare voice element`() {
        // And specifically not to an empty <prosody></prosody>, which would be
        // valid XML the service has no use for.
        assertEquals(
            prologue +
                "<voice name=\"en-US-Ana:DragonHDOmniLatestNeural\">Hello there.</voice></speak>",
            buildSsml(
                "en-US-Ana:DragonHDOmniLatestNeural",
                VoiceExpression(pitch = "+12%"),
                "Hello there."
            )
        )
    }

    @Test
    fun `a style degree without a style is dropped`() {
        // A degree modulates a style; with no style there is nowhere to put it,
        // and an orphan styledegree attribute has no element to sit on.
        assertEquals(
            prologue +
                "<voice name=\"en-US-JennyNeural\">Hello there.</voice></speak>",
            buildSsml("en-US-JennyNeural", VoiceExpression(styleDegree = "1.6"), "Hello there.")
        )
    }

    @Test
    fun `ampersands and angle brackets in the body are escaped`() {
        // The body is model-generated prose, so a literal & is ordinary. Left
        // unescaped it makes the document malformed, and the only symptom is a
        // Canceled line in logcat.
        assertEquals(
            prologue +
                "<voice name=\"en-US-JennyNeural\">Tom &amp; Jerry &lt;3</voice></speak>",
            buildSsml("en-US-JennyNeural", VoiceExpression(), "Tom & Jerry <3")
        )
    }

    @Test
    fun `a quote in an attribute value cannot break out of the attribute`() {
        assertEquals(
            prologue +
                "<voice name=\"en-US-JennyNeural\">" +
                "<prosody pitch=\"+1&quot;0%\">Hello there.</prosody>" +
                "</voice></speak>",
            buildSsml("en-US-JennyNeural", VoiceExpression(pitch = "+1\"0%"), "Hello there.")
        )
    }

    @Test
    fun `the language comes from the voice name`() {
        assertEquals(
            "<speak version=\"1.0\" " +
                "xmlns=\"http://www.w3.org/2001/10/synthesis\" " +
                "xmlns:mstts=\"http://www.w3.org/2001/mstts\" " +
                "xml:lang=\"en-GB\">" +
                "<voice name=\"en-GB-RyanNeural\">Hello there.</voice></speak>",
            buildSsml("en-GB-RyanNeural", VoiceExpression(), "Hello there.")
        )
    }
}
