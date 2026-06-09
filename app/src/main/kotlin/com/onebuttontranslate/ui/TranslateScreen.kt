package com.onebuttontranslate.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            Text(
                settings.sourceLang.uppercase(),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = onSwapLanguages, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap languages")
            }
            Text(
                settings.targetLang.uppercase(),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
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
