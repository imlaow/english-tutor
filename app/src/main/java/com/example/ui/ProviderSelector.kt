package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.settings.ApiProfile
import com.example.data.settings.displayName
import com.example.ui.theme.Accent400
import com.example.ui.theme.Accent700
import com.example.ui.theme.Neutral700

/**
 * The provider tag that sits under the chat screen's title: shows which provider
 * requests currently go to, and opens a menu of the enabled ones.
 *
 * [activeProfile] should come from the repository's resolved active profile rather
 * than a raw stored id, so a disabled or deleted provider never shows up here as
 * active.
 *
 * The title used to live here too, back when this filled the app bar's title slot.
 * [WarmTopBar] owns the title now, so this is only the tag and its menu.
 */
@Composable
fun ProviderPill(
    activeProfile: ApiProfile?,
    enabledProfiles: List<ApiProfile>,
    onSelect: (String) -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(modifier = modifier) {
        // The tag paints its own background, so a ripple would be covered by it;
        // the design's press state is a color swap anyway.
        Pill(
            text = activeProfile?.name ?: "No provider configured",
            backgroundColor = MaterialTheme.colorScheme.background,
            contentColor = if (pressed) Accent700 else Neutral700,
            border = BorderStroke(
                width = 1.dp,
                color = if (pressed) Accent400 else MaterialTheme.colorScheme.outlineVariant
            ),
            trailingIcon = painterResource(R.drawable.ic_chevron_down),
            contentPadding = PaddingValues(start = 11.dp, top = 4.dp, end = 9.dp, bottom = 4.dp),
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = { expanded = true }
                )
                .testTag("provider_selector")
        )

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
                        Box(modifier = Modifier.size(10.dp)) {
                            if (profile.id == activeProfile?.id) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .semantics { contentDescription = "Active" }
                                )
                            }
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
