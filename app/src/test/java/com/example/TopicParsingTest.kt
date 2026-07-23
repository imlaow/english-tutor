package com.example

import com.example.data.repository.TopicRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers [TopicRepository.parseTopics]: it must read the topics array, survive a
 * markdown fence the model may wrap the JSON in, and cap the list at [count].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TopicParsingTest {

    @Test
    fun `parses a clean topics array`() {
        val json = """{"topics": ["Your weekend plans", "A movie you loved", "Favorite food"]}"""
        assertEquals(
            listOf("Your weekend plans", "A movie you loved", "Favorite food"),
            TopicRepository.parseTopics(json, count = 3)
        )
    }

    @Test
    fun `strips a markdown code fence before parsing`() {
        val json = "```json\n{\"topics\": [\"Travel dreams\", \"Your morning routine\"]}\n```"
        assertEquals(
            listOf("Travel dreams", "Your morning routine"),
            TopicRepository.parseTopics(json, count = 3)
        )
    }

    @Test
    fun `caps the result at count and drops blank entries`() {
        val json = """{"topics": ["One", "  ", "Two", "Three", "Four"]}"""
        assertEquals(
            listOf("One", "Two", "Three"),
            TopicRepository.parseTopics(json, count = 3)
        )
    }
}
