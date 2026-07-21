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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Lists every saved API profile and lets the user pick which one is active.
 * Editing, deleting and enabling live in each row's overflow menu; the "+" in
 * the app bar opens the same form in new-profile mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiProfileListScreen(
    viewModel: ApiProfileViewModel,
    navController: NavController
) {
    val profiles by viewModel.profiles.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()
    var pendingDeletion by remember { mutableStateOf<ApiProfile?>(null) }

    // The stored id may point at a disabled or deleted profile; the row that is
    // actually used is the one the repository would fall back to.
    val effectiveActiveId = profiles.firstOrNull { it.id == activeProfileId && it.enabled }?.id
        ?: profiles.firstOrNull { it.enabled }?.id

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API configuration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.safePopBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate(Route.apiProfileEdit()) },
                        modifier = Modifier.testTag("add_api_profile_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add provider")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (profiles.isEmpty()) {
            EmptyProfileList(
                onAdd = { navController.navigate(Route.apiProfileEdit()) },
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
            items(profiles, key = { it.id }) { profile ->
                ApiProfileRow(
                    profile = profile,
                    isActive = profile.id == effectiveActiveId,
                    onSelect = { viewModel.setActive(profile.id) },
                    onEdit = { navController.navigate(Route.apiProfileEdit(profile.id)) },
                    onToggleEnabled = { viewModel.setEnabled(profile.id, !profile.enabled) },
                    onDelete = { pendingDeletion = profile }
                )
            }
        }
    }

    pendingDeletion?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingDeletion = null },
            title = { Text("Delete provider?") },
            text = { Text("\"${profile.name}\" and its API key will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(profile.id)
                    pendingDeletion = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletion = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ApiProfileRow(
    profile: ApiProfile,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    // Disabled profiles can't be used for requests, so they can't be activated
    // either; dimming the whole row says so without an extra label.
    val contentColor =
        if (profile.enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant

    ListItem(
        headlineContent = { Text(profile.name) },
        supportingContent = {
            Text("${profile.apiSpec.displayName} · ${profile.effectiveModel}")
        },
        leadingContent = {
            RadioButton(
                selected = isActive,
                onClick = onSelect.takeIf { profile.enabled },
                enabled = profile.enabled
            )
        },
        trailingContent = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (profile.enabled) "Disable" else "Enable") },
                        onClick = {
                            menuExpanded = false
                            onToggleEnabled()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            headlineColor = contentColor,
            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("api_profile_${profile.id}")
            .clickable(enabled = profile.enabled, onClick = onSelect)
    )
}

@Composable
private fun EmptyProfileList(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No API providers yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Add a provider with your API key to start chatting with the AI tutor.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onAdd, modifier = Modifier.testTag("add_first_api_profile_button")) {
            Text("Add provider")
        }
    }
}
