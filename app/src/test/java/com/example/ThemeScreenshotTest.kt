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
 * Renders the design system in both color schemes so a re-theme can be reviewed
 * as images instead of inferred from a diff.
 *
 * Record new goldens: `./gradlew recordRoborazziDebug`
 * Check against them: `./gradlew verifyRoborazziDebug`
 * Both need Java 21 (`JAVA_HOME=/home/Laow/jdk-21`) for Robolectric.
 *
 * [MyApplicationTheme]'s arguments are pinned explicitly: `dynamicColor = false`
 * so the wallpaper palette can never leak into a golden, and `darkTheme` so the
 * host's night-mode setting cannot flip the result.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ThemeScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun colors_light() = capture("colors_light", darkTheme = false) { ColorSwatches() }

    @Test fun colors_dark() = capture("colors_dark", darkTheme = true) { ColorSwatches() }

    @Test fun typography_light() = capture("typography_light", darkTheme = false) { TypeSpecimens() }

    @Test fun typography_dark() = capture("typography_dark", darkTheme = true) { TypeSpecimens() }

    @Test fun components_light() = capture("components_light", darkTheme = false) { ComponentGallery() }

    @Test fun components_dark() = capture("components_dark", darkTheme = true) { ComponentGallery() }

    @Test fun chatBubbles_light() = capture("chat_bubbles_light", darkTheme = false) { ChatBubbles() }

    @Test fun chatBubbles_dark() = capture("chat_bubbles_dark", darkTheme = true) { ChatBubbles() }

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

    private fun capture(fileName: String, darkTheme: Boolean, content: @Composable () -> Unit) {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = darkTheme, dynamicColor = false) {
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
