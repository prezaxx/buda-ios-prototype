package pl.buda.ios
import io.ktor.http.*
import io.ktor.util.date.GMTDate
import kotlinx.serialization.json.*
import kotlin.test.*

class MessagesTest {
 private fun json(value:String)=Json.parseToJsonElement(value)
 @Test fun inboxAcceptsNumericStringsAndMissingOptionalFields() {
  val mail=parseInbox(json("""[{"apiGlobalKey":"a","id":"14","data":"2026-11-02","temat":"<b>Hello</b>","korespondenci":null,"extra":1}]""")).single()
  assertEquals(14,mail.id);assertEquals("Hello",mail.title);assertEquals("",mail.sender);assertFalse(mail.read)
 }
 @Test fun loginResponseIsNotAnEmptyInbox() {
  assertFailsWith<MessageFormatException> { parseInbox(json("""{"error":"expired"}""")) }
  assertFailsWith<MessageFormatException> { parseInbox(json("""[{"id":1}]""")) }
 }
 @Test fun detailsMustMatchRequestedMessage() {
  assertFailsWith<MessageFormatException> { parseMailDetail(json("""{"apiGlobalKey":"other"}"""),"expected") }
 }
 @Test fun messageHtmlIsReadableWithoutActiveContent() {
  val text=messageText("<p>First</p><p>Second<br>Third</p><script>SECRET_SCRIPT</script><style>SECRET_CSS</style>")
  assertTrue(text.contains("First"));assertTrue(text.contains("\n"));assertTrue(text.contains("Third"));assertFalse(text.contains("SECRET"));assertFalse(text.contains("<p>"))
 }
 @Test fun attachmentsWithoutLinksRemainVisible() {
  val detail=parseMailDetail(json("""{"apiGlobalKey":"a","zalaczniki":[{"idZalacznik":1,"nazwaPliku":"file.pdf"},{"nazwaPliku":"second.pdf","url":"/files/2"}]}"""),"a")
  assertEquals(2,detail.attachments.size);assertNull(detail.attachments[0].url)
  assertEquals("https://wiadomosci.eduvulcan.pl/files/2",detail.attachments[1].url)
 }
 @Test fun attachmentLinksRejectUnsafeSchemesAndCredentials() {
  listOf("javascript:alert(1)","file:///etc/passwd","http://example.com","https://user:pass@example.com/a","").forEach { assertNull(attachmentUrl(it),it) }
  assertEquals("https://example.com/a",attachmentUrl("https://example.com/a"))
 }
 @Test fun paginationDeduplicatesAndRejectsRepeatedPages() {
  fun item(key:String,id:Int)=MailItem(key,id,"","","",false,false)
  val old=listOf(item("a",20),item("b",19))
  val merged=mergeInbox(old,listOf(item("b",19),item("c",18)),true)
  assertEquals(listOf("a","b","c"),merged.map { it.key })
  assertFailsWith<MessageFormatException> { mergeInbox(old,old,true) }
  assertEquals(old,mergeInbox(old,emptyList(),true))
  assertTrue(mergeInbox(old,emptyList(),false).isEmpty())
 }
 @Test fun cookiesRoundTripPreservesDomainAndExpiry() {
  val original=Cookie(name="session",value="test",domain=".eduvulcan.pl",path="/",secure=true,httpOnly=true,expires=GMTDate(1900000000000))
  val stored=StoredCookie.from(original)
  val restored=sessionJson.decodeFromString(StoredCookie.serializer(),sessionJson.encodeToString(StoredCookie.serializer(),stored)).restore()
  assertEquals(original.name,restored.name);assertEquals(original.value,restored.value);assertEquals(original.domain,restored.domain)
  assertEquals(original.expires,restored.expires);assertTrue(restored.secure);assertTrue(restored.httpOnly)
 }
 @Test fun mailboxNamesDoNotDetermineIdentity() {
  val boxes=parseMailboxes(json("""[{"globalKey":"a","nazwa":"Same name"},{"globalKey":"b","nazwa":"Same name"}]"""))
  assertEquals(2,boxes.size);assertNotEquals(boxes[0].key,boxes[1].key)
 }
}