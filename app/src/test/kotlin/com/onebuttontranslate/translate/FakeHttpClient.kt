package com.onebuttontranslate.translate

/** Records every request and returns a caller-supplied [HttpResponse]. */
class FakeHttpClient(private val response: HttpResponse) : HttpClient {
    var lastUrl: String? = null
        private set
    var lastHeaders: Map<String, String> = emptyMap()
        private set
    var lastBody: String? = null
        private set
    var callCount: Int = 0
        private set

    override suspend fun post(
        url: String,
        headers: Map<String, String>,
        body: String,
    ): HttpResponse {
        lastUrl = url
        lastHeaders = headers
        lastBody = body
        callCount += 1
        return response
    }
}
