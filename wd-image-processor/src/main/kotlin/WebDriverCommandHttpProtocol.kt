package info.vividcode.sample.wdip

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

fun createNewSessionCommandHttpRequest() =
        WebDriverCommandHttpRequest("POST", "/session", "{}")
fun createDeleteSessionCommandHttpRequest(sessionId: String) =
        WebDriverCommandHttpRequest("DELETE", "/session/$sessionId", null)
fun createGoCommand(sessionId: String, url: String) =
        WebDriverCommandHttpRequest("POST", "/session/$sessionId/url", JsonObject(mapOf("url" to url)).toJsonString())
fun createTakeScreenshotCommand(sessionId: String) =
        WebDriverCommandHttpRequest("GET", "/session/$sessionId/screenshot", null)
fun createExecuteScriptCommand(sessionId: String, script: String, args: List<Any>? = null) =
        WebDriverCommandHttpRequest("POST", "/session/$sessionId/execute/sync", JsonObject(mapOf(
                "script" to script,
                "args" to args?.let { JsonArray(it) }
        )).toJsonString())

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
                    ?: throw RuntimeException("Unexpected response (sessionId not found): $response")
        } else {
            throw RuntimeException("Http request error: $response")
        }
    }
}
