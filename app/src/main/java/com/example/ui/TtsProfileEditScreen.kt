package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.R
import com.example.data.settings.TtsProfile
import com.example.viewmodel.TtsProfileFormError
import com.example.viewmodel.TtsProfileViewModel

/**
 * New/edit form for a single Azure Speech profile. [profileId] null means "new".
 *
 * There is no engine picker: Azure is the only engine the app speaks, so the
 * form is the three things a subscription needs — key, region, voice.
 */
@Composable
fun TtsProfileEditScreen(
    viewModel: TtsProfileViewModel,
    navController: NavController,
    profileId: String?
) {
    LaunchedEffect(profileId) { viewModel.loadForEdit(profileId) }

    val draft by viewModel.draft.collectAsState()
    val formError by viewModel.formError.collectAsState()

    TtsProfileEditContent(
        profile = draft,
        isNewProfile = profileId == null,
        formError = formError,
        onBack = { navController.safePopBackStack() },
        onNameChange = { value -> viewModel.updateDraft { it.copy(name = value) } },
        onSpeechKeyChange = { value -> viewModel.updateDraft { it.copy(speechKey = value) } },
        onRegionChange = { value -> viewModel.updateDraft { it.copy(region = value) } },
        onVoiceChange = { value -> viewModel.updateDraft { it.copy(voice = value) } },
        onEnabledChange = { value -> viewModel.updateDraft { it.copy(enabled = value) } },
        onSave = { viewModel.save(onSaved = { navController.safePopBackStack() }) }
    )
}

/**
 * The form without its ViewModel: the warm top bar over the profile's fields.
 *
 * Visible to the module (not private) for the same reason [SettingsContent] is —
 * the screenshot specimens render it directly, and it takes plain values and
 * lambdas.
 *
 * The handoff never drew this screen, but it does specify the controls: the
 * shell is its top bar and 44dp button and the fields are its `.input` by way of
 * [WarmTextField]. The switch stays stock Material — the design system has no
 * toggle. There is no "Test voice" button: a probe would have to speak a
 * sentence, and a sentence written into production code is exactly the mock
 * content CLAUDE.md forbids.
 *
 * @param profile null only while an existing profile is being read back from
 *   Room; the bar stays up and the fields are simply absent until it arrives,
 *   rather than flashing blank values over the stored ones.
 * @param isNewProfile only decides the title; the form itself is the same either
 *   way, since a new profile is an unsaved [TtsProfile].
 */
@Composable
internal fun TtsProfileEditContent(
    profile: TtsProfile?,
    isNewProfile: Boolean,
    formError: TtsProfileFormError?,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onSpeechKeyChange: (String) -> Unit,
    onRegionChange: (String) -> Unit,
    onVoiceChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        WarmTopBar(
            title = if (isNewProfile) "New voice" else "Edit voice",
            navigation = {
                IconButton44(
                    icon = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = "Back",
                    onClick = onBack
                )
            }
        )

        if (profile == null) return@Column

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WarmTextField(
                value = profile.name,
                onValueChange = onNameChange,
                label = "Name",
                supportingText =
                    if (formError == TtsProfileFormError.NAME) "Give the voice a name."
                    else "Shown in the voice list.",
                isError = formError == TtsProfileFormError.NAME,
                singleLine = true,
                modifier = Modifier.testTag("tts_name_field")
            )

            WarmTextField(
                value = profile.speechKey,
                onValueChange = onSpeechKeyChange,
                label = "Speech key",
                supportingText =
                    if (formError == TtsProfileFormError.SPEECH_KEY) "A speech key is required."
                    else "Required — there is no default.",
                isError = formError == TtsProfileFormError.SPEECH_KEY,
                singleLine = true,
                modifier = Modifier.testTag("tts_speech_key_field")
            )

            WarmTextField(
                value = profile.region,
                onValueChange = onRegionChange,
                label = "Region",
                supportingText =
                    if (formError == TtsProfileFormError.REGION) "A region is required."
                    // A key is issued for one region and rejected everywhere else,
                    // so this cannot have a default either.
                    else "The region the key was issued for.",
                isError = formError == TtsProfileFormError.REGION,
                singleLine = true,
                modifier = Modifier.testTag("tts_region_field")
            )

            WarmTextField(
                value = profile.voice,
                onValueChange = onVoiceChange,
                label = "Voice",
                placeholder = TtsProfile.DEFAULT_VOICE,
                supportingText = "Leave empty to use the default.",
                singleLine = true,
                modifier = Modifier.testTag("tts_voice_field")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enabled", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Disabled voices can't be selected as active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = profile.enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.testTag("tts_enabled_switch")
                )
            }

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_tts_profile_button")
            ) {
                Text("Save")
            }
        }
    }
}
