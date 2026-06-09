package com.onebuttontranslate.data

/**
 * The fixed set of languages the picker offers. Codes are the two-letter forms
 * DeepL accepts directly and that OpenAI handles via prompt context. Add a row
 * here when you want a new option in the UI -- nothing else needs to change.
 */
data class Language(
    val code: String,
    val displayName: String,
    /**
     * BCP-47 locale tag passed to Android's speech recognizer (and later, the
     * text-to-speech engine). Pick the most widely-supported regional variant
     * for each language so on-device models exist.
     */
    val locale: String,
) {
    companion object {
        val ALL: List<Language> = listOf(
            Language("EN", "English", "en-US"),
            Language("ES", "Spanish", "es-ES"),
            Language("DE", "German", "de-DE"),
            Language("FR", "French", "fr-FR"),
            Language("JA", "Japanese", "ja-JP"),
            Language("PL", "Polish", "pl-PL"),
        )

        /** Returns the matching [Language] or null if [code] is unknown. */
        fun byCode(code: String): Language? =
            ALL.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
