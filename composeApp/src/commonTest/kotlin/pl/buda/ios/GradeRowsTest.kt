package pl.buda.ios
import kotlin.test.*
import kotlinx.serialization.json.Json
class GradeRowsTest {
 @Test fun acceptsMissingUnusedMetadataAndNullComment() {
  val row=parseGrades(Json.parseToJsonElement("""[{"Id":1,"PupilId":42,"Content":"5","Comment":null,"Column":{"Subject":{"Name":"Matematyka"}},"Creator":null,"Weight":"other"}]"""),42).single()
  assertEquals("5",row.content);assertEquals("",row.comment);assertEquals("Matematyka",row.subject)
 }
 @Test fun acceptsStringIdsAndNumericContent() {
  val row=parseGrades(Json.parseToJsonElement("""[{"Id":"2","PupilId":"43","Content":4,"Column":null}]"""),43).single()
  assertEquals(2,row.id);assertEquals("4",row.content)
 }
 @Test fun neverDisplaysAnotherPupilsGrades() {
  assertFailsWith<GradeFormatException> { parseGrades(Json.parseToJsonElement("""[{"Id":1,"PupilId":42,"Content":"5"}]"""),43) }
 }
 @Test fun malformedPayloadIsNotAnEmptyGradeList() {
  assertFailsWith<GradeFormatException> { parseGrades(Json.parseToJsonElement("{}"),42) }
 }
 @Test fun missingGradeValueIsReported() {
  assertFailsWith<GradeFormatException> { parseGrades(Json.parseToJsonElement("""[{"Id":1}]"""),42) }
 }
 @Test fun diagnosticsContainNoServerBody() {
  val message=safeError(kotlinx.serialization.SerializationException("SECRET pupil password"),"oceny")
  assertFalse(message.contains("SECRET"));assertTrue(message.contains("oceny"))
 }
}
