package com.onebuttontranslate.translate

import org.json.JSONArray
import org.json.JSONObject

/**
 * OpenAI Chat Completions translator.
 *
 * We deliberately use a tight system prompt that forbids commentary so the model
 * returns translation-only text suitable to render in the result box verbatim.
 *
 * Docs: https://platform.openai.com/docs/api-reference/chat/create
 */
class OpenAITranslator(
    private val apiKey: String,
    private val model: String,
    private val http: HttpClient = DefaultHttpClient,
) : Translator {

    override suspend fun translate(text: String, sourceLang: String, targetLang: String): String {
        if (apiKey.isBlank()) throw TranslationException("OpenAI API key is empty.")
        if (model.isBlank()) throw TranslationException("OpenAI model is empty.")
        if (text.isBlank()) return ""

        val body = buildRequestBody(model, text, sourceLang, targetLang)

        val response = http.post(
            url = ENDPOINT,
            headers = mapOf(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to "application/json",
                "Accept" to "application/json",
            ),
            body = body,
        )

        if (response.code !in 200..299) {
            throw TranslationException(openAIErrorMessage(response.code, response.body))
        }

        return parseResponse(response.body)
    }

    companion object {
        internal const val ENDPOINT = "https://api.openai.com/v1/chat/completions"

        internal fun systemPrompt(sourceLang: String, targetLang: String): String =
            "You are a translator. Translate the user's message from $sourceLang to $targetLang. " +
                "Reply with ONLY the translated text. No quotes, no notes, no explanations, " +
                "no language tags. Preserve line breaks and punctuation."

        internal fun buildRequestBody(
            model: String,
            text: String,
            sourceLang: String,
            targetLang: String,
        ): String = JSONObject().apply {
            put("model", model)
            put("temperature", 0.2)
            put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "system")
                            .put("content", systemPrompt(sourceLang, targetLang)),
                    )
                    .put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", text),
                    ),
            )
        }.toString()

        internal fun parseResponse(body: String): String {
            val content = runCatching {
                JSONObject(body)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .optString("content", "")
            }.getOrElse {
                throw TranslationException("OpenAI returned an unrecognised response.")
            }
            if (content.isBlank()) throw TranslationException("OpenAI returned empty content.")
            return content.trim()
        }

        internal fun openAIErrorMessage(code: Int, body: String): String {
            val detail = runCatching {
                JSONObject(body).getJSONObject("error").optString("message", "")
            }.getOrDefault("")
            return when (code) {
                401 -> "OpenAI rejected the API key."
                404 -> if (detail.isNotBlank()) "OpenAI: $detail" else "OpenAI model not found."
                429 -> "OpenAI rate limit or quota hit."
                in 500..599 -> "OpenAI service error (HTTP $code). Try again later."
                else -> if (detail.isNotBlank()) "OpenAI: $detail" else "OpenAI failed (HTTP $code)."
            }
        }
    }
}
