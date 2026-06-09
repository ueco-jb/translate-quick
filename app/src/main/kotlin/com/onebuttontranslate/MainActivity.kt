package com.onebuttontranslate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.onebuttontranslate.data.Settings
import com.onebuttontranslate.data.SettingsRepository
import com.onebuttontranslate.ui.OneButtonTranslateTheme
import com.onebuttontranslate.ui.SettingsScreen
import com.onebuttontranslate.ui.TranslateScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = SettingsRepository(applicationContext)
        setContent {
            OneButtonTranslateTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
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
        // First emission from DataStore hasn't arrived yet; nothing useful to draw.
        Box(modifier = Modifier.fillMaxSize()) {}
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
