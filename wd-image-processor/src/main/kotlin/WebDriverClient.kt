package info.vividcode.sample.wdip

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

interface NewSessionCommandExecutor {
    fun WebDriverCommand.NewSession.execute(): WebDriverSession
}
interface DeleteSessionCommandExecutor {
    fun WebDriverCommand.DeleteSession.execute()

    fun <T> WebDriverSession.use(task: (WebDriverSession) -> T): T {
        val webDriverSession = this
        return AutoCloseable { WebDriverCommand.DeleteSession(webDriverSession).execute() }.use {
            task(webDriverSession)
        }
    }
}
interface SetWindowRectExecutor { fun WebDriverCommand.SetWindowRect.execute() }
interface GoCommandExecutor { fun WebDriverCommand.Go.execute() }
interface ExecuteScriptCommandExecutor { fun WebDriverCommand.ExecuteAsyncScript.execute(): ScriptResult }
interface TakeScreenshotCommandExecutor { fun WebDriverCommand.TakeScreenshot.execute(): ByteArray }
interface TakeElementScreenshotCommandExecutor { fun WebDriverCommand.TakeElementScreenshot.execute(): ByteArray }
interface FindElementCommandExecutor { fun WebDriverCommand.FindElement.execute(): WebElement }

data class ElementSelector(val strategy: Strategy, val value: String) {
    enum class Strategy {
        //CSS, // ChromeDriver は "css" で、geckodriver は "css selector"?
        XPATH,
    }
}
data class WebElement(val reference: String) {
    companion object {
        const val IDENTIFIER = "element-6066-11e4-a52e-4f735466cecf"
        const val DEPRECATED_IDENTIFIER = "ELEMENT"

        fun from(obj: JsonObject): WebElement {
            val webElementReference =
                    obj.string(WebElement.IDENTIFIER) ?:
                    obj.string(WebElement.DEPRECATED_IDENTIFIER) ?:
                    throw RuntimeException("$obj")
            return WebElement(webElementReference)
        }
    }
}

interface WebDriverCommandExecutor :
    NewSessionCommandExecutor, DeleteSessionCommandExecutor,
    SetWindowRectExecutor,
    GoCommandExecutor, ExecuteScriptCommandExecutor,
    TakeScreenshotCommandExecutor, TakeElementScreenshotCommandExecutor,
    FindElementCommandExecutor

