package pl.buda.ios
import com.fleeksoft.ksoup.Ksoup
import io.github.szpontium.api.hebe.SzpontHebeCeApi
import io.github.szpontium.api.hebe.models.Account
import kotlinx.serialization.json.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

enum class Section(val label:String,val dated:Boolean=false) {
 MESSAGES("Wiadomości"), GRADES("Oceny"), SCHEDULE("Plan lekcji",true), PRESENCE("Frekwencja",true),
 EXAMS("Sprawdziany",true), HOMEWORK("Zadania",true), NOTES("Uwagi"),
 ANNOUNCEMENTS("Ogłoszenia"), SUMMARY("Średnie i końcowe")
}
data class DiaryLink(val title:String,val url:String)
data class DiaryRow(val key:String,val title:String,val detail:String,val date:String="",val badge:String="",val links:List<DiaryLink> = emptyList())
data class DiaryResult(val rows:List<DiaryRow>,val warning:String?=null)
class DiaryFormatException:Exception("UnsupportedDiaryFormat")
internal fun JsonObject.obj(key:String)=this[key] as? JsonObject
internal fun JsonObject.text(key:String)=(this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
internal fun JsonObject.flag(key:String)=(this[key] as? JsonPrimitive)?.booleanOrNull==true
internal fun plain(value:String)=Ksoup.parse(value.replace(Regex("(?i)<br\\s*/?>"),"\n")).text()
private fun employee(value:JsonObject?):String=value?.text("DisplayName")?.ifBlank {
 listOf(value.text("Name"),value.text("Surname")).filter { it.isNotBlank() }.joinToString(" ")
}.orEmpty()
private fun details(vararg values:String)=values.filter { it.isNotBlank() }.joinToString("\n")
private fun attachments(row:JsonObject):List<DiaryLink> = (row["Attachments"] as? JsonArray).orEmpty().mapNotNull {
 val a=it as? JsonObject ?: return@mapNotNull null
 val url=a.text("Link")
 if(url.startsWith("https://",true)) DiaryLink(a.text("Name").ifBlank { "Załącznik" },url) else null
}
internal fun mapDiaryRow(section:Section,row:JsonObject,source:String):DiaryRow {
 val id=row.text("Id").ifBlank { throw DiaryFormatException() }
 val key="$source:$id"
 val subject=row.obj("Subject")?.text("Name").orEmpty()
 val content=plain(row.text("Content"))
 val author=employee(row.obj("Creator") ?: row.obj("Sender"))
 return when(section) {
 Section.SCHEDULE -> {
  val sub=row.obj("Substitution")
  fun effective(name:String)=sub?.obj(name) ?: row.obj(name)
  val slot=effective("TimeSlot")
  val date=sub?.text("DateAt")?.ifBlank { null } ?: row.text("DateAt")
  val time=details(slot?.text("Start").orEmpty(),slot?.text("End").orEmpty()).replace("\n"," – ")
  val cancelled=sub?.flag("ClassAbsence")==true || sub?.obj("Change")?.text("Type")=="2" || sub?.text("TeacherAbsenceEffectName")?.contains("odwołan",true)==true
  val title=effective("Subject")?.text("Name").orEmpty().ifBlank { row.text("ExtraDescription").ifBlank { "Zajęcia" } }
  val room=if(sub?.flag("NoRoom")==true) "" else effective("Room")?.text("Code").orEmpty()
  val note=sub?.let { details(it.text("PupilNote"),it.text("Reason"),it.text("Description")) }.orEmpty()
  val original=if(sub!=null) details(row.text("DateAt"),row.obj("TimeSlot")?.text("Start").orEmpty(),row.obj("Subject")?.text("Name").orEmpty()) else ""
  DiaryRow(key,title,details(time,employee(effective("TeacherPrimary") ?: effective("Teacher")),if(room.isNotBlank()) "Sala: $room" else "",plain(note),plain(row.text("Event")),if(original.isNotBlank()) "Pierwotnie: $original" else ""),date,if(cancelled) "Odwołane" else if(sub!=null) "Zmiana / zastępstwo" else "")
 }
 Section.PRESENCE -> {
  val type=row.obj("PresenceType");val slot=row.obj("TimeSlot")
  DiaryRow(key,type?.text("Name").orEmpty().ifBlank { "Nieopisany wpis frekwencji" },details(slot?.text("Display").orEmpty(),slot?.text("Start").orEmpty(),type?.text("CategoryName").orEmpty()),row.text("DayAt"),type?.text("Symbol").orEmpty())
 }
 Section.EXAMS -> DiaryRow(key,subject.ifBlank { "Sprawdzian" },details(content,author),row.text("DeadlineAt"),row.text("Type"))
 Section.HOMEWORK -> DiaryRow(key,subject.ifBlank { "Zadanie domowe" },details(content,author,if(row.flag("IsAnswerRequired")) "Wymagana odpowiedź — dodaj ją w dzienniku szkoły." else ""),row.text("DeadlineAt"),links=attachments(row))
 Section.NOTES -> DiaryRow(key,row.obj("Category")?.text("Name").orEmpty().ifBlank { "Uwaga" },details(content,author,row.text("Points").let { if(it.isBlank()) "" else "Punkty: $it" }),row.text("ValidAt"),if(row.flag("Positive")) "Pozytywna" else "")
 Section.ANNOUNCEMENTS -> DiaryRow(key,row.text("Title").ifBlank { "Ogłoszenie" },details(content,author),row.text("From"),links=attachments(row))
 Section.SUMMARY -> if(source.contains("average")) DiaryRow(key,subject,details("Średnia: "+row.text("Average").ifBlank { "nie podano" },row.text("Points").let { if(it.isBlank()) "" else "Punkty: $it" }),badge="Średnia szkoły")
 else DiaryRow(key,subject,details("Przewidywana: "+row.text("Entry_1").ifBlank { "nie podano" },"Końcowa: "+row.text("Entry_2").ifBlank { "nie podano" },row.text("Entry_3").let { if(it.isBlank()) "" else "Dodatkowy wpis szkoły: $it" }),badge="Klasyfikacja")
 Section.GRADES, Section.MESSAGES -> throw DiaryFormatException()
 }
}
internal suspend fun fetchDiary(api:SzpontHebeCeApi,account:Account,section:Section,period:Int?,from:String,to:String):DiaryResult {
 val endpoints=when(section) {
 Section.SCHEDULE -> listOf("mobile/schedule/withchanges/byPupil","mobile/schedule/extra/withchanges/byPupil")
 Section.PRESENCE -> listOf("mobile/presence/extra/byPupil")
 Section.EXAMS -> listOf("mobile/exam/byPupil")
 Section.HOMEWORK -> listOf("mobile/homework/byPupil")
 Section.NOTES -> listOf("mobile/note/byPupil")
 Section.ANNOUNCEMENTS -> listOf("mobile/announcement/byPupil")
 Section.SUMMARY -> listOf("mobile/grade/average/byPupil","mobile/grade/summary/byPupil")
 else -> error("UseGradesReader")
 }
 if(section==Section.SUMMARY && period==null) return DiaryResult(emptyList(),"Brak okresu oceniania dla tego konta.")
 val output=mutableListOf<DiaryRow>();val errors=mutableListOf<String>()
 for(endpoint in endpoints) {
  try {
   val rows=collectPages(500,{ row:JsonObject -> row.text("Id").toIntOrNull() ?: throw DiaryFormatException() }) { last ->
    val query=mutableMapOf<String,Any?>("pupilId" to account.pupil.id,"lastId" to last,"pageSize" to 500)
    if(section.dated) { query["dateFrom"]=from;query["dateTo"]=to }
    if(section in listOf(Section.SCHEDULE,Section.PRESENCE,Section.EXAMS,Section.HOMEWORK,Section.NOTES)) query["lastSyncDate"]="1970-01-01 01:00:00"
    if(section==Section.ANNOUNCEMENTS) { query["unitId"]=account.unit.id;query["view"]=6 }
    if(section==Section.SUMMARY) { query["unitId"]=account.unit.id;query["periodId"]=period;if(endpoint.contains("average")) query["scope"]="auto" }
    val payload=api.readDiaryPayload(account.unit.restUrl,account.pupil.id,endpoint,query)
    val array=payload as? JsonArray ?: throw DiaryFormatException()
    array.map { item ->
     val row=item as? JsonObject ?: throw DiaryFormatException()
     val pupil=row["PupilId"] ?: row["IdPupil"]
     if(pupil!=null && pupil !is JsonNull && (pupil as? JsonPrimitive)?.intOrNull!=account.pupil.id) throw DiaryFormatException()
     row
    }
   }
   output += rows.map { mapDiaryRow(section,it,endpoint) }
  } catch(e:CancellationException) { throw e }
    catch(e:Exception) { currentCoroutineContext().ensureActive(); errors += safeError(e,if(endpoint.contains("extra")) "zajęcia dodatkowe" else if(endpoint.contains("average")) "średnie" else section.label) }
 }
 val sorted=if(section in listOf(Section.NOTES,Section.ANNOUNCEMENTS,Section.PRESENCE)) output.sortedByDescending { it.date } else output.sortedWith(compareBy({it.date},{it.detail},{it.title}))
 return DiaryResult(sorted,errors.takeIf { it.isNotEmpty() }?.joinToString("\n"))
}
