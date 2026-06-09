package com.onebuttontranslate.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** All user-configurable state. Immutable snapshot; written via [SettingsRepository.update]. */
data class Settings(
    val provider: Provider,
    val deeplApiKey: String,
    val openaiApiKey: String,
    val openaiModel: String,
    val sourceLang: String,
    val targetLang: String,
) {
    /** True when the currently selected provider has the credentials it needs to translate. */
    val isReady: Boolean
        get() = when (provider) {
            Provider.DEEPL -> deeplApiKey.isNotBlank()
            Provider.OPENAI -> openaiApiKey.isNotBlank() && openaiModel.isNotBlank()
        }

    companion object {
        val Default = Settings(
            provider = Provider.DEEPL,
            deeplApiKey = "",
            openaiApiKey = "",
            openaiModel = "gpt-4o-mini",
            sourceLang = "EN",
            targetLang = "ES",
        )
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private object Keys {
    val PROVIDER = stringPreferencesKey("provider")
    val DEEPL_API_KEY = stringPreferencesKey("deepl_api_key")
    val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
    val OPENAI_MODEL = stringPreferencesKey("openai_model")
    val SOURCE_LANG = stringPreferencesKey("source_lang")
    val TARGET_LANG = stringPreferencesKey("target_lang")
}

class SettingsRepository(private val context: Context) {

    val flow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            provider = prefs[Keys.PROVIDER]?.let(::providerFromString) ?: Settings.Default.provider,
            deeplApiKey = prefs[Keys.DEEPL_API_KEY] ?: Settings.Default.deeplApiKey,
            openaiApiKey = prefs[Keys.OPENAI_API_KEY] ?: Settings.Default.openaiApiKey,
            openaiModel = prefs[Keys.OPENAI_MODEL] ?: Settings.Default.openaiModel,
            sourceLang = prefs[Keys.SOURCE_LANG] ?: Settings.Default.sourceLang,
            targetLang = prefs[Keys.TARGET_LANG] ?: Settings.Default.targetLang,
        )
    }

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { prefs ->
            val current = Settings(
                provider = prefs[Keys.PROVIDER]?.let(::providerFromString) ?: Settings.Default.provider,
                deeplApiKey = prefs[Keys.DEEPL_API_KEY] ?: Settings.Default.deeplApiKey,
                openaiApiKey = prefs[Keys.OPENAI_API_KEY] ?: Settings.Default.openaiApiKey,
                openaiModel = prefs[Keys.OPENAI_MODEL] ?: Settings.Default.openaiModel,
                sourceLang = prefs[Keys.SOURCE_LANG] ?: Settings.Default.sourceLang,
                targetLang = prefs[Keys.TARGET_LANG] ?: Settings.Default.targetLang,
            )
            val next = transform(current)
            prefs[Keys.PROVIDER] = next.provider.name
            prefs[Keys.DEEPL_API_KEY] = next.deeplApiKey
            prefs[Keys.OPENAI_API_KEY] = next.openaiApiKey
            prefs[Keys.OPENAI_MODEL] = next.openaiModel
            prefs[Keys.SOURCE_LANG] = next.sourceLang
            prefs[Keys.TARGET_LANG] = next.targetLang
        }
    }
}

private fun providerFromString(value: String): Provider =
    runCatching { Provider.valueOf(value) }.getOrDefault(Settings.Default.provider)
