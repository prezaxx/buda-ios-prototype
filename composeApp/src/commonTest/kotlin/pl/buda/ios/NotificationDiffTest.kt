package pl.buda.ios
import kotlinx.serialization.json.*
import kotlin.test.*
class NotificationDiffTest {
 private fun grade(value:String)=NotificationGrade(value,"Math","Test","")
 @Test fun firstSnapshotDoesNotNotifyOldGrades() {
  val (next,count)=mergeGradeBaseline(GradeBaseline(),listOf(GradeObservation("A",mapOf("1" to grade("5")))))
  assertEquals(0,count);assertEquals(1,next.accounts["A"]!!.size)
 }
 @Test fun newAndChangedGradesAreDetectedOnlyOnce() {
  val old=GradeBaseline(mapOf("A" to mapOf("1" to grade("4"))))
  val observations=listOf(GradeObservation("A",mapOf("1" to grade("5"),"2" to grade("6"))))
  val (next,count)=mergeGradeBaseline(old,observations);assertEquals(2,count)
  assertEquals(0,mergeGradeBaseline(next,observations).second)
 }
 @Test fun siblingsAndSemestersDoNotShareBaselines() {
  val old=GradeBaseline(mapOf("first:semester1" to mapOf("1" to grade("4"))))
  val (next,count)=mergeGradeBaseline(old,listOf(GradeObservation("second:semester1",mapOf("1" to grade("1"))),GradeObservation("first:semester2",mapOf("1" to grade("6")))))
  assertEquals(0,count);assertEquals(3,next.accounts.size)
 }
 @Test fun emptyOrFailedResponsesDoNotReplayHistory() {
  val old=GradeBaseline(mapOf("A" to mapOf("1" to grade("4")),"B" to mapOf("1" to grade("6"))))
  val (next,count)=mergeGradeBaseline(old,listOf(GradeObservation("A",emptyMap())))
  assertEquals(old,next);assertEquals(0,count)
  assertEquals(0,mergeGradeBaseline(next,listOf(GradeObservation("A",mapOf("1" to grade("4"))))).second)
 }
 @Test fun baselineRoundTripPreservesTextGrades() {
  val old=GradeBaseline(mapOf("A" to mapOf("1" to grade("np"))))
  assertEquals(old,sessionJson.decodeFromString(GradeBaseline.serializer(),sessionJson.encodeToString(GradeBaseline.serializer(),old)))
 }
 @Test fun teacherColorIsOptionalAndNeverBreaksGrades() {
  val payload=Json.parseToJsonElement("""[{"Id":1,"Content":"1","Column":{"Color":65280}},{"Id":2,"Content":"6","Column":{"Color":"bad"}}]""")
  val grades=parseGrades(payload,42);assertEquals(65280,grades[0].teacherRgb);assertNull(grades[1].teacherRgb)
 }
 @Test fun teacherColorSupportsBlackDecimalAndHexWithoutGuessing() {
  assertEquals(0,parseTeacherColor(JsonPrimitive(0)))
  assertEquals(0xF04C4C,parseTeacherColor(JsonPrimitive("#F04C4C")))
  assertEquals(0xFF0000,parseTeacherColor(JsonPrimitive(-65536)))
  assertNull(parseTeacherColor(JsonObject(emptyMap())))
  assertNull(parseTeacherColor(JsonPrimitive(999999999999)))
 }
 @Test fun teacherColorOverridesNumericScaleInBothThemes() {
  for(dark in listOf(false,true)) {
   val teacher=gradePalette("1",dark,0x00FF00)
   assertEquals(androidx.compose.ui.graphics.Color(0xFF00FF00),teacher.background)
   assertEquals(teacher,gradePalette("6",dark,0x00FF00))
  }
 }
 @Test fun historyNamesTheSiblingAndExplainsCommentOnlyChange() {
  val old=GradeBaseline(mapOf("B" to mapOf("1" to grade("4"))))
  val changed=grade("4").copy(comment="Poprawa")
  val (next,count)=mergeGradeBaseline(old,listOf(GradeObservation("B",mapOf("1" to changed),"Drugie dziecko","Semestr 1")),"2026-11-04T10:00:00Z")
  assertEquals(1,count);val event=next.history.single();assertEquals("Drugie dziecko",event.student);assertEquals("4",event.before!!.content);assertEquals("Poprawa",event.after.comment)
 }
 @Test fun whitespaceOnlyChangesDoNotNotify() {
  val old=GradeBaseline(mapOf("A" to mapOf("1" to grade("4"))))
  assertEquals(0,mergeGradeBaseline(old,listOf(GradeObservation("A",mapOf("1" to grade(" 4 ").copy(description="  Test\n"))))).second)
 }
 @Test fun oldBaselineMigratesWithoutInventedHistory() {
  val baseline=sessionJson.decodeFromString<GradeBaseline>("""{"accounts":{}}""")
  assertTrue(baseline.history.isEmpty())
 }

}