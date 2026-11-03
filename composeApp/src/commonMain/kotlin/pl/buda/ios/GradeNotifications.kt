package pl.buda.ios
import io.github.szpontium.api.hebe.SzpontHebeCeApi
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

interface GradePollCallback { fun completed(result:String) }
interface NotificationControls { fun showSettings();fun clear() }
object NoNotificationControls:NotificationControls { override fun showSettings() {};override fun clear() {} }
@Serializable data class NotificationGrade(val content:String,val subject:String,val description:String,val comment:String)
@Serializable data class GradeBaseline(val accounts:Map<String,Map<String,NotificationGrade>> = emptyMap())
@Serializable data class GradeObservation(val key:String,val grades:Map<String,NotificationGrade>)
@Serializable data class GradePollResult(val status:String,val snapshot:String="",val changes:Int=0,val accounts:Int=0,val failures:Int=0)
internal fun mergeGradeBaseline(previous:GradeBaseline,observations:List<GradeObservation>):Pair<GradeBaseline,Int> {
 val updated=previous.accounts.toMutableMap();var changes=0
 for(observation in observations) {
  val old=updated[observation.key]
  if(old!=null) changes+=observation.grades.count { (id,value) -> old[id]!=value }
  // Keep previously observed IDs so a transient empty response cannot replay history.
  updated[observation.key]=old.orEmpty()+observation.grades
 }
 return GradeBaseline(updated) to changes
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
     observations+=GradeObservation(key,grades.associate { it.id.toString() to NotificationGrade(it.content,it.subject,it.description,it.comment) })
    } catch(e:CancellationException) { throw e }
      catch(e:Exception) { currentCoroutineContext().ensureActive();failures++ }
   }
  } finally { client.close() }
  currentCoroutineContext().ensureActive()
  val (next,changes)=mergeGradeBaseline(previous,observations)
  return GradePollResult(if(observations.isEmpty()) "NO_DATA" else "OK",sessionJson.encodeToString(next),changes,observations.size,failures)
 }
}