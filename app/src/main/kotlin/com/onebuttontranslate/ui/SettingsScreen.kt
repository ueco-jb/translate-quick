package com.onebuttontranslate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.onebuttontranslate.data.Provider
import com.onebuttontranslate.data.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initial: Settings,
    canGoBack: Boolean,
    onSave: (Settings) -> Unit,
    onBack: () -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            Text("Translation provider")
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
                        "Tip: Free-plan keys end in \":fx\". Get one at deepl.com/pro-api.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
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

            Spacer(Modifier.height(4.dp))
            Text("Languages")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft.sourceLang,
                    onValueChange = { draft = draft.copy(sourceLang = it.uppercase()) },
                    label = { Text("From") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = draft.targetLang,
                    onValueChange = { draft = draft.copy(targetLang = it.uppercase()) },
                    label = { Text("To") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                "Use language codes (EN, ES, FR, DE...). DeepL accepts region variants like EN-GB, PT-BR.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { onSave(draft) },
                enabled = draft.isReady,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
        }
    }
}
