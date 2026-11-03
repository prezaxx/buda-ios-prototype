package pl.buda.ios
import kotlinx.datetime.LocalDate
import kotlin.test.*
class ScheduleDaysTest {
 @Test fun groupsByDayAndOrdersByTime() {
  val rows=listOf(DiaryRow("b","Math","10:00 - 10:45","2026-11-03"),DiaryRow("c","Art","09:00 - 09:45","2026-11-04"),DiaryRow("a","English","08:00 - 08:45","2026-11-03"))
  val days=scheduleDays(rows,"2026-11-02","2026-11-08")
  assertEquals(7,days.size);assertTrue(days[0].rows.isEmpty());assertEquals(listOf("a","b"),days[1].rows.map { it.key });assertEquals("c",days[2].rows.single().key)
 }
 @Test fun substitutionsKeepTheirEffectiveDayAndBadge() {
  val row=DiaryRow("x","Art","08:00","2026-11-04T00:00:00","Odwołane")
  val days=scheduleDays(listOf(row),"2026-11-03","2026-11-05")
  assertEquals("Odwołane",days[1].rows.single().badge);assertTrue(days[0].rows.isEmpty())
 }
 @Test fun polishDayLabelsAndInvalidRange() {
  assertEquals("Wtorek, 3.11",dayTitle(LocalDate(2026,11,3)))
  assertFailsWith<IllegalArgumentException> { scheduleDays(emptyList(),"2026-11-04","2026-11-03") }
 }
 @Test fun searchIgnoresCaseAndWhitespace() {
  assertTrue(matchesSearch("  MAT  ","Matematyka","Test"));assertTrue(matchesSearch("","anything"));assertFalse(matchesSearch("geo","Matematyka"))
 }
}