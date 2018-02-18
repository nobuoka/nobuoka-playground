package info.vividcode.sample.wdip

class WebDriverSessionManager(
    private val wd: WebDriverCommandFactory,
    private val okHttpWebDriverCommandHttpRequestDispatcher: OkHttpWebDriverCommandHttpRequestDispatcher
) {
    fun <T> startSession(sessionFunction: (String, OkHttpWebDriverCommandHttpRequestDispatcher) -> T): T {
        val request = wd.createNewSessionCommandHttpRequest()
        val responseContent = okHttpWebDriverCommandHttpRequestDispatcher.dispatch(request)
        val sessionId =
            // W3C WebDriver
            responseContent.obj("value")?.string("sessionId") ?:
            responseContent.string("sessionId") ?:
            throw RuntimeException("Unexpected response content (sessionId not found): $responseContent")
        try {
            return sessionFunction.invoke(sessionId, okHttpWebDriverCommandHttpRequestDispatcher)
        } finally {
            okHttpWebDriverCommandHttpRequestDispatcher.dispatch(wd.createDeleteSessionCommandHttpRequest(sessionId))
        }
    }
}
