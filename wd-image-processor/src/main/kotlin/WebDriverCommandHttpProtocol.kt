package info.vividcode.sample.wdip

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

data class ElementSelector(val strategy: Strategy, val value: String) {
    enum class Strategy {
        CSS
    }
}
data class Element(val id: String)

interface WebDriverCommandFactory {
    fun createNewSessionCommandHttpRequest(): WebDriverCommandHttpRequest
    fun createDeleteSessionCommandHttpRequest(sessionId: String): WebDriverCommandHttpRequest
    fun createGoCommand(sessionId: String, url: String): WebDriverCommandHttpRequest
    fun createTakeScreenshotCommand(sessionId: String): WebDriverCommandHttpRequest
    fun createTakeElementScreenshotCommand(sessionId: String, element: Element): WebDriverCommandHttpRequest
    fun createExecuteScriptCommand(
        sessionId: String,
        script: String,
        args: List<Any>? = null
    ): WebDriverCommandHttpRequest
    fun createSetWindowRect(
        sessionId: String,
        width: Int,
        height: Int
    ): WebDriverCommandHttpRequest
    fun createFindElementCommand(
        sessionId: String,
        selector: ElementSelector
    ): WebDriverCommandHttpRequest

    object ForGeckodriver : WebDriverCommandFactory {
        override fun createTakeElementScreenshotCommand(
            sessionId: String,
            element: Element
        ): WebDriverCommandHttpRequest {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun createFindElementCommand(
            sessionId: String,
            selector: ElementSelector
        ): WebDriverCommandHttpRequest {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun createNewSessionCommandHttpRequest() =
            WebDriverCommandHttpRequest("POST", "/session", "{}")
        override fun createDeleteSessionCommandHttpRequest(sessionId: String) =
            WebDriverCommandHttpRequest("DELETE", "/session/$sessionId", null)
        override fun createGoCommand(sessionId: String, url: String) =
            WebDriverCommandHttpRequest("POST", "/session/$sessionId/url", JsonObject(mapOf("url" to url)).toJsonString())
        override fun createTakeScreenshotCommand(sessionId: String) =
            WebDriverCommandHttpRequest("GET", "/session/$sessionId/screenshot", null)
        override fun createExecuteScriptCommand(sessionId: String, script: String, args: List<Any>?) =
            WebDriverCommandHttpRequest("POST", "/session/$sessionId/execute/sync", JsonObject(mapOf(
                "script" to script,
                "args" to args?.let { JsonArray(it) }
            )).toJsonString())
        override fun createSetWindowRect(sessionId: String, width: Int, height: Int): WebDriverCommandHttpRequest =
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    object ForChromeDriver : WebDriverCommandFactory {
        override fun createNewSessionCommandHttpRequest() =
            WebDriverCommandHttpRequest("POST", "/session", JsonObject(mapOf(
                "desiredCapabilities" to JsonObject(mapOf()))
            ).toJsonString())
        override fun createDeleteSessionCommandHttpRequest(sessionId: String) =
            WebDriverCommandHttpRequest("DELETE", "/session/$sessionId", null)
        override fun createGoCommand(sessionId: String, url: String) =
            WebDriverCommandHttpRequest("POST", "/session/$sessionId/url", JsonObject(mapOf("url" to url)).toJsonString())
        override fun createTakeScreenshotCommand(sessionId: String) =
            WebDriverCommandHttpRequest("GET", "/session/$sessionId/screenshot", null)
        override fun createTakeElementScreenshotCommand(sessionId: String, element: Element) =
            WebDriverCommandHttpRequest("GET", "/session/$sessionId/element/${element.id}/screenshot", null)
        override fun createExecuteScriptCommand(sessionId: String, script: String, args: List<Any>?) =
            WebDriverCommandHttpRequest("POST", "/session/$sessionId/execute/async", JsonObject(mapOf(
                // From https://github.com/manakai/perl-web-driver-client/blob/master/lib/Web/Driver/Client/Session.pm#L103
                "script" to """
                    var code = new Function(arguments[0]);
                    var args = arguments[1];
                    var callback = arguments[2];
                    Promise.resolve().then (function () {
                      return code.apply(null, args);
                    }).then(function (r) {
                      callback([true, r]);
                    }, function (e) {
                      callback([false, e + ""]);
                    });
                    """,
                "args" to JsonArray(script, JsonArray(args ?: emptyList()))
            )).toJsonString())
        override fun createSetWindowRect(sessionId: String, width: Int, height: Int): WebDriverCommandHttpRequest =
            WebDriverCommandHttpRequest("POST", "/session/$sessionId/window/rect", JsonObject(mapOf(
                "height" to height + 105,
                "width" to width
            )).toJsonString())
        override fun createFindElementCommand(
            sessionId: String,
            selector: ElementSelector
        ): WebDriverCommandHttpRequest =
            WebDriverCommandHttpRequest("POST", "/session/$sessionId/element", JsonObject(mapOf(
                "using" to when (selector.strategy) {
                    ElementSelector.Strategy.CSS -> "css"
                },
                "value" to selector.value
            )).toJsonString())
    }
}

data class WebDriverCommandHttpRequest(
        val method: String,
        val path: String,
        val requestContent: String?
)

class OkHttpWebDriverCommandHttpRequestDispatcher(private val okHttpClient: OkHttpClient, private val baseUrl: String) {
    fun dispatch(commandHttpRequest: WebDriverCommandHttpRequest): JsonObject {
        val okHttpRequest = Request.Builder()
                .url(baseUrl + commandHttpRequest.path)
                .method(commandHttpRequest.method, commandHttpRequest.requestContent?.let { RequestBody.create(MediaType.parse("application/json"), it) })
                .build()
        val response = okHttpClient.newCall(okHttpRequest).execute()
        if (response.isSuccessful) {
            return response.body()?.charStream()?.let { Parser().parse(it) } as? JsonObject
                    ?: throw RuntimeException("Unexpected response: $response")
        } else {
            throw RuntimeException("Http request error: $response")
        }
    }
}
