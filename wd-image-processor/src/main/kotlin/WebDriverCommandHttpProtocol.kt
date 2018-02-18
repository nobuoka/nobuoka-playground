package info.vividcode.sample.wdip

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

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

    fun <T> dispatch(commandHttpRequest: WebDriverCommandHttpRequest, responseHandler: (JsonObject) -> T): T =
        responseHandler(dispatch(commandHttpRequest))
}
