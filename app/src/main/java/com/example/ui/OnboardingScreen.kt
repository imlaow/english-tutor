package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.OnboardingViewModel

private val ENGLISH_LEVELS = listOf(
    "Beginner", "Elementary", "Intermediate", "Upper-Intermediate", "Advanced"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome") },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("onboarding_settings_button")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val inputsEnabled = !uiState.isGeneratingProfile

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
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

            Text(
                text = "How would you rate your English level?",
                style = MaterialTheme.typography.titleMedium
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ENGLISH_LEVELS.forEach { level ->
                    FilterChip(
                        selected = uiState.answers.selfAssessedLevel == level,
                        onClick = { viewModel.onLevelChanged(level) },
                        enabled = inputsEnabled,
                        label = { Text(level) },
                        modifier = Modifier.testTag("level_chip_$level")
                    )
                }
            }

            OutlinedTextField(
                value = uiState.answers.learningGoal,
                onValueChange = viewModel::onGoalChanged,
                enabled = inputsEnabled,
                label = { Text("What is your learning goal?") },
                placeholder = { Text("e.g. Speak confidently at work") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("goal_field")
            )

            OutlinedTextField(
                value = uiState.answers.interests,
                onValueChange = viewModel::onInterestsChanged,
                enabled = inputsEnabled,
                label = { Text("What topics interest you?") },
                placeholder = { Text("e.g. Travel, movies, technology") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("interests_field")
            )

            OutlinedTextField(
                value = uiState.answers.dailyPracticeMinutes,
                onValueChange = viewModel::onPracticeMinutesChanged,
                enabled = inputsEnabled,
                label = { Text("Minutes you can practice per day") },
                placeholder = { Text("e.g. 30") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("minutes_field")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = viewModel::completeOnboarding,
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
