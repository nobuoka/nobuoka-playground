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

    val settings = parseProcessorsConfigJson(Paths.get(processorsConfigJsonPath))
    val functionMap = settings.map {
        it.path to createWdImageProcessingPipelineInterceptor(
            WebDriverFoo(it.html, it.js, webDriverBaseUrl, WebDriverCommandFactory.ForChromeDriver)
        )
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

fun createWdImageProcessingPipelineInterceptor(webDriverFoo: WebDriverFoo): PipelineInterceptor<Unit, ApplicationCall> = {
    val width = call.request.queryParameters.get("width")?.toIntOrNull() ?: 360
    val height = call.request.queryParameters.get("height")?.toIntOrNull() ?: 360
    val arg = call.request.queryParameters.get("arg") ?: "null"
    call.respond(ByteArrayContent(ContentType.Image.PNG, webDriverFoo.execute(WindowRect(width, height), arg)))
}

class WebDriverFoo(private val htmlString: String, private val jsString: String, private val webDriverBaseUrl: String, private val wd: WebDriverCommandFactory) {
    fun execute(windowRect: WindowRect, jsArg: String): ByteArray {
        val okHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()
        val baseUrl = webDriverBaseUrl
        val wdHttpRequestDispatcher = OkHttpWebDriverCommandHttpRequestDispatcher(okHttpClient, baseUrl)
        val wdSessionManager = WebDriverSessionManager(WebDriverCommandFactory.ForChromeDriver, wdHttpRequestDispatcher)

        return wdSessionManager.startSession { sessionId, wdHttpRequestDispatcher ->
            wdHttpRequestDispatcher.dispatch(wd.createGoCommand(sessionId, createHtmlDataUrl(htmlString)))
            wdHttpRequestDispatcher.dispatch(wd.createSetWindowRect(sessionId, windowRect.width, windowRect.height))

            // Take Element Screenshot command is not implemented by ChromeDriver...
//            val elementId = wdHttpRequestDispatcher.dispatch(wd.createFindElementCommand(sessionId, ElementSelector(ElementSelector.Strategy.CSS, "#main"))).obj("value")?.string("ELEMENT")
//                    ?: throw RuntimeException()
//            val element = Element(elementId)

            val executeResult = wdHttpRequestDispatcher.dispatch(wd.createExecuteScriptCommand(sessionId, jsString, listOf(jsArg))).get("value")
            println("Execute result: $executeResult")

            //val screenshotBase64 = wdHttpRequestDispatcher.dispatch(wd.createTakeElementScreenshotCommand(sessionId, element)).string("value")
            val screenshotBase64 = wdHttpRequestDispatcher.dispatch(wd.createTakeScreenshotCommand(sessionId)).string("value")
            val screenshot = Base64.getDecoder().decode(screenshotBase64)
            //Files.write(Paths.get("./test.png"), screenshot, StandardOpenOption.CREATE)
            return@startSession screenshot
        }
    }
}

fun createHtmlDataUrl(html: String) =
        "data:text/html;charset=utf-8;base64,${Base64.getEncoder().encodeToString(html.toByteArray(StandardCharsets.UTF_8))}"
