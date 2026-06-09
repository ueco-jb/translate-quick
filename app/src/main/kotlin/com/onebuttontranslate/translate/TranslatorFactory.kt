package com.onebuttontranslate.translate

import com.onebuttontranslate.data.Provider
import com.onebuttontranslate.data.Settings

/** Builds the [Translator] that matches the current [Settings.provider]. */
object TranslatorFactory {
    fun forSettings(settings: Settings, http: HttpClient = DefaultHttpClient): Translator =
        when (settings.provider) {
            Provider.DEEPL -> DeepLTranslator(settings.deeplApiKey, http)
            Provider.OPENAI -> OpenAITranslator(settings.openaiApiKey, settings.openaiModel, http)
        }
}
