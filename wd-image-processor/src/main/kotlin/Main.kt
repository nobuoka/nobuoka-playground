package info.vividcode.sample.wdip

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.nio.charset.StandardCharsets
import java.util.*

import io.ktor.routing.*
import io.ktor.application.*
import io.ktor.content.OutgoingContent
import io.ktor.http.ContentType
import io.ktor.pipeline.PipelineInterceptor
import io.ktor.response.respond
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ByteArrayContent(override val contentType: ContentType, private val bytes: ByteArray) : OutgoingContent.ByteArrayContent() {
    override fun bytes(): ByteArray = bytes
}

fun main(args: Array<String>) {
    val webDriverBaseUrl = System.getenv("WD_BASE_URL") ?: "http://localhost:10000"
    val processorsConfigJsonPath = System.getenv("PROCESSORS_CONFIG_PATH") ?: "./sampleProcessors/processors.json"

    val okHttpClient = OkHttpClient.Builder()
        .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()
    val wdHttpRequestDispatcher = OkHttpWebDriverCommandHttpRequestDispatcher(okHttpClient, webDriverBaseUrl)
    val wdCommandExecutor = OkHttpWebDriverCommandExecutor(wdHttpRequestDispatcher)

    val settings = parseProcessorsConfigJson(Paths.get(processorsConfigJsonPath))
    val functionMap = settings.map {
        it.path to createWdImageProcessingPipelineInterceptor(WdImageProcessingExecutor(it.html, it.js, wdCommandExecutor))
    }.toMap()

    val server = embeddedServer(Netty, 8080) {
        intercept(ApplicationCallPipeline.Call) {
            try {
                proceed()
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }

        routing {
            functionMap.map { function ->
                get(function.key, function.value)
            }
        }
    }
    server.start(wait = true)
}

data class ProcessorSetting(val path: String, val html: String, val js: String)

fun parseProcessorsConfigJson(jsonFile: Path): List<ProcessorSetting> {
    fun Path.readContent(): String = Files.readAllBytes(jsonFile.parent.resolve(this)).toString(StandardCharsets.UTF_8)

    val configObject = jsonFile.toFile().reader().use { Klaxon().parseJsonObject(it) }
    return configObject.entries.map { (path, config) ->
        val htmlString = (config as? JsonObject)?.string("html")?.let { Paths.get(it).readContent() }
        val jsString = (config as? JsonObject)?.string("js")?.let { Paths.get(it).readContent() }
        ProcessorSetting(path, htmlString ?: "", jsString ?: "")
    }
}

data class WindowRect(val width: Int, val height: Int)

fun createWdImageProcessingPipelineInterceptor(wdImageProcessingExecutor: WdImageProcessingExecutor): PipelineInterceptor<Unit, ApplicationCall> = {
    val width = call.request.queryParameters["width"]?.toIntOrNull() ?: 360
    val height = call.request.queryParameters["height"]?.toIntOrNull() ?: 360
    val arg = call.request.queryParameters["arg"] ?: "null"
    call.respond(ByteArrayContent(ContentType.Image.PNG, wdImageProcessingExecutor.execute(WindowRect(width, height), arg)))
}

class WdImageProcessingExecutor(private val htmlString: String, private val jsString: String, private val webDriver: WebDriverCommandExecutor) {
    fun execute(windowRect: WindowRect, jsArg: String): ByteArray {
        return with(webDriver) {
            WebDriverCommand.NewSession().execute().use { session ->
                WebDriverCommand.Go(session, createHtmlDataUrl(htmlString)).execute()
                WebDriverCommand.SetWindowRect(session, Rect(windowRect.width, windowRect.height)).execute()
                val executeResult = WebDriverCommand.ExecuteAsyncScript(session, Script(jsString, listOf(jsArg))).execute()
                println("Execute result: $executeResult")
                WebDriverCommand.TakeScreenshot(session).execute()
            }
        }
    }
}

fun createHtmlDataUrl(html: String) =
        "data:text/html;charset=utf-8;base64,${Base64.getEncoder().encodeToString(html.toByteArray(StandardCharsets.UTF_8))}"
