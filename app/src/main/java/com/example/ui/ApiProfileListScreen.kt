package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.R
import com.example.data.settings.ApiProfile
import com.example.data.settings.displayName
import com.example.viewmodel.ApiProfileViewModel

/**
 * Lists every saved API profile and lets the user pick which one is active.
 * Editing, deleting and enabling live in each row's overflow menu; the "+" in
 * the app bar opens the same form in new-profile mode.
 */
@Composable
fun ApiProfileListScreen(
    viewModel: ApiProfileViewModel,
    navController: NavController
) {
    val profiles by viewModel.profiles.collectAsState()
    // Resolved by the repository, so a stored id pointing at a disabled or
    // deleted profile shows the same fallback row the chat top bar shows.
    val activeProfile by viewModel.activeProfile.collectAsState()
    var pendingDeletion by remember { mutableStateOf<ApiProfile?>(null) }

    ApiProfileListContent(
        profiles = profiles,
        activeProfileId = activeProfile?.id,
        onBack = { navController.safePopBackStack() },
        onAdd = { navController.navigate(Route.apiProfileEdit()) },
        onSelect = { profile -> viewModel.setActive(profile.id) },
        onEdit = { profile -> navController.navigate(Route.apiProfileEdit(profile.id)) },
        onToggleEnabled = { profile -> viewModel.setEnabled(profile.id, !profile.enabled) },
        // Arms the dialog only; the delete itself happens in its confirm button.
        onDelete = { profile -> pendingDeletion = profile }
    )

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

/**
 * The list without its ViewModel: the warm top bar over the saved profiles, or
 * over the empty state.
 *
 * Visible to the module (not private) for the same reason [SettingsContent] is —
 * the screenshot specimens render it directly, and it takes plain values and
 * lambdas. The delete confirmation stays outside, with the state it arms.
 *
 * This screen has no design in the handoff, so only the shell follows it: the
 * warm top bar and its 44dp icon buttons. The rows are stock Material.
 */
@Composable
internal fun ApiProfileListContent(
    profiles: List<ApiProfile>,
    activeProfileId: String?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onSelect: (ApiProfile) -> Unit,
    onEdit: (ApiProfile) -> Unit,
    onToggleEnabled: (ApiProfile) -> Unit,
    onDelete: (ApiProfile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WarmTopBar(
            title = "API configuration",
            navigation = {
                IconButton44(
                    icon = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Back",
                    onClick = onBack
                )
            },
            actions = {
                IconButton44(
                    icon = painterResource(R.drawable.ic_plus),
                    contentDescription = "Add provider",
                    onClick = onAdd,
                    modifier = Modifier.testTag("add_api_profile_button"),
                    iconSize = 22.dp
                )
            }
        )

        if (profiles.isEmpty()) {
            EmptyProfileList(
                onAdd = onAdd,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            items(profiles, key = { it.id }) { profile ->
                ApiProfileRow(
                    profile = profile,
                    isActive = profile.id == activeProfileId,
                    onSelect = { onSelect(profile) },
                    onEdit = { onEdit(profile) },
                    onToggleEnabled = { onToggleEnabled(profile) },
                    onDelete = { onDelete(profile) }
                )
            }
        }
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
                Icon(
                    painter = painterResource(R.drawable.ic_more_vertical),
                    contentDescription = "More options"
                )
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
