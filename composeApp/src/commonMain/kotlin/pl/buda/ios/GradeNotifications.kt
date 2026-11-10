package pl.buda.ios
import io.github.szpontium.api.hebe.SzpontHebeCeApi
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.time.Clock

interface GradePollCallback { fun completed(result:String) }
interface NotificationControls { fun showSettings();fun clear();fun planReminder(title:String,date:String) }
object NoNotificationControls:NotificationControls { override fun showSettings() {};override fun clear() {};override fun planReminder(title:String,date:String) {} }
@Serializable data class NotificationGrade(val content:String,val subject:String,val description:String,val comment:String)
@Serializable data class GradeChange(val id:String,val student:String,val period:String,val at:String,val before:NotificationGrade?=null,val after:NotificationGrade,val accountKey:String?=null)
@Serializable data class GradeBaseline(val accounts:Map<String,Map<String,NotificationGrade>> = emptyMap(),val history:List<GradeChange> = emptyList())
@Serializable data class GradeObservation(val key:String,val grades:Map<String,NotificationGrade>,val student:String="Uczeń",val period:String="")
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
   if(before==null || normalizedGrade(before)!=normalizedGrade(value)) events+=GradeChange(scopeKey(listOf(observation.key,id,at)),observation.student,observation.period,at,before,value,observation.key)
  }
  // Keep IDs through empty/transient responses to avoid replaying old grades.
  updated[observation.key]=old.orEmpty()+observation.grades
 }
 return GradeBaseline(updated,(events.asReversed()+previous.history).take(100)) to events.size
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
  val client=HttpClient { install(HttpTimeout) { requestTimeoutMillis=7000 } }
  try {
   for(student in stored.students) {
    currentCoroutineContext().ensureActive()
    try {
     val account=student.account
     val period=account.periods?.firstOrNull { it.current==true } ?: account.periods?.lastOrNull()
     if(period==null) { failures++;continue }
     val api=SzpontHebeCeApi(student.credential.restore(),client)
     val grades=collectPages(500,{ row:GradeRow -> row.id }) { last -> parseGrades(api.getGradePayload(account.unit.restUrl,account.unit.id,account.pupil.id,period.id,lastId=last,pageSize=500),account.pupil.id) }
     val key=listOf(student.credential.deviceId,account.unit.restUrl,account.unit.id,account.pupil.id,account.caretakerId,account.profileId,period.id).joinToString(":")
     observations+=GradeObservation(key,grades.associate { it.id.toString() to NotificationGrade(it.content,it.subject,it.description,it.comment) },"${account.pupil.firstName} ${account.pupil.surname}","${period.start} — ${period.end}")
    } catch(e:CancellationException) { throw e }
      catch(e:Exception) { currentCoroutineContext().ensureActive();failures++ }
   }
  } finally { client.close() }
  currentCoroutineContext().ensureActive()
  val (next,changes)=mergeGradeBaseline(previous,observations)
  return GradePollResult(if(observations.isEmpty()) "NO_DATA" else "OK",sessionJson.encodeToString(next),changes,observations.size,failures)
 }
}