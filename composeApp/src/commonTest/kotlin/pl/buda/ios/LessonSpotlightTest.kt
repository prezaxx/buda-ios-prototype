package pl.buda.ios
import kotlinx.datetime.*
import kotlin.test.*
class LessonSpotlightTest {
 private fun row(id:String,start:String="08:00",end:String="08:45",cancelled:Boolean=false,date:String="2026-11-10")=
  DiaryRow(id,"Matematyka","",date,lesson=LessonInfo(start,end,"12",cancelled))
 @Test fun selectsCurrentBeforeUpcomingRegardlessOfInputOrder() {
  val result=lessonSpotlight(listOf(row("next","09:00","09:45"),row("current")),LocalDateTime(2026,11,10,8,30))!!
  assertEquals("current",result.row.key);assertTrue(result.active);assertEquals(15,result.minutes)
  assertEquals(2f/3f,result.progress,0.001f)
 }
 @Test fun boundariesDoNotShowEndedLesson() {
  assertTrue(lessonSpotlight(listOf(row("a")),LocalDateTime(2026,11,10,8,0))!!.active)
  assertNull(lessonSpotlight(listOf(row("a")),LocalDateTime(2026,11,10,8,45)))
 }
 @Test fun nextLessonRoundsRemainingSecondsUp() {
  val result=lessonSpotlight(listOf(row("b","10:00","10:45"),row("a")),LocalDateTime(2026,11,10,7,59,30))!!
  assertEquals("a",result.row.key);assertFalse(result.active);assertEquals(1,result.minutes);assertEquals(0f,result.progress)
 }
 @Test fun rejectsCancelledInvalidOtherDayAndMissingTimes() {
  val rows=listOf(row("cancelled",cancelled=true),row("bad",start="bad"),row("reverse",start="09:00"),row("tomorrow",date="2026-11-11"),row("missing").copy(lesson=null))
  assertNull(lessonSpotlight(rows,LocalDateTime(2026,11,10,8,15)))
 }
 @Test fun finishedDayDoesNotInventNextLesson() {
  assertNull(lessonSpotlight(listOf(row("a")),LocalDateTime(2026,11,10,18,0)))
 }
}
