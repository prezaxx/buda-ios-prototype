package pl.buda.ios
import kotlin.test.*
class MailboxSelectionTest {
 private val boxes=listOf(Mailbox("ABC","Parent - First"),Mailbox("DEF","Parent - Second"))
 @Test fun eachChildUsesItsAccountKey() {
  assertEquals("ABC",resolveMailbox(boxes,null,"abc",null));assertEquals("DEF",resolveMailbox(boxes,null,"{DEF}",null))
 }
 @Test fun explicitChoiceOverridesMetadata() { assertEquals("DEF",resolveMailbox(boxes,"DEF","ABC","Parent - First")) }
 @Test fun removedChoiceFallsBackToCurrentAccount() { assertEquals("ABC",resolveMailbox(boxes,"deleted","ABC",null)) }
 @Test fun exactMailboxNameCanBridgeDifferentApiKeys() { assertEquals("DEF",resolveMailbox(boxes,null,"other","  Parent   - Second ")) }
 @Test fun ambiguityNeverSelectsFirstMailbox() {
  assertNull(resolveMailbox(listOf(Mailbox("a","Same"),Mailbox("b","Same")),null,null,"Same"))
  assertNull(resolveMailbox(boxes,null,null,"Parent"));assertNull(resolveMailbox(boxes,null,null,null))
 }
 @Test fun uniqueMailboxWithoutAssociationStillRequiresChoice() { assertNull(resolveMailbox(boxes.take(1),null,null,null)) }
 @Test fun missingMailboxesNeverReviveStaleSelection() { assertNull(resolveMailbox(emptyList(),"ABC","ABC","Parent - First")) }
}