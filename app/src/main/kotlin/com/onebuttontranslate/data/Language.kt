package com.onebuttontranslate.data

/**
 * The fixed set of languages the picker offers. Codes are the two-letter forms
 * DeepL accepts directly and that OpenAI handles via prompt context. Add a row
 * here when you want a new option in the UI -- nothing else needs to change.
 */
data class Language(val code: String, val displayName: String) {
    companion object {
        val ALL: List<Language> = listOf(
            Language("EN", "English"),
            Language("ES", "Spanish"),
            Language("DE", "German"),
            Language("FR", "French"),
            Language("JA", "Japanese"),
            Language("PL", "Polish"),
        )

        /** Returns the matching [Language] or null if [code] is unknown. */
        fun byCode(code: String): Language? =
            ALL.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
