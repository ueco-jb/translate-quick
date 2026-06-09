package com.onebuttontranslate.translate

import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OpenAITranslatorTest {

    @Test fun system_prompt_mentions_both_languages_and_forbids_commentary() {
        val prompt = OpenAITranslator.systemPrompt("EN", "ES")
        assertTrue(prompt.contains("EN"))
        assertTrue(prompt.contains("ES"))
        assertTrue(prompt.contains("ONLY") || prompt.contains("only"))
    }

    @Test fun request_body_includes_model_messages_and_low_temperature() {
        val raw = OpenAITranslator.buildRequestBody(
            model = "gpt-4o-mini",
            text = "Hello",
            sourceLang = "EN",
            targetLang = "ES",
        )
        val obj = JSONObject(raw)
        assertEquals("gpt-4o-mini", obj.getString("model"))
        assertTrue("temperature should be low for translation", obj.getDouble("temperature") <= 0.3)

        val msgs = obj.getJSONArray("messages")
        assertEquals(2, msgs.length())
        assertEquals("system", msgs.getJSONObject(0).getString("role"))
        assertEquals("user", msgs.getJSONObject(1).getString("role"))
        assertEquals("Hello", msgs.getJSONObject(1).getString("content"))
    }

    @Test fun parses_content_from_chat_completion_response() {
        val body = """
            {"id":"x","choices":[{"index":0,"message":{"role":"assistant","content":"Hola mundo"}}]}
        """.trimIndent()
        assertEquals("Hola mundo", OpenAITranslator.parseResponse(body))
    }

    @Test fun parse_trims_whitespace_from_content() {
        val body = """{"choices":[{"message":{"content":"  Hola  \n"}}]}"""
        assertEquals("Hola", OpenAITranslator.parseResponse(body))
    }

    @Test fun parse_throws_when_choices_missing() {
        try {
            OpenAITranslator.parseResponse("""{"unexpected":true}""")
            fail("expected TranslationException")
        } catch (e: TranslationException) {
            assertTrue(e.userMessage.contains("unrecognised", ignoreCase = true))
        }
    }

    @Test fun parse_throws_on_blank_content() {
        try {
            OpenAITranslator.parseResponse("""{"choices":[{"message":{"content":""}}]}""")
            fail("expected TranslationException")
        } catch (e: TranslationException) {
            assertTrue(e.userMessage.contains("empty", ignoreCase = true))
        }
    }

    @Test fun error_messages_are_mapped_by_status_code() {
        assertTrue(OpenAITranslator.openAIErrorMessage(401, "").contains("rejected", ignoreCase = true))
        assertTrue(OpenAITranslator.openAIErrorMessage(429, "").contains("rate limit", ignoreCase = true))
        assertTrue(OpenAITranslator.openAIErrorMessage(502, "").contains("service error", ignoreCase = true))
    }

    @Test fun error_message_extracts_openai_error_detail() {
        val body = """{"error":{"message":"The model 'gpt-9' does not exist","type":"invalid_request_error"}}"""
        val msg = OpenAITranslator.openAIErrorMessage(404, body)
        assertTrue(msg.contains("does not exist"))
    }

    @Test fun translate_sets_bearer_auth_header_and_returns_content() = runTest {
        val fake = FakeHttpClient(
            HttpResponse(200, """{"choices":[{"message":{"content":"Hola"}}]}"""),
        )
        val out = OpenAITranslator(
            apiKey = "sk-test",
            model = "gpt-4o-mini",
            http = fake,
        ).translate("Hello", sourceLang = "EN", targetLang = "ES")

        assertEquals("Hola", out)
        assertEquals(OpenAITranslator.ENDPOINT, fake.lastUrl)
        assertEquals("Bearer sk-test", fake.lastHeaders["Authorization"])
    }

    @Test fun translate_throws_translation_exception_on_http_error() = runTest {
        val errorBody = """{"error":{"message":"Invalid API key"}}"""
        val fake = FakeHttpClient(HttpResponse(401, errorBody))
        try {
            OpenAITranslator("bad", "gpt-4o-mini", fake).translate("hi", "EN", "ES")
            fail("expected TranslationException")
        } catch (e: TranslationException) {
            assertTrue(e.userMessage.contains("rejected", ignoreCase = true))
        }
    }

    @Test fun translate_throws_when_api_key_blank() = runTest {
        val fake = FakeHttpClient(HttpResponse(200, ""))
        try {
            OpenAITranslator("", "gpt-4o-mini", fake).translate("hi", "EN", "ES")
            fail("expected TranslationException")
        } catch (e: TranslationException) {
            assertNotNull(e.userMessage)
            assertEquals(0, fake.callCount)
        }
    }

    @Test fun translate_throws_when_model_blank() = runTest {
        val fake = FakeHttpClient(HttpResponse(200, ""))
        try {
            OpenAITranslator("sk-test", "", fake).translate("hi", "EN", "ES")
            fail("expected TranslationException")
        } catch (e: TranslationException) {
            assertNotNull(e.userMessage)
            assertEquals(0, fake.callCount)
        }
    }

    @Test fun translate_returns_empty_for_blank_input_without_http_call() = runTest {
        val fake = FakeHttpClient(HttpResponse(500, "should not be called"))
        val out = OpenAITranslator("sk-test", "gpt-4o-mini", fake).translate("   ", "EN", "ES")
        assertEquals("", out)
        assertEquals(0, fake.callCount)
    }
}
