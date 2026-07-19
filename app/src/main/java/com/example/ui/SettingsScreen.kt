package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.remote.GeminiApiService
import com.example.data.remote.OpenAiApiService
import com.example.data.settings.ApiProvider
import com.example.data.settings.ModelApiSettings
import com.example.data.settings.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository.getInstance(context) }
    var draft by remember { mutableStateOf(settingsRepository.settings.value) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Model API",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose which API specification your AI tutor uses and how to reach it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ApiProvider.entries.forEachIndexed { index, provider ->
                    SegmentedButton(
                        selected = draft.provider == provider,
                        onClick = { draft = draft.copy(provider = provider) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ApiProvider.entries.size
                        ),
                        modifier = Modifier.testTag("provider_${provider.name.lowercase()}")
                    ) {
                        Text(if (provider == ApiProvider.GEMINI) "Gemini" else "OpenAI")
                    }
                }
            }

            when (draft.provider) {
                ApiProvider.GEMINI -> {
                    OutlinedTextField(
                        value = draft.geminiBaseUrl,
                        onValueChange = { draft = draft.copy(geminiBaseUrl = it) },
                        label = { Text("Base URL") },
                        placeholder = { Text(GeminiApiService.DEFAULT_BASE_URL) },
                        supportingText = { Text("Leave empty to use the default.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.geminiApiKey,
                        onValueChange = { draft = draft.copy(geminiApiKey = it) },
                        label = { Text("API key") },
                        supportingText = { Text("Leave empty to use the app's built-in key.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.geminiModel,
                        onValueChange = { draft = draft.copy(geminiModel = it) },
                        label = { Text("Model") },
                        placeholder = { Text(GeminiApiService.DEFAULT_MODEL) },
                        supportingText = { Text("Leave empty to use the default.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ApiProvider.OPENAI -> {
                    OutlinedTextField(
                        value = draft.openAiBaseUrl,
                        onValueChange = { draft = draft.copy(openAiBaseUrl = it) },
                        label = { Text("Base URL") },
                        placeholder = { Text(OpenAiApiService.DEFAULT_BASE_URL) },
                        supportingText = { Text("Leave empty to use the default. Any OpenAI-compatible endpoint works.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.openAiApiKey,
                        onValueChange = { draft = draft.copy(openAiApiKey = it) },
                        label = { Text("API key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.openAiModel,
                        onValueChange = { draft = draft.copy(openAiModel = it) },
                        label = { Text("Model") },
                        placeholder = { Text(OpenAiApiService.DEFAULT_MODEL) },
                        supportingText = { Text("Leave empty to use the default.") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        settingsRepository.save(normalized(draft))
                        draft = settingsRepository.settings.value
                        scope.launch { snackbarHostState.showSnackbar("Settings saved") }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_settings_button")
                ) {
                    Text("Save")
                }
            }
        }
    }
}

/** Trims stray whitespace; empty fields stay empty and fall back to defaults at request time. */
private fun normalized(draft: ModelApiSettings): ModelApiSettings = draft.copy(
    geminiBaseUrl = draft.geminiBaseUrl.trim(),
    geminiApiKey = draft.geminiApiKey.trim(),
    geminiModel = draft.geminiModel.trim(),
    openAiBaseUrl = draft.openAiBaseUrl.trim(),
    openAiApiKey = draft.openAiApiKey.trim(),
    openAiModel = draft.openAiModel.trim()
)
