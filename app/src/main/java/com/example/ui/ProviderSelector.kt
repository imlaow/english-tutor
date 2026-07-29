package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.settings.ApiProfile
import com.example.data.settings.displayName

/**
 * App-bar title that doubles as the active-provider picker: shows which
 * provider requests currently go to and opens a menu of the enabled ones.
 *
 * [activeProfile] should come from the repository's resolved active profile
 * rather than a raw stored id, so a disabled or deleted provider never shows
 * up here as active.
 */
@Composable
fun ProviderSelector(
    title: String,
    activeProfile: ApiProfile?,
    enabledProfiles: List<ApiProfile>,
    onSelect: (String) -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("provider_selector"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = activeProfile?.name ?: "No provider configured",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose provider")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            enabledProfiles.forEach { profile ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(profile.name)
                            Text(
                                text = "${profile.apiSpec.displayName} · ${profile.effectiveModel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    // Reserve the leading slot on every row so names stay aligned
                    // whether or not the row is the active one.
                    leadingIcon = {
                        if (profile.id == activeProfile?.id) {
                            Icon(Icons.Default.Check, contentDescription = "Active")
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(profile.id)
                    },
                    modifier = Modifier.testTag("provider_option_${profile.id}")
                )
            }

            if (enabledProfiles.isNotEmpty()) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = {
                    Text(
                        if (enabledProfiles.isEmpty()) "Add a provider…" else "Manage providers…"
                    )
                },
                onClick = {
                    expanded = false
                    onManage()
                },
                modifier = Modifier.testTag("provider_manage_option")
            )
        }
    }
}
