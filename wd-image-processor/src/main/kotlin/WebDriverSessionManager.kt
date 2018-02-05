package info.vividcode.sample.wdip

class WebDriverSessionManager(private val okHttpWebDriverCommandHttpRequestDispatcher: OkHttpWebDriverCommandHttpRequestDispatcher) {
    fun <T> startSession(sessionFunction: (String, OkHttpWebDriverCommandHttpRequestDispatcher) -> T): T {
        val request = createNewSessionCommandHttpRequest()
        val responseContent = okHttpWebDriverCommandHttpRequestDispatcher.dispatch(request)
        val sessionId = responseContent.obj("value")?.string("sessionId")
                ?: throw RuntimeException("Unexpected response content (sessionId not found): $responseContent")
        try {
            return sessionFunction.invoke(sessionId, okHttpWebDriverCommandHttpRequestDispatcher)
        } finally {
            okHttpWebDriverCommandHttpRequestDispatcher.dispatch(createDeleteSessionCommandHttpRequest(sessionId))
        }
    }
}
