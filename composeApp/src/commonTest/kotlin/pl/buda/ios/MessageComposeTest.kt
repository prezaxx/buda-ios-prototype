package pl.buda.ios
import kotlin.test.*
import kotlinx.serialization.json.Json
import io.github.szpontium.api.prometheus.validateSendResponse
import io.github.szpontium.api.prometheus.MessagesAccessException
class MessageComposeTest {
 @Test fun recipientIdsComeFromDirectoryNotDisplayNames() {
  val rows=parseRecipients(Json.parseToJsonElement("""[{"skrzynkaGlobalKey":"a","nazwa":"Same"},{"skrzynkaGlobalKey":"b","nazwa":"Same"}]"""))
  assertEquals(2,rows.size);assertNotEquals(rows[0].key,rows[1].key)
 }
 @Test fun malformedDirectoryCannotCreateEmptyAddress() { assertFailsWith<MessageFormatException> { parseRecipients(Json.parseToJsonElement("""[{"nazwa":"Teacher"}]""")) } }
 @Test fun replyMustBelongToSelectedMailboxAndMessage() {
  val body=Json.parseToJsonElement("""{"apiGlobalKey":"msg","uzytkownikSkrzynkaGlobalKey":"box","nadawcaSkrzynkaGlobalKey":"teacher","nadawcaSkrzynkaNazwa":"Name","temat":"Topic"}""")
  assertEquals("teacher",parseReplyRecipient(body,"msg","box").second.key)
  assertFailsWith<MessageFormatException> { parseReplyRecipient(body,"other","box") };assertFailsWith<MessageFormatException> { parseReplyRecipient(body,"msg","sibling") }
 }
 @Test fun sendingRequiresCurrentlyVerifiedRecipientsAndContent() {
  val recipient=MailRecipient("a","Name");val draft=MailDraft("id","thread","Subject","Text",listOf(recipient))
  assertTrue(canSendDraft(draft,listOf(recipient)));assertFalse(canSendDraft(draft,emptyList()));assertFalse(canSendDraft(draft.copy(text=" "),listOf(recipient)));assertFalse(canSendDraft(draft.copy(uncertain=true),listOf(recipient)))
 }
 @Test fun loginHtmlAndHttpFailuresNeverCountAsSent() {
  assertFailsWith<MessagesAccessException> { validateSendResponse(302,"","id") };assertFailsWith<MessagesAccessException> { validateSendResponse(403,"","id") };assertFailsWith<MessagesAccessException> { validateSendResponse(200,"<html>login</html>","id") }
 }
 @Test fun explicitServerFailureWinsOverMessageId() { assertFailsWith<MessagesAccessException> { validateSendResponse(200,"""{"success":false,"apiGlobalKey":"id"}""","id") };assertFailsWith<MessagesAccessException> { validateSendResponse(200,"""{"error":"rejected"}""","id") } }
 @Test fun unknownBodyDoesNotClaimSuccess() { assertFailsWith<MessagesAccessException> { validateSendResponse(200,"{}","id") };assertFailsWith<MessagesAccessException> { validateSendResponse(200,"[]","id") } }
 @Test fun recognizedAcknowledgementsPass() { validateSendResponse(204,"","id");validateSendResponse(200,"null","id");validateSendResponse(200,"true","id");validateSendResponse(200,"""{"success":true}""","id") }
}
