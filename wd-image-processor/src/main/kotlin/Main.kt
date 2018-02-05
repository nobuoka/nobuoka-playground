package info.vividcode.sample.wdip

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.nio.charset.StandardCharsets
import java.util.*

import io.ktor.routing.*
import io.ktor.application.*
import io.ktor.content.OutgoingContent
import io.ktor.http.ContentType
import io.ktor.response.respond
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

class ByteArrayContent(override val contentType: ContentType, private val bytes: ByteArray) : OutgoingContent.ByteArrayContent() {
    override fun bytes(): ByteArray = bytes
}

fun main(args: Array<String>) {
    val webDriverBaseUrl = System.getenv("WD_BASE_URL") ?: "http://localhost:9516"

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
            get("/map") {
                call.respond(ByteArrayContent(ContentType.Image.PNG, foo(webDriverBaseUrl)))
            }
        }
    }
    server.start(wait = true)
}

fun foo(webDriverBaseUrl: String): ByteArray {
    val okHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()
    val baseUrl = webDriverBaseUrl
    val wdHttpRequestDispatcher = OkHttpWebDriverCommandHttpRequestDispatcher(okHttpClient, baseUrl)
    val wdSessionManager = WebDriverSessionManager(wdHttpRequestDispatcher)

    return wdSessionManager.startSession { sessionId, wdHttpRequestDispatcher ->
        val html = """<!doctype html>
<html lang="en">
  <head>
    <link rel="stylesheet" href="https://openlayers.org/en/v4.6.4/css/ol.css" type="text/css">
    <style>
      html, body {
        margin: 0;
        padding: 0;
        height: 100%;
        width: 100%;
      }
      .map {
        height: 100%;
        width: 100%;
      }
    </style>
    <script src="https://openlayers.org/en/v4.6.4/build/ol.js" type="text/javascript"></script>
    <title>OpenLayers example</title>
  </head>
  <body>
    <div id="map" class="map"></div>
  </body>
</html>
            """
        wdHttpRequestDispatcher.dispatch(createGoCommand(sessionId, createHtmlDataUrl(html)))

        val script = """
      var map = new ol.Map({
        target: 'map',
        layers: [
          new ol.layer.Tile({
            source: new ol.source.OSM()
          })
        ],
        view: new ol.View({
          center: ol.proj.fromLonLat([37.41, 8.82]),
          zoom: 4
        })
      });
            return arguments[0];
            """
        val executeResult = wdHttpRequestDispatcher.dispatch(createExecuteScriptCommand(sessionId, script, listOf("test!?!?!"))).string("value")
        println("Execute result: $executeResult")

        Thread.sleep(8000)

        val screenshotBase64 = wdHttpRequestDispatcher.dispatch(createTakeScreenshotCommand(sessionId)).string("value")
        val screenshot = Base64.getDecoder().decode(screenshotBase64)
        //Files.write(Paths.get("./test.png"), screenshot, StandardOpenOption.CREATE)
        return@startSession screenshot
    }
}

fun createHtmlDataUrl(html: String) =
        "data:text/html;charset=utf-8;base64,${Base64.getEncoder().encodeToString(html.toByteArray(StandardCharsets.UTF_8))}"
