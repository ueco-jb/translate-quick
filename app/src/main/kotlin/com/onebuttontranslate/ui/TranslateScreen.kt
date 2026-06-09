package com.onebuttontranslate.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onebuttontranslate.data.Settings
import com.onebuttontranslate.data.Language

/**
 * Popup-style translation UI. Layout, top to bottom:
 *   [ sourceLang  <->  targetLang                       gear ]
 *   [ multi-line input (auto-focused; Enter/Send -> translate) ]
 *   [ progress bar OR error OR result text                    ]
 *
 * Enter on a hardware keyboard or the IME Send action submit the request;
 * Shift+Enter still inserts a newline for the rare multi-line case.
 */
@Composable
fun TranslateScreen(
    settings: Settings,
    onOpenSettings: () -> Unit,
    onSwapLanguages: () -> Unit,
    onSourceLangChange: (String) -> Unit,
    onTargetLangChange: (String) -> Unit,
    vm: TranslateViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Pop the soft keyboard the moment the popup appears so the user can type
    // immediately without an extra tap.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    val context = LocalContext.current
    val sourceLocale = Language.byCode(settings.sourceLang)?.locale ?: "en-US"

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotEmpty()) {
                vm.onInputChange(spoken)
                // After the recognizer activity returns, refocus the field so
                // the user can immediately glance, tweak if needed, and Enter.
                focusRequester.requestFocus()
                keyboard?.show()
            }
        }
    }

    fun launchVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLocale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, sourceLocale)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak in ${settings.sourceLang}")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No speech recognizer found on this device.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

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
            LanguageDropdownCompact(
                selectedCode = settings.sourceLang,
                onSelect = onSourceLangChange,
            )
            Spacer(Modifier.size(8.dp))
            FilledTonalIconButton(
                onClick = onSwapLanguages,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Filled.SwapHoriz,
                    contentDescription = "Swap languages",
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.size(8.dp))
            LanguageDropdownCompact(
                selectedCode = settings.targetLang,
                onSelect = onTargetLangChange,
            )
            Spacer(Modifier.weight(1f))
            FilledTonalIconButton(
                onClick = { launchVoiceInput() },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = "Voice input",
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.size(4.dp))
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

        OutlinedTextField(
            value = state.input,
            onValueChange = vm::onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                // Belt-and-suspenders: also intercept hardware-keyboard Enter
                // directly, in case the IME action below isn't routed through
                // (some hardware keyboards / IMEs skip it).
                .onPreviewKeyEvent { ev ->
                    if (ev.type == KeyEventType.KeyDown && ev.key == Key.Enter) {
                        vm.translate(settings)
                        true
                    } else {
                        false
                    }
                },
            placeholder = { Text("Type and press Enter") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send,
                capitalization = KeyboardCapitalization.Sentences,
            ),
            keyboardActions = KeyboardActions(
                onSend = { vm.translate(settings) },
            ),
        )

        when {
            state.loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.error != null -> Text(
                state.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
            )
            state.result.isNotEmpty() -> SelectionContainer {
                Text(
                    state.result,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Compact tappable language indicator: shows just the 2-letter code (EN, ES,
 * ...) with a small chevron; tapping opens a short list of all available
 * languages. Designed to be as fast to use as static text -- one tap to open,
 * one tap to pick.
 */
@Composable
private fun LanguageDropdownCompact(
    selectedCode: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                selectedCode.uppercase(),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Language.ALL.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Text(
                            lang.code,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    onClick = {
                        onSelect(lang.code)
                        expanded = false
                    },
                )
            }
        }
    }
}
