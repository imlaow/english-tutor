package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavController
import com.example.data.repository.ApiProfileRepository

/**
 * Main settings hub: lists setting categories, each navigating to its own
 * sub-screen. New categories (appearance, tutor preferences, ...) should be
 * added as further [SettingsEntry] rows below the existing ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val apiProfileRepository = remember { ApiProfileRepository.getInstance(context) }
    val activeProfile by apiProfileRepository.activeProfile.collectAsState()
    val topicProfileId by apiProfileRepository.topicProfileId.collectAsState()
    val topicProfile by apiProfileRepository.topicProfile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsEntry(
                title = "API configuration",
                subtitle = activeProfile?.name ?: "Not configured",
                icon = Icons.Filled.Cloud,
                onClick = { navController.navigate(Route.API_PROFILES) { launchSingleTop = true } },
                modifier = Modifier.testTag("settings_api_configuration")
            )
            SettingsEntry(
                title = "Topic generation",
                subtitle = if (topicProfileId == null) {
                    "Chat provider (default)"
                } else {
                    topicProfile?.name ?: "Chat provider (default)"
                },
                icon = Icons.Filled.Lightbulb,
                onClick = { navController.navigate(Route.TOPIC_PROVIDER) { launchSingleTop = true } },
                modifier = Modifier.testTag("settings_topic_generation")
            )
        }
    }
}

@Composable
private fun SettingsEntry(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}
