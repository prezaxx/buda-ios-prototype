package pl.buda.ios
import kotlinx.datetime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*
class DashboardTest {
 @Test fun startFetchesTodayAndSevenInclusiveDays()=runTest {
  val calls=mutableListOf<Triple<Section,String,String>>()
  val data=loadDashboard(LocalDate(2026,11,3)) { section,from,to -> calls+=Triple(section,from,to);DiaryResult(emptyList()) }
  assertEquals(3,calls.size);assertTrue(Triple(Section.SCHEDULE,"2026-11-03","2026-11-03") in calls)
  assertTrue(Triple(Section.EXAMS,"2026-11-03","2026-11-09") in calls);assertTrue(Triple(Section.HOMEWORK,"2026-11-03","2026-11-09") in calls)
  assertNull(data.schedule.warning)
 }
 @Test fun failureInOneSectionDoesNotHideOthers()=runTest {
  val data=loadDashboard(LocalDate(2026,11,3)) { section,_,_ ->
   if(section==Section.EXAMS) error("SECRET_TOKEN")
   DiaryResult(listOf(DiaryRow(section.name,"Test","")))
  }
  assertEquals(1,data.schedule.rows.size);assertEquals(1,data.homework.rows.size);assertNotNull(data.exams.warning);assertFalse(data.exams.warning!!.contains("SECRET_TOKEN"))
 }
 @Test fun cancellationIsNotTurnedIntoEmptyDashboard()=runTest {
  assertFailsWith<CancellationException> { loadDashboard(LocalDate(2026,11,3)) { _,_,_ -> throw CancellationException() } }
 }
 @Test fun upcomingRangeFiltersOldUnknownAndOutOfRangeDates() {
  val rows=listOf("2026-11-02","2026-11-03T10:00:00","2026-11-09","2026-11-10","").mapIndexed { i,date -> DiaryRow("$i","Test","",date) }
  assertEquals(listOf("1","2"),upcomingRows(rows,LocalDate(2026,11,3),LocalDate(2026,11,9)).map { it.key })
 }
 private fun lesson()=DiaryRow("x","Math","","2026-11-03",lesson=LessonInfo("08:00","08:45","1",false))
 @Test fun lessonStatusHonorsStartAndEndBoundaries() {
  assertEquals("Przed Tobą",lessonPhase(lesson(),LocalDateTime(2026,11,3,7,59)))
  assertEquals("Teraz",lessonPhase(lesson(),LocalDateTime(2026,11,3,8,0)))
  assertEquals("Zakończona",lessonPhase(lesson(),LocalDateTime(2026,11,3,8,45)))
 }
 @Test fun cancelledMissingAndOtherDayLessonsAreNotLive() {
  val now=LocalDateTime(2026,11,3,8,15)
  assertNull(lessonPhase(lesson().copy(lesson=lesson().lesson!!.copy(cancelled=true)),now))
  assertNull(lessonPhase(lesson().copy(date="2026-11-04"),now))
  assertNull(lessonPhase(lesson().copy(lesson=null),now))
  assertNull(lessonPhase(lesson().copy(lesson=LessonInfo("bad","08:45","",false)),now))
  assertNull(lessonPhase(lesson().copy(lesson=LessonInfo("09:00","08:45","",false)),now))
 }
 @Test fun groupingPreservesNonNumericGradesAndDetails() {
  val grades=listOf(GradeRow(1,"np","Math","A","B","2026-11-03"),GradeRow(2,"5+","Art","C","D","2026-11-02"),GradeRow(3,"4","Math","E","F","2026-11-01"))
  val groups=gradeGroups(grades)
  assertEquals(listOf("Art","Math"),groups.map { it.first });assertEquals(listOf("np","4"),groups.last().second.map { it.content });assertEquals("B",groups.last().second.first().comment)
 }
}