package com.example

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.ui.ChatBubble
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the design system so a re-theme can be reviewed as images instead of
 * inferred from a diff.
 *
 * Record new goldens: `./gradlew recordRoborazziDebug`
 * Check against them: `./gradlew verifyRoborazziDebug`
 * Both need Java 21 (`JAVA_HOME=/home/Laow/jdk-21`) for Robolectric.
 *
 * [MyApplicationTheme] takes no arguments: the design ships a single light
 * palette and dynamic color is off, so neither the wallpaper nor the host's
 * night-mode setting can leak into a golden.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ThemeScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun colors_light() = capture("colors_light") { ColorSwatches() }

    @Test fun typography_light() = capture("typography_light") { TypeSpecimens() }

    @Test fun components_light() = capture("components_light") { ComponentGallery() }

    @Test fun chatBubbles_light() = capture("chat_bubbles_light") { ChatBubbles() }

    /**
     * The real [ChatBubble] from the chat screen — a pure composable with no
     * ViewModel, so it can be rendered directly. Covers all three variants:
     * learner, tutor, and tutor-with-correction.
     */
    @Composable
    private fun ChatBubbles() {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChatBubble(text = "I go to the beach yesterday with my friends.", isUser = true)
            ChatBubble(
                text = "That sounds fun! What did you do there?",
                isUser = false,
                grammarCorrection = "Use the past tense: \"I went to the beach yesterday.\"",
            )
            ChatBubble(text = "We swim and eat ice cream.", isUser = true)
            ChatBubble(text = "Lovely. Which flavour did you pick?", isUser = false)
        }
    }

    private fun capture(fileName: String, content: @Composable () -> Unit) {
        composeTestRule.setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    content()
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/$fileName.png")
    }
}
