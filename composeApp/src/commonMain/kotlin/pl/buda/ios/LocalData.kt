package pl.buda.ios
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.time.Clock

@Serializable data class SavedScreen(val at:String,val grades:List<GradeRow> = emptyList(),val rows:List<DiaryRow> = emptyList(),val dashboard:DashboardData?=null)
@Serializable data class MailRecipient(val key:String,val name:String)
@Serializable data class MailDraft(val id:String,val thread:String,val title:String="",val text:String="",val recipients:List<MailRecipient> = emptyList(),val uncertain:Boolean=false,val replyTo:String?=null)
@Serializable private data class LocalData(val version:Int=1,val screens:Map<String,SavedScreen> = emptyMap(),val drafts:Map<String,MailDraft> = emptyMap())
internal fun scopeKey(parts:List<String>)=sessionJson.encodeToString(parts)
internal fun studentKey(student:Student)=scopeKey(listOf(student.credential.deviceId,student.account.unit.restUrl,student.account.unit.id.toString(),student.account.pupil.id.toString(),student.account.caretakerId.toString(),student.account.profileId.toString()))
internal fun screenKey(student:Student,state:ScreenState)=scopeKey(listOf(studentKey(student),state.section.name,state.periodId.toString(),if(state.section.dated) state.from else "",if(state.section.dated) state.to else ""))
class LocalDataStore(private val vault:SessionVault) {
 private var data=LocalData()
 var available=true;private set
 var persistent=true
 init { try { vault.read()?.let { data=sessionJson.decodeFromString<LocalData>(it);require(data.version==1) } } catch(e:Exception) { available=false;data=LocalData() } }
 fun screen(key:String):SavedScreen?=data.screens[key]
 fun draft(key:String):MailDraft?=data.drafts[key]
 private fun commit(value:LocalData):Boolean {
  if(!available) return false
  var candidate=value.copy(screens=value.screens.toList().sortedByDescending { it.second.at }.take(20).toMap())
  var encoded=sessionJson.encodeToString(candidate)
  while(encoded.encodeToByteArray().size>750000 && candidate.screens.isNotEmpty()) {
   val oldest=candidate.screens.minBy { it.value.at }.key
   candidate=candidate.copy(screens=candidate.screens-oldest);encoded=sessionJson.encodeToString(candidate)
  }
  if(encoded.encodeToByteArray().size>750000 || candidate.drafts.size>30) return false
  if(persistent && !vault.write(encoded)) return false
  data=candidate;return true
 }
 fun saveScreen(key:String,screen:SavedScreen)=sessionJson.encodeToString(screen).encodeToByteArray().size<=600000 && commit(data.copy(screens=data.screens+(key to screen)))
 fun saveDraft(key:String,draft:MailDraft):Boolean {
  if(draft.title.length>200 || draft.text.length>12000 || draft.recipients.size>30) return false
  return commit(data.copy(drafts=data.drafts+(key to draft)))
 }
 fun deleteDraft(key:String)=commit(data.copy(drafts=data.drafts-key))
 fun clear():Boolean { if(!vault.clear()) return false;data=LocalData();available=true;return true }
}
internal fun savedScreen(state:ScreenState)=SavedScreen(Clock.System.now().toString(),state.grades,state.rows,state.dashboard)
