package com.onebuttontranslate.data

/** Translation backend choices the user can switch between in Settings. */
enum class Provider(val displayName: String) {
    DEEPL("DeepL"),
    OPENAI("OpenAI"),
}
