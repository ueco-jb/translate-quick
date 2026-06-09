package com.onebuttontranslate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onebuttontranslate.data.Settings
import com.onebuttontranslate.translate.TranslationException
import com.onebuttontranslate.translate.TranslatorFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TranslateUiState(
    val input: String = "",
    val result: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

/**
 * Holds the translate screen state and runs translation requests. Survives rotation;
 * Settings are passed in per-call so the screen always uses the current preferences.
 */
class TranslateViewModel : ViewModel() {

    private val _state = MutableStateFlow(TranslateUiState())
    val state: StateFlow<TranslateUiState> = _state.asStateFlow()

    private var inFlight: Job? = null

    fun onInputChange(value: String) {
        _state.update { it.copy(input = value, error = null) }
    }

    fun clearResult() {
        _state.update { it.copy(result = "", error = null) }
    }

    fun translate(settings: Settings) {
        val text = _state.value.input.trim()
        if (text.isEmpty()) return

        inFlight?.cancel()
        inFlight = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, result = "") }
            try {
                val translator = TranslatorFactory.forSettings(settings)
                val out = translator.translate(text, settings.sourceLang, settings.targetLang)
                _state.update { it.copy(loading = false, result = out) }
            } catch (e: TranslationException) {
                _state.update { it.copy(loading = false, error = e.userMessage) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Network error: ${e.message ?: e::class.simpleName}") }
            }
        }
    }
}
