package pl.buda.ios
import kotlin.test.*
class LocalDataTest {
 @Test fun scopesCannotCollideOnSeparators() { assertNotEquals(scopeKey(listOf("a:b","c")),scopeKey(listOf("a","b:c"))) }
 @Test fun siblingAndSemesterCachesStaySeparateAfterRestart() {
  val vault=MemoryVault();val store=LocalDataStore(vault)
  val a=scopeKey(listOf("child-a","1"));val b=scopeKey(listOf("child-b","1"));val c=scopeKey(listOf("child-a","2"))
  store.saveScreen(a,SavedScreen("2026-11-04",rows=listOf(DiaryRow("a","Math","test"))))
  store.saveScreen(b,SavedScreen("2026-11-04",rows=listOf(DiaryRow("b","History","test"))))
  val restored=LocalDataStore(vault)
  assertEquals("Math",restored.screen(a)!!.rows.single().title);assertEquals("History",restored.screen(b)!!.rows.single().title);assertNull(restored.screen(c))
 }
 @Test fun unknownSendOutcomeSurvivesRestartAndBlocksSending() {
  val vault=MemoryVault();val store=LocalDataStore(vault);val recipient=MailRecipient("key","Teacher")
  val draft=MailDraft("id","thread","Subject","Text",listOf(recipient),true)
  assertTrue(store.saveDraft("mailbox1",draft));val restored=LocalDataStore(vault).draft("mailbox1")!!
  assertTrue(restored.uncertain);assertFalse(canSendDraft(restored,listOf(recipient)));assertNull(store.draft("mailbox2"))
 }
 @Test fun clearingDeletesCacheAndDraftsFromPersistentVault() {
  val vault=MemoryVault();val store=LocalDataStore(vault);store.saveScreen("a",SavedScreen("now"));store.saveDraft("a",MailDraft("id","thread"))
  assertTrue(store.clear());assertNull(vault.read());assertNull(LocalDataStore(vault).screen("a"));assertNull(store.draft("a"))
 }
 @Test fun memoryOnlyModeDoesNotPersist() { val vault=MemoryVault();val store=LocalDataStore(vault);store.persistent=false;assertTrue(store.saveScreen("a",SavedScreen("now")));assertNotNull(store.screen("a"));assertNull(vault.read()) }
 @Test fun unavailableVaultIsNotSilentlyOverwritten() { val vault=MemoryVault();vault.write("KEYCHAIN_UNAVAILABLE");val store=LocalDataStore(vault);assertFalse(store.available);assertFalse(store.saveDraft("a",MailDraft("id","thread")));assertEquals("KEYCHAIN_UNAVAILABLE",vault.read()) }
 @Test fun failedWriteDoesNotClaimCacheWasSaved() {
  val vault=object:SessionVault { override fun read():String?=null;override fun write(value:String)=false;override fun clear()=false }
  val store=LocalDataStore(vault);assertFalse(store.saveScreen("a",SavedScreen("now")));assertNull(store.screen("a"));assertFalse(store.clear())
 }
 @Test fun evictionKeepsDraftAndNewestScreens() {
  val store=LocalDataStore(MemoryVault());store.saveDraft("draft",MailDraft("id","thread","hello"))
  repeat(25) { store.saveScreen("$it",SavedScreen(it.toString().padStart(3,'0'))) }
  assertNull(store.screen("0"));assertNotNull(store.screen("24"));assertEquals("hello",store.draft("draft")!!.title)
 }
 @Test fun oversizeScreenDoesNotEvictExistingData() {
  val store=LocalDataStore(MemoryVault());store.saveScreen("old",SavedScreen("now"))
  assertFalse(store.saveScreen("huge",SavedScreen("now",rows=listOf(DiaryRow("a","x","x".repeat(650000))))));assertNotNull(store.screen("old"))
 }
}
