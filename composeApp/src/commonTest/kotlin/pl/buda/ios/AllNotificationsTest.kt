package pl.buda.ios
import kotlin.test.*
import kotlinx.datetime.LocalDate
import io.github.szpontium.api.hebe.models.Period
class AllNotificationsTest {
 @Test fun dateOverridesStaleCurrentFlag() {
  val old=Period(id=1,start=LocalDate(2026,1,1),end=LocalDate(2026,6,30),current=true)
  val now=Period(id=2,start=LocalDate(2026,9,1),end=LocalDate(2027,1,31),current=false)
  assertEquals(2,currentPeriod(listOf(old,now),LocalDate(2026,11,23))?.id)
 }
 @Test fun categoriesRemainIndependentAndNewMessageIsDetected() {
  val value=NotificationGrade("Temat","Wiadomości","Nadawca","2026-11-23")
  val initial=mergeGradeBaseline(GradeBaseline(),listOf(GradeObservation("messages",mapOf("a" to value),category="Wiadomości"))).first
  val (next,count)=mergeGradeBaseline(initial,listOf(GradeObservation("messages",mapOf("a" to value,"b" to value),category="Wiadomości"),GradeObservation("homework",mapOf("a" to value),category="Zadania")))
  assertEquals(1,count);assertEquals("Wiadomości",next.history.first().category)
  assertEquals(0,mergeGradeBaseline(next,listOf(GradeObservation("messages",mapOf("a" to value,"b" to value),category="Wiadomości"))).second)
 }
}
