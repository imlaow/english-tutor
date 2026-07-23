package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.settings.ApiProfile
import com.example.data.settings.displayName
import com.example.viewmodel.ApiProfileViewModel

/**
 * Lets the user pick which configured provider generates the home-screen topic
 * suggestions, independent of the active chat provider. Selecting nothing (the
 * default) reuses whatever the chat is using.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicProviderScreen(
    viewModel: ApiProfileViewModel,
    navController: NavController
) {
    val enabledProfiles by viewModel.enabledProfiles.collectAsState()
    val topicProfileId by viewModel.topicProfileId.collectAsState()
    // What "default" resolves to right now, so the row can name it.
    val topicProfile by viewModel.topicProfile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Topic generation", fontWeight = FontWeight.Bold) },
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
        if (enabledProfiles.isEmpty()) {
            EmptyTopicProviders(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item(key = "default") {
                TopicProviderRow(
                    title = "Use chat provider (default)",
                    subtitle = topicProfile?.let { "Currently: ${it.name}" }
                        ?: "No provider configured",
                    selected = topicProfileId == null,
                    onSelect = { viewModel.setTopicProfile(null) },
                    testTag = "topic_provider_default"
                )
                HorizontalDivider()
            }
            items(enabledProfiles, key = { it.id }) { profile ->
                TopicProviderRow(
                    title = profile.name,
                    subtitle = "${profile.apiSpec.displayName} · ${profile.effectiveModel}",
                    selected = topicProfileId == profile.id,
                    onSelect = { viewModel.setTopicProfile(profile.id) },
                    testTag = "topic_provider_${profile.id}"
                )
            }
        }
    }
}

@Composable
private fun TopicProviderRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
    testTag: String
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            RadioButton(selected = selected, onClick = onSelect)
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onSelect)
    )
}

@Composable
private fun EmptyTopicProviders(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No providers to choose from",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Add a provider under API configuration first; topic suggestions will then use it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
