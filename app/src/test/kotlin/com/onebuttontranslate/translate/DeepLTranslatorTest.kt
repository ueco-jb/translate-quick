package com.onebuttontranslate.translate

import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DeepLTranslatorTest {

    @Test fun endpoint_for_free_key_uses_api_free_host() {
        assertEquals(
            DeepLTranslator.FREE_ENDPOINT,
            DeepLTranslator.endpointFor("abc-123:fx"),
        )
    }

    @Test fun endpoint_for_pro_key_uses_api_host() {
        assertEquals(
            DeepLTranslator.PRO_ENDPOINT,
            DeepLTranslator.endpointFor("plain-pro-key"),
        )
    }

    @Test fun parses_translation_text_from_successful_response() {
        val body = """{"translations":[{"detected_source_language":"EN","text":"Hola"}]}"""
        assertEquals("Hola", DeepLTranslator.parseResponse(body))
    }

    @Test fun parses_first_translation_when_multiple_returned() {
        val body = """{"translations":[{"text":"Hola"},{"text":"Buenas"}]}"""
        assertEquals("Hola", DeepLTranslator.parseResponse(body))
    }

    @Test fun parse_throws_on_missing_translations_key() {
        try {
            DeepLTranslator.parseResponse("""{"unexpected":true}""")
            fail("expected TranslationException")
        } catch (e: TranslationException) {
            assertTrue(e.userMessage.contains("unrecognised", ignoreCase = true))
        }
    }

    @Test fun parse_throws_on_empty_translations_array() {
        try {
            DeepLTranslator.parseResponse("""{"translations":[]}""")
            fail("expected TranslationException")
        } catch (e: TranslationException) {
            assertTrue(e.userMessage.contains("no translation", ignoreCase = true))
        }
    }

    @Test fun error_messages_are_mapped_by_status_code() {
        assertTrue(DeepLTranslator.deeplErrorMessage(401, "").contains("rejected"))
        assertTrue(DeepLTranslator.deeplErrorMessage(403, "").contains("rejected"))
        assertTrue(DeepLTranslator.deeplErrorMessage(429, "").contains("rate limit", ignoreCase = true))
        assertTrue(DeepLTranslator.deeplErrorMessage(456, "").contains("quota", ignoreCase = true))
        assertTrue(DeepLTranslator.deeplErrorMessage(503, "").contains("service error", ignoreCase = true))
    }

    @Test fun error_message_includes_deepl_detail_when_present() {
        val body = """{"message":"Value for 'target_lang' not supported."}"""
        val msg = DeepLTranslator.deeplErrorMessage(400, body)
        assertTrue(msg.contains("Value for 'target_lang' not supported"))
    }

    @Test fun translate_sends_auth_header_uppercased_langs_and_json_body() = runTest {
        val fake = FakeHttpClient(
            HttpResponse(200, """{"translations":[{"text":"Hola mundo"}]}"""),
        )
        val out = DeepLTranslator(apiKey = "secret:fx", http = fake)
            .translate("Hello world", sourceLang = "en", targetLang = "es")

        assertEquals("Hola mundo", out)
        assertEquals(DeepLTranslator.FREE_ENDPOINT, fake.lastUrl)
        assertEquals("DeepL-Auth-Key secret:fx", fake.lastHeaders["Authorization"])
        assertEquals("application/json", fake.lastHeaders["Content-Type"])

        val body = JSONObject(fake.lastBody!!)
        assertEquals("EN", body.getString("source_lang"))
        assertEquals("ES", body.getString("target_lang"))
        val arr = body.getJSONArray("text")
        assertEquals(1, arr.length())
        assertEquals("Hello world", arr.getString(0))
    }

    @Test fun translate_propagates_translation_exception_on_http_error() = runTest {
        val fake = FakeHttpClient(HttpResponse(401, ""))
        try {
            DeepLTranslator("bad-key", fake).translate("x", "EN", "ES")
            fail("expected TranslationException")
        } catch (e: TranslationException) {
            assertTrue(e.userMessage.contains("rejected"))
        }
    }

    @Test fun translate_returns_empty_for_blank_input_and_skips_http() = runTest {
        val fake = FakeHttpClient(HttpResponse(500, "should not be called"))
        val out = DeepLTranslator("key", fake).translate("   ", "EN", "ES")
        assertEquals("", out)
        assertEquals(0, fake.callCount)
    }

    @Test fun translate_throws_when_api_key_blank() = runTest {
        val fake = FakeHttpClient(HttpResponse(200, ""))
        try {
            DeepLTranslator("", fake).translate("hello", "EN", "ES")
            fail("expected TranslationException")
        } catch (e: TranslationException) {
            assertNotNull(e.userMessage)
            assertEquals(0, fake.callCount)
        }
    }
}
