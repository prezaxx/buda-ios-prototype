package pl.buda.ios
import io.github.szpontium.api.hebe.SzpontHebeCeApi
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.time.Clock
import kotlinx.datetime.*
import io.github.szpontium.api.prometheus.PrometheusMessagesApi

interface GradePollCallback { fun completed(result:String) }
interface NotificationControls { fun showSettings();fun clear();fun planReminder(title:String,date:String);fun showHistory()=showSettings();fun showAbout() {};fun appVersion():String="dev" }
object NoNotificationControls:NotificationControls { override fun showSettings() {};override fun clear() {};override fun planReminder(title:String,date:String) {} }
@Serializable data class NotificationGrade(val content:String,val subject:String,val description:String,val comment:String)
@Serializable data class GradeChange(val id:String,val student:String,val period:String,val at:String,val before:NotificationGrade?=null,val after:NotificationGrade,val accountKey:String?=null,val category:String?=null)
@Serializable data class GradeBaseline(val accounts:Map<String,Map<String,NotificationGrade>> = emptyMap(),val history:List<GradeChange> = emptyList(),val cursor:Int=0)
@Serializable data class GradeObservation(val key:String,val grades:Map<String,NotificationGrade>,val student:String="Uczeń",val period:String="",val category:String?=null)
@Serializable data class GradePollResult(val status:String,val snapshot:String="",val changes:Int=0,val accounts:Int=0,val failures:Int=0)
private fun normalizedGrade(value:NotificationGrade):NotificationGrade {
 fun clean(text:String)=text.trim().replace(Regex("\\s+")," ")
 return NotificationGrade(clean(value.content),clean(value.subject),clean(value.description),clean(value.comment))
}
internal fun mergeGradeBaseline(previous:GradeBaseline,observations:List<GradeObservation>,at:String=Clock.System.now().toString()):Pair<GradeBaseline,Int> {
 val updated=previous.accounts.toMutableMap();val events=mutableListOf<GradeChange>()
 for(observation in observations) {
  val old=updated[observation.key]
  if(old!=null) observation.grades.forEach { (id,value) ->
   val before=old[id]
   if(before==null || normalizedGrade(before)!=normalizedGrade(value)) events+=GradeChange(scopeKey(listOf(observation.key,id,at)),observation.student,observation.period,at,before,value,observation.key,observation.category)
  }
  // Keep IDs through empty/transient responses to avoid replaying old grades.
  updated[observation.key]=old.orEmpty()+observation.grades
 }
 return GradeBaseline(updated,(events.asReversed()+previous.history).take(100),previous.cursor) to events.size
}

