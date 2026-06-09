package com.onebuttontranslate.translate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** A one-shot translation backend. Throws [TranslationException] on any failure. */
interface Translator {
    suspend fun translate(text: String, sourceLang: String, targetLang: String): String
}

/** Domain error surfaced to the UI. [userMessage] is safe to render verbatim. */
class TranslationException(val userMessage: String, cause: Throwable? = null) :
    RuntimeException(userMessage, cause)

/**
 * Minimal HTTP seam so the translators stay unit-testable without a network.
 * Implementations MUST NOT throw on non-2xx; callers inspect [HttpResponse.code].
 */
fun interface HttpClient {
    suspend fun post(url: String, headers: Map<String, String>, body: String): HttpResponse
}

data class HttpResponse(val code: Int, val body: String)

/**
 * Real HTTP client using Android's built-in [HttpURLConnection]. Kept dependency-free
 * on purpose -- two endpoints don't justify pulling OkHttp.
 */
object DefaultHttpClient : HttpClient {
    override suspend fun post(
        url: String,
        headers: Map<String, String>,
        body: String,
    ): HttpResponse = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 30_000
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            HttpResponse(code, text)
        } finally {
            conn.disconnect()
        }
    }
}
