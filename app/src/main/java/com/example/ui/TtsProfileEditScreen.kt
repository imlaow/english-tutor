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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
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
 * form is the three things a subscription needs — name, key and region — plus
 * the voice and the four optional SSML expression knobs that shape how it
 * speaks. Every one of those five is safe to leave empty.
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
        onStyleChange = { value -> viewModel.updateDraft { it.copy(style = value) } },
        onStyleDegreeChange = { value -> viewModel.updateDraft { it.copy(styleDegree = value) } },
        onPitchChange = { value -> viewModel.updateDraft { it.copy(pitch = value) } },
        onRateChange = { value -> viewModel.updateDraft { it.copy(rate = value) } },
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
    onStyleChange: (String) -> Unit,
    onStyleDegreeChange: (String) -> Unit,
    onPitchChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
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

            WarmTextField(
                value = profile.style,
                onValueChange = onStyleChange,
                label = "Speaking style",
                // Azure's own vocabulary for a style, shown the way the supporting
                // text below shows pitch syntax — UI copy, not sample content.
                placeholder = "excited",
                supportingText = "Optional. Only styles the voice supports have any effect.",
                singleLine = true,
                modifier = Modifier.testTag("tts_style_field")
            )

            WarmTextField(
                value = profile.styleDegree,
                onValueChange = onStyleDegreeChange,
                label = "Style degree",
                placeholder = TtsProfile.DEFAULT_STYLE_DEGREE,
                supportingText =
                    if (formError == TtsProfileFormError.STYLE_DEGREE) "Use a number from 0.01 to 2."
                    else "0.01–2. Only applies with a style.",
                isError = formError == TtsProfileFormError.STYLE_DEGREE,
                singleLine = true,
                // Decimal, not Number: 1.6 needs a decimal point. Pitch and Speed
                // below keep the text keyboard, since +, %, st and x-high are all
                // valid there and a numeric one would block them.
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.testTag("tts_style_degree_field")
            )

            WarmTextField(
                value = profile.pitch,
                onValueChange = onPitchChange,
                label = "Pitch",
                placeholder = TtsProfile.NEUTRAL_RATE,
                supportingText = "Optional. Also accepts -2st or high. HD voices ignore it.",
                singleLine = true,
                modifier = Modifier.testTag("tts_pitch_field")
            )

            WarmTextField(
                value = profile.rate,
                onValueChange = onRateChange,
                label = "Speed",
                placeholder = TtsProfile.NEUTRAL_RATE,
                supportingText = "Optional. +10% speaks faster, -10% slower.",
                singleLine = true,
                modifier = Modifier.testTag("tts_rate_field")
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