class GradeNotificationPoller(private val vault:SessionVault,private val baselineVault:SessionVault) {
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
 private var job:Job?=null
 fun cancel() { job?.cancel();job=null }
 fun start(callback:GradePollCallback) {
  cancel()
  job=scope.launch {
   val result=try { withTimeout(25000) { poll() } }
   catch(e:TimeoutCancellationException) { GradePollResult("TIMEOUT") }
   catch(e:CancellationException) { throw e }
   catch(e:Exception) { GradePollResult("CHECK_FAILED") }
   ensureActive();callback.completed(sessionJson.encodeToString(result))
  }
 }
 private suspend fun poll():GradePollResult {
  val session=vault.read() ?: return GradePollResult("NO_SAVED_SESSION")
  if(session=="KEYCHAIN_UNAVAILABLE") return GradePollResult("DEVICE_LOCKED")
  val stored=try { decodeSession(session) } catch(e:Exception) { return GradePollResult("SESSION_UNAVAILABLE") }
  val rawBaseline=baselineVault.read()
  if(rawBaseline=="KEYCHAIN_UNAVAILABLE") return GradePollResult("DEVICE_LOCKED")
  val previous=try { rawBaseline?.let { sessionJson.decodeFromString(GradeBaseline.serializer(),it) } ?: GradeBaseline() }
   catch(e:Exception) { return GradePollResult("BASELINE_UNAVAILABLE") }
  val observations=mutableListOf<GradeObservation>();var failures=0
  val client=HttpClient { install(HttpTimeout) { requestTimeoutMillis=6000 } }
  val kinds=listOf(Section.GRADES,Section.PRESENCE,Section.EXAMS,Section.HOMEWORK,Section.NOTES,Section.ANNOUNCEMENTS,Section.SCHEDULE,Section.SUMMARY,Section.MESSAGES)
  val tasks=stored.students.flatMap { student -> kinds.map { student to it } }
  var cursor=previous.cursor.mod(tasks.size)
  val started=Clock.System.now()
  val today=started.toLocalDateTime(TimeZone.currentSystemDefault()).date
  try {
   for(n in tasks.indices) {
    if((Clock.System.now()-started).inWholeMilliseconds>18000) break
    val (student,section)=tasks[cursor];cursor=(cursor+1)%tasks.size
    try {
     val observation=withTimeout(6000) {
      val account=student.account
      val period=currentPeriod(account.periods.orEmpty(),today)
      val api=SzpontHebeCeApi(student.credential.restore(),client)
      val base=listOf(student.credential.deviceId,account.unit.restUrl,account.unit.id,account.pupil.id,account.caretakerId,account.profileId).joinToString(":")
      val title=account.pupil.firstName+" "+account.pupil.surname
      if(section==Section.GRADES) {
       check(period!=null) { "MissingPeriod" }
       val grades=collectPages(500,{ row:GradeRow -> row.id }) { last -> parseGrades(api.getGradePayload(account.unit.restUrl,account.unit.id,account.pupil.id,period.id,lastId=last,pageSize=500),account.pupil.id) }
       GradeObservation(base+":"+period.id,grades.associate { it.id.toString() to NotificationGrade(it.content,it.subject,it.description,it.comment) },title,period.start.toString()+" — "+period.end,"Oceny")
      } else if(section==Section.MESSAGES) {
       check(student.tenant!=null && student.cookies.isNotEmpty()) { "MissingMessagesSession" }
       val mail=PrometheusMessagesApi(tenant=student.tenant,initialCookies=student.cookies.map { it.restore() })
       try {
        val boxes=parseMailboxes(mail.readPayload("Skrzynki"))
        val box=resolveMailbox(boxes,student.mailboxKey,account.messageBox?.globalKey,account.messageBox?.name) ?: error("MailboxNotSelected")
        val rows=parseInbox(mail.readPayload("OdebraneSkrzynka",mapOf("globalKeySkrzynka" to box,"idLastWiadomosc" to "0","pageSize" to "100")))
        GradeObservation(base+":messages:"+box,rows.associate { it.key to NotificationGrade(it.title,"Wiadomości",it.sender,it.date) },title,"Wiadomości","Wiadomości")
       } finally { mail.close() }
      } else {
       val result=fetchDiary(api,account,section,period?.id,today.minus(30,DateTimeUnit.DAY).toString(),today.plus(90,DateTimeUnit.DAY).toString())
       check(result.warning==null) { "IncompleteCategory" }
       GradeObservation(base+":"+section.name+(if(section==Section.SUMMARY) ":"+period?.id else ""),
        result.rows.associate { it.key to NotificationGrade(it.title,section.label,it.detail,it.date+" "+it.badge) },title,section.label,section.label)
      }
     }
     observations+=observation
    } catch(e:TimeoutCancellationException) { failures++ }
      catch(e:CancellationException) { throw e }
      catch(e:Exception) { currentCoroutineContext().ensureActive();failures++ }
   }
  } finally { client.close() }
  currentCoroutineContext().ensureActive()
  val (next,changes)=mergeGradeBaseline(previous,observations)
  return GradePollResult("OK",sessionJson.encodeToString(next.copy(cursor=cursor)),changes,observations.size,failures)
 }
}