package com.onebuttontranslate.translate

import org.json.JSONArray
import org.json.JSONObject

/**
 * DeepL Translate API client.
 *
 * Endpoint is chosen from the API key: keys ending in `:fx` belong to the Free plan
 * and must hit `api-free.deepl.com`; everything else is Pro.
 *
 * Docs: https://developers.deepl.com/docs/api-reference/translate
 */
class DeepLTranslator(
    private val apiKey: String,
    private val http: HttpClient = DefaultHttpClient,
) : Translator {

    override suspend fun translate(text: String, sourceLang: String, targetLang: String): String {
        if (apiKey.isBlank()) throw TranslationException("DeepL API key is empty.")
        if (text.isBlank()) return ""

        val body = JSONObject().apply {
            put("text", JSONArray().put(text))
            // DeepL is permissive with `source_lang` casing; we send what the user typed
            // (normalised upstream). `target_lang` is required.
            put("source_lang", sourceLang.uppercase())
            put("target_lang", targetLang.uppercase())
        }.toString()

        val response = http.post(
            url = endpointFor(apiKey),
            headers = mapOf(
                "Authorization" to "DeepL-Auth-Key $apiKey",
                "Content-Type" to "application/json",
                "Accept" to "application/json",
            ),
            body = body,
        )

        if (response.code !in 200..299) {
            throw TranslationException(deeplErrorMessage(response.code, response.body))
        }

        return parseResponse(response.body)
    }

    companion object {
        internal const val FREE_ENDPOINT = "https://api-free.deepl.com/v2/translate"
        internal const val PRO_ENDPOINT = "https://api.deepl.com/v2/translate"

        internal fun endpointFor(apiKey: String): String =
            if (apiKey.trim().endsWith(":fx")) FREE_ENDPOINT else PRO_ENDPOINT

        /** Extracts the first translation from a successful DeepL response body. */
        internal fun parseResponse(body: String): String {
            val translations = runCatching {
                JSONObject(body).getJSONArray("translations")
            }.getOrElse {
                throw TranslationException("DeepL returned an unrecognised response.")
            }
            if (translations.length() == 0) {
                throw TranslationException("DeepL returned no translation.")
            }
            return translations.getJSONObject(0).optString("text", "")
        }

        internal fun deeplErrorMessage(code: Int, body: String): String {
            val detail = runCatching { JSONObject(body).optString("message", "") }.getOrDefault("")
            return when (code) {
                401, 403 -> "DeepL rejected the API key (HTTP $code)."
                429 -> "DeepL rate limit hit. Try again shortly."
                456 -> "DeepL quota exceeded for this billing period."
                in 500..599 -> "DeepL service error (HTTP $code). Try again later."
                else -> if (detail.isNotBlank()) "DeepL: $detail" else "DeepL failed (HTTP $code)."
            }
        }
    }
}
