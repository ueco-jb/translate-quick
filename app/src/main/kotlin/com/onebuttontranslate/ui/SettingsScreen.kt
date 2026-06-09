package com.onebuttontranslate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.onebuttontranslate.data.Provider
import com.onebuttontranslate.data.Settings
import com.onebuttontranslate.data.Language

/**
 * Popup-friendly settings panel. No Scaffold/TopAppBar (those fill the parent
 * which would force the floating window to full screen height); instead, a
 * small header row + a verticalScroll Column constrained to fillMaxWidth.
 */
@Composable
fun SettingsScreen(
    initial: Settings,
    canGoBack: Boolean,
    onSave: (Settings) -> Unit,
    onBack: () -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canGoBack) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.size(4.dp))
            }
            Text(
                "Settings",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Text("Translation provider", style = MaterialTheme.typography.labelMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            Provider.entries.forEachIndexed { index, provider ->
                SegmentedButton(
                    selected = draft.provider == provider,
                    onClick = { draft = draft.copy(provider = provider) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = Provider.entries.size,
                    ),
                ) { Text(provider.displayName) }
            }
        }

        when (draft.provider) {
            Provider.DEEPL -> {
                OutlinedTextField(
                    value = draft.deeplApiKey,
                    onValueChange = { draft = draft.copy(deeplApiKey = it) },
                    label = { Text("DeepL API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Free-plan keys end in \":fx\". Get one at deepl.com/pro-api.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Provider.OPENAI -> {
                OutlinedTextField(
                    value = draft.openaiApiKey,
                    onValueChange = { draft = draft.copy(openaiApiKey = it) },
                    label = { Text("OpenAI API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.openaiModel,
                    onValueChange = { draft = draft.copy(openaiModel = it) },
                    label = { Text("OpenAI model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Text("Languages", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LanguageDropdown(
                label = "From",
                selectedCode = draft.sourceLang,
                onSelect = { draft = draft.copy(sourceLang = it) },
                modifier = Modifier.weight(1f),
            )
            LanguageDropdown(
                label = "To",
                selectedCode = draft.targetLang,
                onSelect = { draft = draft.copy(targetLang = it) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = { onSave(draft) },
            enabled = draft.isReady,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }
    }
}

/**
 * Read-only dropdown showing a language by its display name. The underlying
 * value emitted via [onSelect] is the 2-letter code (e.g. "EN") so the rest
 * of the app keeps working unchanged. If the persisted code is unknown the
 * code itself is shown verbatim -- the user can pick a known one to fix it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    label: String,
    selectedCode: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = Language.byCode(selectedCode)?.displayName ?: selectedCode

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Language.ALL.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.displayName) },
                    onClick = {
                        onSelect(lang.code)
                        expanded = false
                    },
                )
            }
        }
    }
}
