package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.viewmodel.OnboardingUiState
import com.example.viewmodel.OnboardingViewModel

private val ENGLISH_LEVELS = listOf(
    "Beginner", "Elementary", "Intermediate", "Upper-Intermediate", "Advanced"
)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isProfileSaved) {
        if (uiState.isProfileSaved) onOnboardingComplete()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OnboardingContent(
            uiState = uiState,
            onOpenSettings = onOpenSettings,
            onLevelChanged = viewModel::onLevelChanged,
            onGoalChanged = viewModel::onGoalChanged,
            onInterestsChanged = viewModel::onInterestsChanged,
            onPracticeMinutesChanged = viewModel::onPracticeMinutesChanged,
            onSubmit = viewModel::completeOnboarding
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

/**
 * The questionnaire without its ViewModel: the warm top bar over the form.
 *
 * Visible to the module (not private) for the same reason [SettingsContent] is —
 * the screenshot specimens render it directly, and it takes plain values and
 * lambdas. The snackbar stays outside, since it belongs to the ViewModel's error
 * flow rather than to the layout.
 *
 * The handoff never drew this screen, but it does specify the controls: the
 * shell is its top bar and 44dp button, the fields are its `.input` and the level
 * picker its `.seg-opt`, both by way of [WarmTextField] and [ChoicePill].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OnboardingContent(
    uiState: OnboardingUiState,
    onOpenSettings: () -> Unit,
    onLevelChanged: (String) -> Unit,
    onGoalChanged: (String) -> Unit,
    onInterestsChanged: (String) -> Unit,
    onPracticeMinutesChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WarmTopBar(
            title = "Welcome",
            actions = {
                IconButton44(
                    icon = painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings",
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("onboarding_settings_button")
                )
            }
        )

        val inputsEnabled = !uiState.isGeneratingProfile

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Tell us about yourself",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "We'll use your answers to build a personalized learning profile.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                WarmFieldLabel("How would you rate your English level?")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ENGLISH_LEVELS.forEach { level ->
                        ChoicePill(
                            label = level,
                            selected = uiState.answers.selfAssessedLevel == level,
                            onClick = { onLevelChanged(level) },
                            enabled = inputsEnabled,
                            modifier = Modifier.testTag("level_chip_$level")
                        )
                    }
                }
            }

            WarmTextField(
                value = uiState.answers.learningGoal,
                onValueChange = onGoalChanged,
                label = "What is your learning goal?",
                enabled = inputsEnabled,
                placeholder = "e.g. Speak confidently at work",
                modifier = Modifier.testTag("goal_field")
            )

            WarmTextField(
                value = uiState.answers.interests,
                onValueChange = onInterestsChanged,
                label = "What topics interest you?",
                enabled = inputsEnabled,
                placeholder = "e.g. Travel, movies, technology",
                modifier = Modifier.testTag("interests_field")
            )

            WarmTextField(
                value = uiState.answers.dailyPracticeMinutes,
                onValueChange = onPracticeMinutesChanged,
                label = "Minutes you can practice per day",
                enabled = inputsEnabled,
                placeholder = "e.g. 30",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.testTag("minutes_field")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSubmit,
                enabled = uiState.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_button")
            ) {
                if (uiState.isGeneratingProfile) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create my profile")
                }
            }

            if (uiState.isGeneratingProfile) {
                Text(
                    text = "Generating your learning profile...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
