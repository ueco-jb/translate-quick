package com.onebuttontranslate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onebuttontranslate.data.SettingsRepository
import com.onebuttontranslate.ui.OneButtonTranslateTheme
import com.onebuttontranslate.ui.SettingsScreen
import com.onebuttontranslate.ui.TranslateScreen
import kotlinx.coroutines.launch

/**
 * Single Activity, themed as a floating dialog (see themes.xml). The visible
 * "card" is this Surface; the window itself is transparent so the activity
 * behind shows through under the dim. fillMaxWidth (not fillMaxSize) keeps
 * the popup wrap-content tall.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = SettingsRepository(applicationContext)
        setContent {
            OneButtonTranslateTheme {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 4.dp,
                ) {
                    App(repo)
                }
            }
        }
    }
}

@Composable
private fun App(repo: SettingsRepository) {
    val settings by repo.flow.collectAsState(initial = null)
    var showSettings by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val current = settings ?: run {
        // First DataStore emission hasn't arrived yet; render a tiny placeholder
        // so the popup doesn't visibly resize when settings load.
        Box(modifier = Modifier.size(1.dp)) {}
        return
    }

    // Auto-route to settings on first run (no key set yet).
    LaunchedEffect(current.isReady) {
        if (!current.isReady) showSettings = true
    }

    if (showSettings || !current.isReady) {
        BackHandler(enabled = current.isReady) { showSettings = false }
        SettingsScreen(
            initial = current,
            canGoBack = current.isReady,
            onSave = { updated ->
                scope.launch {
                    repo.update { updated }
                    if (updated.isReady) showSettings = false
                }
            },
            onBack = { showSettings = false },
        )
    } else {
        TranslateScreen(
            settings = current,
            onOpenSettings = { showSettings = true },
            onSwapLanguages = {
                scope.launch {
                    repo.update { s ->
                        s.copy(sourceLang = s.targetLang, targetLang = s.sourceLang)
                    }
                }
            },
        )
    }
}
