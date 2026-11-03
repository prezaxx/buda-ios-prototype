package pl.buda.ios
import kotlinx.datetime.*

data class ScheduleDay(val date:LocalDate,val rows:List<DiaryRow>)
internal fun scheduleDays(rows:List<DiaryRow>,from:String,to:String):List<ScheduleDay> {
 val start=LocalDate.parse(from);val end=LocalDate.parse(to)
 require(start<=end && start.daysUntil(end)<=366)
 val grouped=rows.groupBy { it.date.take(10) }
 return (0..start.daysUntil(end)).map { offset ->
  val date=start.plus(offset,DateTimeUnit.DAY)
  ScheduleDay(date,grouped[date.toString()].orEmpty().sortedWith(compareBy({it.detail.substringBefore('\n')},{it.title},{it.key})))
 }
}
internal fun dayTitle(date:LocalDate):String {
 val names=listOf("Poniedziałek","Wtorek","Środa","Czwartek","Piątek","Sobota","Niedziela")
 return "${names[date.dayOfWeek.ordinal]}, ${date.dayOfMonth}.${date.monthNumber.toString().padStart(2,'0')}"
}
internal fun matchesSearch(query:String,vararg values:String)=query.trim().let { q -> q.isBlank() || values.any { it.contains(q,true) } }