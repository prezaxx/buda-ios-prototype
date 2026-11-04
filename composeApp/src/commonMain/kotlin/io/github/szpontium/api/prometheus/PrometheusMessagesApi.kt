package io.github.szpontium.api.prometheus

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import io.github.szpontium.api.prometheus.models.PrometheusMailbox
import io.github.szpontium.api.prometheus.models.PrometheusMessage
import io.github.szpontium.api.prometheus.models.PrometheusMessageDetails
import io.github.szpontium.api.prometheus.models.PrometheusSendMessage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.parametersOf
import io.ktor.serialization.kotlinx.json.json as ktorJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.thauvin.erik.urlencoder.UrlEncoderUtil

class PrometheusMessagesApi(
    val tenant: String,
    val login: String? = null,
    val password: String? = null,
    private var initialCookies: List<Cookie>? = null
) {
    private val ssoBaseUrl = "https://dziennik-logowanie.vulcan.net.pl"
    private val messagesBaseUrl = "https://wiadomosci.eduvulcan.pl"

    private var antiForgeryToken: String = ""
    private var appGuid: String = ""
    private var isInitialized: Boolean = false

    private val json = Json { ignoreUnknownKeys = true }
    private val cookieStorage = AcceptAllCookiesStorage()

    private val httpClient = HttpClient {
        followRedirects = false
        expectSuccess = false

        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }
        install(HttpCookies) {
            storage = cookieStorage
        }
        install(ContentNegotiation) {
            ktorJson(json)
        }
        install(UserAgent) {
            agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
        }
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
            }
        }
    }

    suspend fun initialize() {
        if (isInitialized) return

        var currentCookies = initialCookies
        if (currentCookies == null) {
            if (login != null && password != null) {
                val helper = PrometheusLoginHelper()
                val result = helper.login(login, password, "Szpontium")
                currentCookies = result.cookies
            } else {
                throw IllegalStateException("Brak ciasteczek i brak danych logowania")
            }
        }

        require(tenant.matches(Regex("[a-zA-Z0-9_-]+")))
        // Restore each cookie only within its original domain.
        currentCookies.forEach { cookie ->
            val domain = cookie.domain?.removePrefix(".") ?: "eduvulcan.pl"
            require(domain == "eduvulcan.pl" || domain.endsWith(".eduvulcan.pl") || domain == "dziennik-logowanie.vulcan.net.pl")
            cookieStorage.addCookie(Url("https://$domain"), cookie)

        }

        val prometheusEncoded = UrlEncoderUtil.encode("https://eduvulcan.pl")
        val ssoEncoded = UrlEncoderUtil.encode(ssoBaseUrl)
        val studentEncoded = UrlEncoderUtil.encode("https://uczen.eduvulcan.pl")

        // First SSO flow to authenticate the client in the SSO domain
        val firstAuthUrl = "https://eduvulcan.pl/fs/ls?wa=wsignin1.0&wtrealm=$ssoEncoded%2F${tenant}%2FFs%2FLs%3Fwa%3Dwsignin1.0%26wtrealm%3D$studentEncoded%2F${tenant}%2FAccount%2FLogin%3FreturnUrl%3D$prometheusEncoded%26wctx%3Dauth%3DstudentEV%26nslo%3D1&wctx=nslo%3D1"
        authorizePrometheus(firstAuthUrl)

        // Second SSO flow to authenticate the client in the messages domain
        val authorizeUrl = "$ssoBaseUrl/$tenant/Fs/Ls?wa=wsignin1.0&wtrealm=$messagesBaseUrl/$tenant/Account/Login?returnUrl=/$tenant/App&wctx=auth=studentEV&nslo=1"
        authorizePrometheus(authorizeUrl)

        // Fetch tokens from App
        val appScript = Ksoup.parse(ssoRequest(httpClient,"$messagesBaseUrl/$tenant/App").bodyAsText())
            .select("script").joinToString("\n") { it.html() }

        antiForgeryToken = messagesScriptValue(appScript,"antiForgeryToken")
        appGuid = messagesScriptValue(appScript,"appGuid")
        if(antiForgeryToken.isBlank() || appGuid.isBlank()) throw MessagesAccessException("SSO_APP_TOKENS")
        isInitialized = true
    }

    private suspend fun authorizePrometheus(url: String) {
        var response=ssoRequest(httpClient,url)
        repeat(8) {
            val document=Ksoup.parse(response.bodyAsText(),response.call.request.url.toString())
            val form=document.forms().firstOrNull() ?: return
            if(form.select("input[type=password]").isNotEmpty()) throw MessagesAccessException("SSO_LOGIN_REQUIRED")
            val action=form.absUrl("action").ifBlank { return }
            val fields=form.select("input[type=hidden]").filter { it.attr("name").isNotBlank() }
                .groupBy({ it.attr("name") },{ it.value() })
            if(fields.isEmpty()) throw MessagesAccessException("SSO_FORM")
            response=ssoRequest(httpClient,action,parametersOf(fields))
        }
        throw MessagesAccessException("SSO_FORM_LIMIT")
    }

    fun close() { httpClient.close() }

    suspend fun readPayload(endpoint:String,query:Map<String,String> = emptyMap()):kotlinx.serialization.json.JsonElement {
        require(endpoint in setOf("Skrzynki","OdebraneSkrzynka","WyslaneSkrzynka","WiadomoscSzczegoly","Pracownicy","WiadomoscOdpowiedzPrzekaz"))
        initialize()
        val response=httpClient.get("$messagesBaseUrl/$tenant/api/$endpoint") {
            query.forEach { (name,value) -> parameter(name,value) }
            header("X-V-AppGuid", appGuid)
            header("X-V-RequestVerificationToken", antiForgeryToken)
            contentType(ContentType.Application.Json)
        }
        if(response.status.value !in 200..299) {
            isInitialized=false
            throw MessagesAccessException("API_"+endpoint+"_HTTP_"+response.status.value)
        }
        val body=response.bodyAsText()
        if(body.trimStart().startsWith("<")) { isInitialized=false;throw MessagesAccessException("API_HTML_"+endpoint) }
        return json.parseToJsonElement(body)
    }

    suspend fun getMailboxes(): List<PrometheusMailbox> {
        initialize()
        val response = httpClient.get("$messagesBaseUrl/$tenant/api/Skrzynki") {
            header("X-V-AppGuid", appGuid)
            header("X-V-RequestVerificationToken", antiForgeryToken)
            contentType(ContentType.Application.Json)
        }
        return response.body()
    }

    suspend fun getReceivedMessages(mailboxKey: String, pageSize: Int = 50, lastMessageId: Int = 0): List<PrometheusMessage> {
        return fetchMessages("/api/OdebraneSkrzynka", mailboxKey, pageSize, lastMessageId)
    }

    suspend fun getSentMessages(mailboxKey: String, pageSize: Int = 50, lastMessageId: Int = 0): List<PrometheusMessage> {
        return fetchMessages("/api/WyslaneSkrzynka", mailboxKey, pageSize, lastMessageId)
    }

    suspend fun getDeletedMessages(mailboxKey: String, pageSize: Int = 50, lastMessageId: Int = 0): List<PrometheusMessage> {
        return fetchMessages("/api/UsunieteSkrzynka", mailboxKey, pageSize, lastMessageId)
    }

    private suspend fun fetchMessages(endpoint: String, mailboxKey: String, pageSize: Int, lastMessageId: Int): List<PrometheusMessage> {
        initialize()
        val response = httpClient.get("$messagesBaseUrl/$tenant$endpoint") {
            parameter("globalKeySkrzynka", mailboxKey)
            parameter("idLastWiadomosc", lastMessageId)
            parameter("pageSize", pageSize)
            header("X-V-AppGuid", appGuid)
            header("X-V-RequestVerificationToken", antiForgeryToken)
            contentType(ContentType.Application.Json)
        }
        return response.body()
    }

    suspend fun getMessageDetails(apiGlobalKey: String): PrometheusMessageDetails {
        initialize()
        val response = httpClient.get("$messagesBaseUrl/$tenant/api/WiadomoscSzczegoly") {
            parameter("apiGlobalKey", apiGlobalKey)
            header("X-V-AppGuid", appGuid)
            header("X-V-RequestVerificationToken", antiForgeryToken)
            contentType(ContentType.Application.Json)
        }
        return response.body()
    }

    suspend fun markMessageAsRead(apiGlobalKey: String) {
        initialize()
        httpClient.put("$messagesBaseUrl/$tenant/api/WiadomoscSzczegoly") {
            header("X-V-AppGuid", appGuid)
            header("X-V-RequestVerificationToken", antiForgeryToken)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("apiGlobalKey", apiGlobalKey)
            })
        }
    }

    suspend fun sendMessage(message: PrometheusSendMessage) {
        initialize()
        val response=httpClient.post("$messagesBaseUrl/$tenant/api/WiadomoscNowa") {
            header("X-V-AppGuid", appGuid)
            header("X-V-RequestVerificationToken", antiForgeryToken)
            contentType(ContentType.Application.Json)
            setBody(message)
        }
        validateSendResponse(response.status.value,response.bodyAsText(),message.globalKey)
    }

    suspend fun replyForwardMessage(message: PrometheusSendMessage) {
        initialize()
        httpClient.post("$messagesBaseUrl/$tenant/api/WiadomoscOdpowiedzPrzekaz") {
            header("X-V-AppGuid", appGuid)
            header("X-V-RequestVerificationToken", antiForgeryToken)
            contentType(ContentType.Application.Json)
            setBody(message)
        }
    }
}