open class OkHttpWebDriverCommandExecutor(
    protected val dispatcher: OkHttpWebDriverCommandHttpRequestDispatcher
) : WebDriverCommandExecutor {

    class OkHttpWebDriverSession(override val id: String) : WebDriverSession

    // TODO : Escaping
    private val WebDriverCommand.SessionCommand.sessionPathSegment get() = session.id

    override fun WebDriverCommand.NewSession.execute(): WebDriverSession =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest("POST", "/session", JsonObject(
                mapOf("desiredCapabilities" to JsonObject(mapOf()))).toJsonString()
            )
        ) {
            val sessionId =
                it.obj("value")?.string("sessionId") ?:
                it.string("sessionId") ?:
                throw RuntimeException("Unexpected response content (sessionId not found): $it")
            OkHttpWebDriverSession(sessionId)
        }

    override fun WebDriverCommand.DeleteSession.execute() =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest("DELETE", "/session/$sessionPathSegment", null)
        ) {}

    override fun WebDriverCommand.SetWindowRect.execute() =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest("POST", "/session/$sessionPathSegment/window/rect", JsonObject(mapOf(
                "height" to rect.height + 105,
                "width" to rect.width
            )).toJsonString())
        ) {}

    override fun WebDriverCommand.Go.execute() =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest("POST", "/session/$sessionPathSegment/url", JsonObject(mapOf("url" to url)).toJsonString())
        ) {}

    override fun WebDriverCommand.ExecuteAsyncScript.execute() =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest("POST", "/session/$sessionPathSegment/execute/async", JsonObject(mapOf(
                // From https://github.com/manakai/perl-web-driver-client/blob/master/lib/Web/Driver/Client/Session.pm#L103
                "script" to """
                    var code = new Function(arguments[0]);
                    var args = arguments[1];
                    var callback = arguments[2];
                    Promise.resolve().then(function () {
                      return code.apply(null, args);
                    }).then(function (r) {
                      callback([true, r]);
                    }, function (e) {
                      callback([false, e + ""]);
                    });
                    """,
                "args" to JsonArray(script.script, JsonArray(script.args ?: emptyList()))
            )).toJsonString())
        ) {
            (it.array<Any?>("value") ?: throw RuntimeException("$it")).let {
                when (it[0]) {
                    true -> it[1].let {
                        when (it) {
                        // Possible types : https://github.com/cbeust/klaxon#low-level-api
                            is Int -> ScriptResult.Number(BigDecimal(it))
                            is Long -> ScriptResult.Number(BigDecimal(it))
                            is BigInteger -> ScriptResult.Number(BigDecimal(it))
                            is Double -> ScriptResult.Number(BigDecimal(it))
                            is String -> ScriptResult.String(it)
                            is Boolean -> ScriptResult.Boolean(it)
                            is JsonObject -> ScriptResult.Object(it)
                            is JsonArray<*> -> ScriptResult.Array(it)
                            null -> ScriptResult.Null
                            else -> throw RuntimeException("")
                        }
                    }
                    false -> throw RuntimeException("${it[1]}")
                    else -> throw RuntimeException("$it")
                }
            }
        }

    override fun WebDriverCommand.TakeElementScreenshot.execute(): ByteArray =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest("GET", "/session/$sessionPathSegment/element/${this.targetElement.reference}/screenshot", null)
        ) {
            val screenshotBase64 = it.string("value")
            Base64.getDecoder().decode(screenshotBase64)
        }

    override fun WebDriverCommand.TakeScreenshot.execute(): ByteArray =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest("GET", "/session/$sessionPathSegment/screenshot", null)
        ) {
            val screenshotBase64 = it.string("value")
            Base64.getDecoder().decode(screenshotBase64)
        }

    override fun WebDriverCommand.FindElement.execute(): WebElement =
        dispatcher.dispatch(
            WebDriverCommandHttpRequest("POST", "/session/$sessionPathSegment/element", JsonObject(mapOf(
                "using" to when (selector.strategy) {
                    //ElementSelector.Strategy.CSS -> "css" // "css selector"
                    ElementSelector.Strategy.XPATH -> "xpath"
                },
                "value" to selector.value
            )).toJsonString())
        ) {
            (it.obj("value") ?: throw RuntimeException("$it")).let(WebElement.Companion::from)
        }

}

interface WebDriverCommand {
    class NewSession : WebDriverCommand

    interface SessionCommand {
        val session: WebDriverSession
    }

    data class DeleteSession(override val session: WebDriverSession) : SessionCommand
    data class SetWindowRect(override val session: WebDriverSession, val rect: Rect) : SessionCommand
    data class Go(override val session: WebDriverSession, val url: String) : SessionCommand
    data class ExecuteAsyncScript(override val session: WebDriverSession, val script: Script) : SessionCommand
    data class TakeScreenshot(override val session: WebDriverSession) : SessionCommand
    data class TakeElementScreenshot(override val session: WebDriverSession, val targetElement: WebElement) : SessionCommand
    data class FindElement(override val session: WebDriverSession, val selector: ElementSelector) : SessionCommand

}

sealed class ScriptResult {
    data class Object(val value: JsonObject) : ScriptResult()
    data class Array(val value: JsonArray<*>) : ScriptResult()
    data class String(val value: kotlin.String) : ScriptResult()
    data class Number(val value: BigDecimal) : ScriptResult()
    data class Boolean(val value: kotlin.Boolean) : ScriptResult()
    object Null : ScriptResult()
}
data class Rect(val width: Int, val height: Int)
data class Script(val script: String, val args: List<Any>? = null)

interface WebDriverSession {
    val id: String
}
