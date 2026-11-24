package pl.buda.ios
import io.ktor.http.*
import kotlin.test.*
class CookiePersistenceTest {
 @Test fun serverCookieRestorationPreservesWireValue() {
  val original=parseServerSetCookieHeader("session=a%2Bb/c==; Path=/; Secure; HttpOnly")
  val restored=StoredCookie.from(original).restore()
  assertEquals(renderCookieHeader(original),renderCookieHeader(restored))
  assertEquals(CookieEncoding.RAW,restored.encoding)
 }
 @Test fun oldSavedServerCookieDefaultsToRaw() {
  val old=StoredCookie("session","a%2Bb/c==","eduvulcan.pl","/",true,true)
  assertEquals("session=a%2Bb/c==",renderCookieHeader(old.restore()))
 }
 @Test fun explicitEncodingSurvivesStorage() {
  val original=Cookie("session","a+b/c",encoding=CookieEncoding.URI_ENCODING)
  assertEquals(renderCookieHeader(original),renderCookieHeader(StoredCookie.from(original).restore()))
 }
}
