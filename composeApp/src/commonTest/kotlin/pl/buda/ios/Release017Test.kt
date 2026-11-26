package pl.buda.ios
import kotlin.test.*
import kotlinx.serialization.json.*
class Release017Test {
 private fun lesson(id:String,start:String,end:String,cancelled:Boolean=false)=DiaryRow(id,"Lesson","","2026-11-26",lesson=LessonInfo(start,end,"1",cancelled))
 @Test fun overlapUsesSeparateLanesAndAdjacentLessonsReuseLane() {
  val slots=weekSlots(listOf(lesson("a","08:00","08:45"),lesson("b","08:10","08:40"),lesson("c","08:45","09:30")))
  assertEquals(listOf(0,1,0),slots.map { it.lane })
  assertEquals(480,slots[0].start)
 }
 @Test fun invalidTimesStayOutOfGridAndCancelledLessonsRemain() {
  val slots=weekSlots(listOf(lesson("bad","no","08:00"),lesson("reverse","09:00","08:00"),lesson("cancelled","08:00","08:45",true)))
  assertEquals(listOf("cancelled"),slots.map { it.row.key })
 }
 @Test fun orderSurvivesRestartAndUnknownDuplicateSectionsDoNotHideCards() {
  assertEquals(defaultDashboardOrder.size,normalizeDashboardOrder(listOf("EXAMS","EXAMS","UNKNOWN")).size)
  val vault=MemoryVault();val first=LocalDataStore(vault)
  assertTrue(first.saveDashboardOrder(defaultDashboardOrder.reversed()))
  assertEquals(defaultDashboardOrder.reversed(),LocalDataStore(vault).dashboardOrder())
 }
 private fun map(section:Section,value:String)=mapDiaryRow(section,Json.parseToJsonElement(value).jsonObject,"test")
 @Test fun meetingsAndSchoolInfoKeepDatesAndTextWithoutHTML() {
  val meeting=map(Section.MEETINGS,"""{"Id":1,"Why":"Zebranie","Agenda":"<b>Oceny</b>","Where":"Sala 5","DateAt":"2026-12-01T17:00:00"}""")
  assertEquals("Zebranie",meeting.title);assertTrue(meeting.detail.contains("Oceny"));assertTrue(meeting.detail.contains("Sala 5"));assertFalse(meeting.detail.contains("<b>"))
  val info=map(Section.INFO,"""{"Id":2,"Topic":"Sekretariat","Content":"Godziny","DateAt":"2026-11-26"}""")
  assertEquals("Sekretariat",info.title);assertEquals("2026-11-26",info.date)
 }
 @Test fun plannedLessonsMealsAndTeachersDoNotInventMissingData() {
  val planned=map(Section.PLANNED,"""{"Id":1,"Subject":{"Name":"Biologia"},"Topic":"Komórka","DayAt":"2026-11-27","Replacement":true}""")
  assertEquals("Biologia",planned.title);assertTrue(planned.detail.contains("Komórka"));assertEquals("Zastępstwo",planned.badge)
  val meal=map(Section.MEALS,"""{"Id":2,"MealName":"Obiad","DietName":"Bezglutenowa","Dishes":"Zupa","Entires":[{"Name":"Ryż"}]}""")
  assertTrue(meal.detail.contains("Ryż"));assertEquals("",meal.date);assertEquals("Bezglutenowa",meal.badge)
  val teacher=map(Section.TEACHERS,"""{"Id":3,"DisplayName":"Anna Test","Description":"Wychowawca"}""")
  assertEquals("Anna Test",teacher.title);assertEquals("Wychowawca",teacher.detail)
 }
}
