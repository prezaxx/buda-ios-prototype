package pl.buda.ios
import kotlinx.serialization.Serializable
import kotlinx.datetime.*
import kotlinx.coroutines.*

@Serializable data class DashboardData(val today:LocalDate,val schedule:DiaryResult,val exams:DiaryResult,val homework:DiaryResult)
internal suspend fun loadDashboard(today:LocalDate,fetch:suspend(Section,String,String)->DiaryResult):DashboardData=coroutineScope {
 suspend fun load(section:Section,to:LocalDate):DiaryResult=try { fetch(section,today.toString(),to.toString()) }
 catch(e:CancellationException) { throw e }
 catch(e:Exception) { currentCoroutineContext().ensureActive();DiaryResult(emptyList(),safeError(e,section.label)) }
 val end=today.plus(6,DateTimeUnit.DAY)
 val schedule=async { load(Section.SCHEDULE,today) }
 val exams=async { load(Section.EXAMS,end) }
 val homework=async { load(Section.HOMEWORK,end) }
 DashboardData(today,schedule.await(),exams.await(),homework.await())
}
internal fun upcomingRows(rows:List<DiaryRow>,from:LocalDate,to:LocalDate)=rows.filter {
 val date=try { LocalDate.parse(it.date.take(10)) } catch(e:Exception) { null }
 date!=null && date>=from && date<=to
}.sortedWith(compareBy({it.date},{it.title},{it.key}))
internal fun lessonPhase(row:DiaryRow,now:LocalDateTime):String? {
 val lesson=row.lesson ?: return null
 if(lesson.cancelled || row.date.take(10)!=now.date.toString()) return null
 val start=try { LocalTime.parse(lesson.start) } catch(e:Exception) { return null }
 val end=try { LocalTime.parse(lesson.end) } catch(e:Exception) { return null }
 if(end<=start) return null
 return when { now.time>=start && now.time<end -> "Teraz";now.time<start -> "Przed Tobą";else -> "Zakończona" }
}
internal fun gradeGroups(rows:List<GradeRow>)=rows.groupBy { it.subject }.toList().sortedBy { it.first.lowercase() }