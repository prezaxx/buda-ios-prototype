package pl.buda.ios
import io.github.szpontium.api.hebe.models.Account
import kotlinx.serialization.json.*
import kotlin.test.*

class DiaryTest {
 private fun row(text:String)=Json.parseToJsonElement(text).jsonObject
 @Test fun substitutionUsesNewSubjectAndRoom() {
  val value=mapDiaryRow(Section.SCHEDULE,row("""{"Id":1,"DateAt":"2026-11-02","Subject":{"Name":"Math"},"Room":{"Code":"1"},"TimeSlot":{"Start":"08:00","End":"08:45"},"Substitution":{"Subject":{"Name":"Art"},"Room":{"Code":"2"},"DateAt":"2026-11-03","Change":{"Type":1}}}"""),"schedule")
  assertEquals("2",value.lesson?.room);assertEquals("08:00",value.lesson?.start);assertEquals("Art",value.title);assertEquals("2026-11-03",value.date);assertTrue(value.detail.contains("2"));assertTrue(value.badge.isNotBlank())
 }
 @Test fun cancelledLessonRemainsVisible() {
  val value=mapDiaryRow(Section.SCHEDULE,row("""{"Id":1,"Substitution":{"ClassAbsence":true}}"""),"schedule")
  assertEquals("Odwołane",value.badge);assertEquals(true,value.lesson?.cancelled)
 }
 @Test fun dangerousAttachmentProtocolsAreNotOpened() {
  val value=mapDiaryRow(Section.HOMEWORK,row("""{"Id":1,"Content":"<b>Read</b>","Attachments":[{"Name":"bad","Link":"javascript:alert(1)"},{"Name":"good","Link":"https://example.com/a"}]}"""),"homework")
  assertTrue(value.detail.contains("Read"));assertFalse(value.detail.contains("<b>"));assertEquals(1,value.links.size)
 }
 @Test fun summaryDoesNotInventAverageOrFinal() {
  val average=mapDiaryRow(Section.SUMMARY,row("""{"Id":1,"Subject":{"Name":"Math"},"Average":null}"""),"average")
  assertTrue(average.detail.contains("nie podano"))
  val summary=mapDiaryRow(Section.SUMMARY,row("""{"Id":1,"Entry_1":"4","Entry_2":"5"}"""),"summary")
  assertTrue(summary.detail.contains("Przewidywana: 4"));assertTrue(summary.detail.contains("Końcowa: 5"))
 }
 @Test fun nullPresenceTypeIsNotClaimedPresent() {
  val value=mapDiaryRow(Section.PRESENCE,row("""{"Id":1,"DayAt":"2026-11-02","PresenceType":null}"""),"presence")
  assertEquals("Nieopisany wpis frekwencji",value.title)
 }
 @Test fun collidingIdsAcrossEndpointsStayDistinct() {
  val data=row("""{"Id":1}""")
  assertNotEquals(mapDiaryRow(Section.SCHEDULE,data,"schedule").key,mapDiaryRow(Section.SCHEDULE,data,"extra").key)
 }
}
class SessionTest {
 private fun fixture():StoredSession {
  val account=sessionJson.decodeFromString(Account.serializer(),"""{
   "Links":{"Root":"","Group":"","Symbol":"","QuestionnaireRoot":"","ExResourcesUrl":""},
   "Unit":{"Id":2,"Symbol":"s","Short":"S","RestURL":"https://example.com/api","Name":"School","DisplayName":"School","SchoolTopic":""},
   "ConstituentUnit":{"Id":2,"Short":"S","Name":"School","SchoolTopic":""},
   "Pupil":{"Id":7,"LoginId":3,"FirstName":"Test","SecondName":"","Surname":"Student","Sex":false},
   "Periods":[]
  }""")
  return StoredSession(students=listOf(StoredStudent(account,StoredCredential("RSA_PEM","https://example.com/api","PUBLIC","PRIVATE","FP","DEVICE","iOS","iPhone"))))
 }
 @Test fun sessionRoundTripPreservesPupilAndKeyButHasNoPassword() {
  val encoded=encodeSession(fixture());val restored=decodeSession(encoded)
  assertEquals(7,restored.students.single().account.pupil.id)
  assertEquals("PRIVATE",restored.students.single().credential.restore().privateKey)
  assertFalse(encoded.contains("password",true))
 }
 @Test fun mailboxChoicesRemainSeparateAfterSessionRestore() {
  val first=fixture().students.single().copy(mailboxKey="first-box")
  val second=first.copy(account=first.account.copy(pupil=first.account.pupil.copy(id=8)),mailboxKey="second-box")
  val stored=decodeSession(encodeSession(StoredSession(students=listOf(first,second))))
  assertEquals("first-box",stored.students[0].mailboxKey);assertEquals("second-box",stored.students[1].mailboxKey)
 }
 @Test fun oldSessionHasNoForcedMailbox() { assertNull(decodeSession(encodeSession(fixture())).students.single().mailboxKey) }
 @Test fun invalidSelectedPupilIsRejected() {
  assertFailsWith<IllegalArgumentException> { decodeSession(encodeSession(fixture().copy(selected=8))) }
 }
 @Test fun futureSchemaIsNotSilentlyUsed() {
  assertFailsWith<IllegalArgumentException> { decodeSession(encodeSession(fixture().copy(version=100))) }
 }
 @Test fun clearRemovesSavedSession() {
  val vault=MemoryVault();vault.write(encodeSession(fixture()));assertNotNull(vault.read());assertTrue(vault.clear());assertNull(vault.read())
 }
}