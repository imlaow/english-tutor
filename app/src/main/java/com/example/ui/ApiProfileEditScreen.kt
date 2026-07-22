package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.remote.ProbeOutcome
import com.example.data.settings.ApiSpec
import com.example.data.settings.defaultBaseUrl
import com.example.data.settings.defaultModel
import com.example.data.settings.displayName
import com.example.viewmodel.ApiProfileFormError
import com.example.viewmodel.ApiProfileViewModel
import com.example.viewmodel.ConnectionTestState

/**
 * New/edit form for a single API profile. [profileId] null means "new"; the
 * spec picker only swaps the placeholders, since one profile holds exactly one
 * base URL / key / model.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiProfileEditScreen(
    viewModel: ApiProfileViewModel,
    navController: NavController,
    profileId: String?
) {
    LaunchedEffect(profileId) { viewModel.loadForEdit(profileId) }

    val draft by viewModel.draft.collectAsState()
    val formError by viewModel.formError.collectAsState()
    val connectionTest by viewModel.connectionTest.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (profileId == null) "New provider" else "Edit provider",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.safePopBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        // Null only while an existing profile is being read back from Room.
        val profile = draft ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = profile.name,
                onValueChange = { value -> viewModel.updateDraft { it.copy(name = value) } },
                label = { Text("Name") },
                supportingText = {
                    Text(
                        if (formError == ApiProfileFormError.NAME) "Give the provider a name."
                        else "Shown in the provider list."
                    )
                },
                isError = formError == ApiProfileFormError.NAME,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_field")
            )

            Text(
                text = "API specification",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ApiSpec.entries.forEachIndexed { index, spec ->
                    SegmentedButton(
                        selected = profile.apiSpec == spec,
                        onClick = { viewModel.updateSpec(spec) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ApiSpec.entries.size
                        ),
                        modifier = Modifier.testTag("provider_${spec.name.lowercase()}")
                    ) {
                        Text(spec.displayName)
                    }
                }
            }

            OutlinedTextField(
                value = profile.baseUrl,
                onValueChange = { value -> viewModel.updateDraft { it.copy(baseUrl = value) } },
                label = { Text("Base URL") },
                placeholder = { Text(profile.apiSpec.defaultBaseUrl) },
                supportingText = {
                    Text(
                        if (profile.apiSpec == ApiSpec.OPENAI) {
                            "Leave empty to use the default. Any OpenAI-compatible endpoint works."
                        } else {
                            "Leave empty to use the default."
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = profile.apiKey,
                onValueChange = { value -> viewModel.updateDraft { it.copy(apiKey = value) } },
                label = { Text("API key") },
                supportingText = {
                    Text(
                        if (formError == ApiProfileFormError.API_KEY) "An API key is required."
                        else "Required — there is no default."
                    )
                },
                isError = formError == ApiProfileFormError.API_KEY,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_api_key_field")
            )

            OutlinedTextField(
                value = profile.model,
                onValueChange = { value -> viewModel.updateDraft { it.copy(model = value) } },
                label = { Text("Model") },
                placeholder = { Text(profile.apiSpec.defaultModel) },
                supportingText = { Text("Leave empty to use the default.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enabled", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Disabled providers can't be selected as active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = profile.enabled,
                    onCheckedChange = { value ->
                        viewModel.updateDraft { it.copy(enabled = value) }
                    },
                    modifier = Modifier.testTag("profile_enabled_switch")
                )
            }

            OutlinedButton(
                onClick = { viewModel.testConnection() },
                enabled = connectionTest !is ConnectionTestState.Running,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_connection_button")
            ) {
                if (connectionTest is ConnectionTestState.Running) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text("Test connection")
                }
            }

            (connectionTest as? ConnectionTestState.Done)?.let { done ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("connection_test_result")
                ) {
                    ProbeResultRow("Non-streaming", done.result.nonStreaming)
                    ProbeResultRow("Streaming", done.result.streaming)
                }
            }

            Button(
                onClick = { viewModel.save(onSaved = { navController.safePopBackStack() }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_settings_button")
            ) {
                Text("Save")
            }
        }
    }
}

/**
 * One line of connection-test feedback. A failure keeps the provider's raw
 * response below the headline — that body is what distinguishes a rejected key
 * from an unknown model name — but capped, so a provider that answers with an
 * HTML error page can't push the Save button off screen.
 */
@Composable
private fun ProbeResultRow(label: String, outcome: ProbeOutcome) {
    val color =
        if (outcome is ProbeOutcome.Success) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.error

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (outcome is ProbeOutcome.Success) Icons.Filled.Check
            else Icons.Filled.Close,
            contentDescription = if (outcome is ProbeOutcome.Success) "Passed" else "Failed",
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = when (outcome) {
                    is ProbeOutcome.Success -> "$label · ${outcome.detail}"
                    is ProbeOutcome.Failure -> "$label · ${outcome.summary}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
            (outcome as? ProbeOutcome.Failure)?.detail?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
