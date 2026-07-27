package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Test-only design-system specimens. These render the theme itself rather than
 * any screen, so they stay stable while screens are refactored and they fail
 * loudly the moment a token, type style, or shape changes.
 *
 * Split into three composables because a Pixel 8 viewport clips anything much
 * taller than ~900dp, and [captureRoboImage] captures only what is laid out.
 */

/** One filled block per container pair, with its "on" color as the label. */
@Composable
internal fun ColorSwatches() {
    val scheme = MaterialTheme.colorScheme
    val pairs =
        listOf(
            Triple("primary", scheme.primary, scheme.onPrimary),
            Triple("primaryContainer", scheme.primaryContainer, scheme.onPrimaryContainer),
            Triple("secondary", scheme.secondary, scheme.onSecondary),
            Triple("secondaryContainer", scheme.secondaryContainer, scheme.onSecondaryContainer),
            Triple("tertiary", scheme.tertiary, scheme.onTertiary),
            Triple("tertiaryContainer", scheme.tertiaryContainer, scheme.onTertiaryContainer),
            Triple("error", scheme.error, scheme.onError),
            Triple("errorContainer", scheme.errorContainer, scheme.onErrorContainer),
            Triple("background", scheme.background, scheme.onBackground),
            Triple("surface", scheme.surface, scheme.onSurface),
            Triple("surfaceVariant", scheme.surfaceVariant, scheme.onSurfaceVariant),
            Triple("surfaceContainerLowest", scheme.surfaceContainerLowest, scheme.onSurface),
            Triple("surfaceContainerLow", scheme.surfaceContainerLow, scheme.onSurface),
            Triple("surfaceContainer", scheme.surfaceContainer, scheme.onSurface),
            Triple("surfaceContainerHigh", scheme.surfaceContainerHigh, scheme.onSurface),
            Triple("surfaceContainerHighest", scheme.surfaceContainerHighest, scheme.onSurface),
            Triple("inverseSurface", scheme.inverseSurface, scheme.inverseOnSurface),
        )

    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        pairs.forEach { (name, container, onContainer) ->
            Row(
                modifier = Modifier.fillMaxWidth().background(container).padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = name, color = onContainer, style = MaterialTheme.typography.labelMedium)
            }
        }
        // outline / outlineVariant have no "on" pair, so show them as rules.
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("outline", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(110.dp))
            HorizontalDivider(color = scheme.outline, thickness = 2.dp)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("outlineVariant", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(110.dp))
            HorizontalDivider(color = scheme.outlineVariant, thickness = 2.dp)
        }
    }
}

/** The styles the app actually uses, each labelled with its own name. */
@Composable
internal fun TypeSpecimens() {
    val type = MaterialTheme.typography
    val styles =
        listOf<Pair<String, TextStyle>>(
            "displaySmall" to type.displaySmall,
            "headlineMedium" to type.headlineMedium,
            "headlineSmall" to type.headlineSmall,
            "titleLarge" to type.titleLarge,
            "titleMedium" to type.titleMedium,
            "titleSmall" to type.titleSmall,
            "bodyLarge" to type.bodyLarge,
            "bodyMedium" to type.bodyMedium,
            "bodySmall" to type.bodySmall,
            "labelLarge" to type.labelLarge,
            "labelMedium" to type.labelMedium,
            "labelSmall" to type.labelSmall,
        )

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        styles.forEach { (name, style) ->
            Text(text = name, style = style, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

/** The Material components the app builds its screens from. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComponentGallery() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {}) { Text("Save") }
            FilledTonalButton(onClick = {}) { Text("Test") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = {}) { Text("Connect") }
            TextButton(onClick = {}) { Text("Refresh") }
            FloatingActionButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Mic, contentDescription = null)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = true, onClick = {}, label = { Text("Intermediate") })
            FilterChip(selected = false, onClick = {}, label = { Text("Advanced") })
        }

        OutlinedTextField(
            value = "gpt-4o-mini",
            onValueChange = {},
            label = { Text("Model") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "What did you do last weekend?",
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        ListItem(
            headlineContent = { Text("API configuration") },
            supportingContent = { Text("OpenAI") },
            leadingContent = { RadioButton(selected = true, onClick = {}) },
            trailingContent = { Switch(checked = true, onCheckedChange = {}) },
            modifier = Modifier.fillMaxWidth(),
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Topic generation") },
            supportingContent = { Text("Chat provider (default)") },
            leadingContent = { RadioButton(selected = false, onClick = {}) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
