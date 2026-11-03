package io.github.szpontium.api.prometheus
import io.ktor.client.*
import io.ktor.client.request.forms.submitForm
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MessagesSsoTest {
 @Test fun oldConfigurationReproducesReportedPostRedirectException()=runTest {
  val client=HttpClient(MockEngine { respond("",HttpStatusCode.Found,headersOf(HttpHeaders.Location,"/App")) }) { followRedirects=true;expectSuccess=true }
  try {
   assertFailsWith<io.ktor.client.plugins.RedirectResponseException> {
    client.submitForm("https://wiadomosci.eduvulcan.pl/Login",parametersOf("token","test"))
   }
  } finally { client.close() }
 }

 @Test fun post302FollowsAsGetAndRetainsSessionCookie()=runTest {
  var count=0
  val engine=MockEngine { request ->
   count++
   if(count==1) {
    assertEquals(HttpMethod.Post,request.method)
    respond("",HttpStatusCode.Found,headersOf(HttpHeaders.Location to listOf("/tenant/App"),HttpHeaders.SetCookie to listOf("session=test; Path=/; Secure; HttpOnly")))
   } else {
    assertEquals(HttpMethod.Get,request.method);assertEquals("/tenant/App",request.url.encodedPath)
    assertTrue(request.headers[HttpHeaders.Cookie].orEmpty().contains("session=test"))
    respond("OK")
   }
  }
  val client=HttpClient(engine) { followRedirects=false;expectSuccess=false;install(HttpCookies) }
  try { assertEquals(HttpStatusCode.OK,ssoRequest(client,"https://wiadomosci.eduvulcan.pl/tenant/Login",parametersOf("token","test")).status);assertEquals(2,count) } finally { client.close() }
 }
 @Test fun redirect307PreservesPost()=runTest {
  var count=0
  val client=HttpClient(MockEngine { request ->
   count++;assertEquals(HttpMethod.Post,request.method)
   if(count==1) respond("",HttpStatusCode.TemporaryRedirect,headersOf(HttpHeaders.Location,"/next")) else respond("OK")
  }) { followRedirects=false;expectSuccess=false }
  try { ssoRequest(client,"https://eduvulcan.pl/start",parametersOf("token","test"));assertEquals(2,count) } finally { client.close() }
 }
 @Test fun redirectToOtherHostIsRejectedBeforeRequest()=runTest {
  var count=0
  val client=HttpClient(MockEngine { count++;respond("",HttpStatusCode.Found,headersOf(HttpHeaders.Location,"https://example.com/steal")) }) { followRedirects=false;expectSuccess=false }
  try { assertEquals("SSO_HOST",assertFailsWith<MessagesAccessException> { ssoRequest(client,"https://eduvulcan.pl/start") }.code);assertEquals(1,count) } finally { client.close() }
 }
 @Test fun redirectLoopStops()=runTest {
  var count=0
  val client=HttpClient(MockEngine { count++;respond("",HttpStatusCode.Found,headersOf(HttpHeaders.Location,"/start")) }) { followRedirects=false;expectSuccess=false }
  try { assertEquals("SSO_REDIRECT_LIMIT",assertFailsWith<MessagesAccessException> { ssoRequest(client,"https://eduvulcan.pl/start") }.code);assertEquals(12,count) } finally { client.close() }
 }
 @Test fun serverErrorHasSafeDiagnosticCode()=runTest {
  val client=HttpClient(MockEngine { respond("SECRET",HttpStatusCode.Forbidden) }) { followRedirects=false;expectSuccess=false }
  try { val e=assertFailsWith<MessagesAccessException> { ssoRequest(client,"https://eduvulcan.pl/start?token=SECRET") };assertEquals("SSO_HTTP_403",e.code);assertFalse(e.toString().contains("SECRET")) } finally { client.close() }
 }
 @Test fun invalidDestinationIsRejected() {
  listOf("http://eduvulcan.pl/","https://eduvulcan.pl:444/","https://user:password@eduvulcan.pl/","https://eduvulcan.pl.example.com/").forEach { assertFailsWith<MessagesAccessException> { trustedMessagesUrl(it) } }
 }
 @Test fun tokensAcceptQuotedKeysWhitespaceAndBothQuoteStyles() {
  assertEquals("abc",messagesScriptValue("{\"antiForgeryToken\" : \"abc\"}","antiForgeryToken"))
  assertEquals("guid",messagesScriptValue("appGuid: 'guid'","appGuid"))
 }
}