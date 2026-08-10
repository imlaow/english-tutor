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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
 * Lets the user pick which configured provider generates the home-screen topic
 * suggestions, independent of the active chat provider. Selecting nothing (the
 * default) reuses whatever the chat is using.
 */
@Composable
fun TopicProviderScreen(
    viewModel: ApiProfileViewModel,
    navController: NavController
) {
    val enabledProfiles by viewModel.enabledProfiles.collectAsState()
    val topicProfileId by viewModel.topicProfileId.collectAsState()
    // What "default" resolves to right now, so the row can name it.
    val topicProfile by viewModel.topicProfile.collectAsState()

    TopicProviderContent(
        profiles = enabledProfiles,
        selectedProfileId = topicProfileId,
        defaultSubtitle = topicProfile?.let { "Currently: ${it.name}" }
            ?: "No provider configured",
        onBack = { navController.safePopBackStack() },
        onSelect = { profileId -> viewModel.setTopicProfile(profileId) }
    )
}

/**
 * The picker without its ViewModel: the warm top bar over the "use the chat
 * provider" row and one row per enabled profile.
 *
 * Visible to the module (not private) for the same reason [SettingsContent] is —
 * the screenshot specimens render it directly, and it takes plain values and
 * lambdas.
 *
 * This screen has no design in the handoff, so only the shell follows it: the
 * warm top bar and its 44dp icon button. The rows are stock Material.
 *
 * @param selectedProfileId null when topics follow the chat provider.
 * @param defaultSubtitle what that fallback currently resolves to, resolved by
 *   the repository rather than restated here.
 * @param onSelect called with null to clear the override.
 */
@Composable
internal fun TopicProviderContent(
    profiles: List<ApiProfile>,
    selectedProfileId: String?,
    defaultSubtitle: String,
    onBack: () -> Unit,
    onSelect: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WarmTopBar(
            title = "Topic generation",
            navigation = {
                IconButton44(
                    icon = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Back",
                    onClick = onBack
                )
            }
        )

        if (profiles.isEmpty()) {
            EmptyTopicProviders(
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
            item(key = "default") {
                TopicProviderRow(
                    title = "Use chat provider (default)",
                    subtitle = defaultSubtitle,
                    selected = selectedProfileId == null,
                    onSelect = { onSelect(null) },
                    testTag = "topic_provider_default"
                )
                HorizontalDivider()
            }
            items(profiles, key = { it.id }) { profile ->
                TopicProviderRow(
                    title = profile.name,
                    subtitle = "${profile.apiSpec.displayName} · ${profile.effectiveModel}",
                    selected = selectedProfileId == profile.id,
                    onSelect = { onSelect(profile.id) },
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
