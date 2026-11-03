package io.github.szpontium.api.prometheus

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*

class MessagesAccessException(val code:String):Exception(code)
internal fun trustedMessagesUrl(value:String):Url {
 val url=try { Url(value) } catch(e:Exception) { throw MessagesAccessException("SSO_URL") }
 if(url.protocol!=URLProtocol.HTTPS || url.port!=443 || !url.user.isNullOrEmpty() || !url.password.isNullOrEmpty() ||
  url.host !in setOf("eduvulcan.pl","uczen.eduvulcan.pl","wiadomosci.eduvulcan.pl","dziennik-logowanie.vulcan.net.pl")) throw MessagesAccessException("SSO_HOST")
 return url
}
/** Browser-style POST/redirect/GET, with bounded redirects and exact HTTPS host validation. */
internal suspend fun ssoRequest(client:HttpClient,start:String,fields:Parameters?=null):HttpResponse {
 var url=trustedMessagesUrl(start).toString()
 var form=fields
 repeat(12) {
  val response=if(form==null) client.get(url) else client.submitForm(url,form)
  val status=response.status.value
  if(status in 200..299) return response
  if(status !in listOf(301,302,303,307,308)) throw MessagesAccessException("SSO_HTTP_$status")
  val location=response.headers[HttpHeaders.Location] ?: throw MessagesAccessException("SSO_LOCATION")
  url=trustedMessagesUrl(URLBuilder(response.call.request.url).takeFrom(location).buildString()).toString()
  // 307/308 retain the form; 301/302/303 follow browser navigation as GET.
  if(status in listOf(301,302,303)) form=null
 }
 throw MessagesAccessException("SSO_REDIRECT_LIMIT")
}
internal fun messagesScriptValue(script:String,name:String):String =
 Regex("[\"']?"+Regex.escape(name)+"[\"']?\\s*:\\s*[\"']([^\"']+)[\"']").find(script)?.groupValues?.get(1).orEmpty()