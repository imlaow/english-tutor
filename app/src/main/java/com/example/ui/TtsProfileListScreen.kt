package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import com.example.data.settings.TtsProfile
import com.example.viewmodel.TtsProfileViewModel

/**
 * Indent of the hairline between two rows, so it starts under the voice name
 * rather than under the radio's 48dp tap target.
 */
private val RowDividerIndent = 80.dp

/**
 * Lists every saved Azure Speech profile and lets the user pick which one the
 * tutor speaks with. Editing, deleting and enabling live in each row's overflow
 * menu; the "+" in the app bar opens the same form in new-profile mode.
 *
 * Structurally the twin of [ApiProfileListScreen] — same rows, same radio, same
 * overflow — because it is the same job for the other half of the credentials.
 */
@Composable
fun TtsProfileListScreen(
    viewModel: TtsProfileViewModel,
    navController: NavController
) {
    val profiles by viewModel.profiles.collectAsState()
    // Resolved by the repository, so a stored id pointing at a disabled or
    // deleted profile shows the same fallback the manager would speak with.
    val activeProfile by viewModel.activeProfile.collectAsState()
    var pendingDeletion by remember { mutableStateOf<TtsProfile?>(null) }

    TtsProfileListContent(
        profiles = profiles,
        activeProfileId = activeProfile?.id,
        onBack = { navController.safePopBackStack() },
        onAdd = { navController.navigate(Route.ttsProfileEdit()) },
        onSelect = { profile -> viewModel.setActive(profile.id) },
        onEdit = { profile -> navController.navigate(Route.ttsProfileEdit(profile.id)) },
        onToggleEnabled = { profile -> viewModel.setEnabled(profile.id, !profile.enabled) },
        // Arms the dialog only; the delete itself happens in its confirm button.
        onDelete = { profile -> pendingDeletion = profile }
    )

    pendingDeletion?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingDeletion = null },
            title = { Text("Delete voice?") },
            text = { Text("\"${profile.name}\" and its speech key will be removed.") },
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
 * The handoff never drew this screen, so it borrows the settings hub's furniture:
 * the design's top bar and 44dp buttons, its `.radio` dot by way of [WarmRadio],
 * and the same grouped card of [SettingsRow]s one level up.
 */
@Composable
internal fun TtsProfileListContent(
    profiles: List<TtsProfile>,
    activeProfileId: String?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onSelect: (TtsProfile) -> Unit,
    onEdit: (TtsProfile) -> Unit,
    onToggleEnabled: (TtsProfile) -> Unit,
    onDelete: (TtsProfile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WarmTopBar(
            title = "Text to speech",
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
                    contentDescription = "Add voice",
                    onClick = onAdd,
                    modifier = Modifier.testTag("add_tts_profile_button"),
                    iconSize = 22.dp
                )
            }
        )

        if (profiles.isEmpty()) {
            EmptyTtsProfileList(
                onAdd = onAdd,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
            return@Column
        }

        SettingsCardColumn(modifier = Modifier.weight(1f)) {
            SectionLabel("Voices")
            Spacer(modifier = Modifier.height(10.dp))

            SettingsCard {
                profiles.forEachIndexed { index, profile ->
                    if (index > 0) SettingsCardDivider(RowDividerIndent)
                    TtsProfileRow(
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
}

@Composable
private fun TtsProfileRow(
    profile: TtsProfile,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    // Disabled profiles can't be spoken with, so they can't be activated either;
    // dimming the whole row says so without an extra label.
    val contentColor =
        if (profile.enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant

    SettingsRow(
        title = profile.name,
        subtitle = "${profile.region} · ${profile.effectiveVoice}",
        onClick = onSelect,
        enabled = profile.enabled,
        titleColor = contentColor,
        leading = {
            WarmRadio(
                selected = isActive,
                onClick = onSelect.takeIf { profile.enabled },
                enabled = profile.enabled
            )
        },
        trailing = {
            // The menu anchors to this box rather than to the button, which has no
            // slot of its own.
            Box {
                IconButton44(
                    icon = painterResource(R.drawable.ic_more_vertical),
                    contentDescription = "More options",
                    onClick = { menuExpanded = true },
                    size = 36.dp,
                    iconSize = 20.dp
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
        modifier = Modifier.testTag("tts_profile_${profile.id}")
    )
}

@Composable
private fun EmptyTtsProfileList(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No voices yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Add an Azure Speech key to hear the tutor read its replies out loud.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onAdd, modifier = Modifier.testTag("add_first_tts_profile_button")) {
            Text("Add voice")
        }
    }
}
