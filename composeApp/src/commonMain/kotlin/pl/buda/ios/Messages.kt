package pl.buda.ios

import com.fleeksoft.ksoup.Ksoup
import io.github.szpontium.api.prometheus.PrometheusMessagesApi
import io.github.szpontium.api.prometheus.models.PrometheusSendMessage
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

data class Mailbox(val key:String,val name:String)
data class MailItem(val key:String,val id:Int,val title:String,val sender:String,val date:String,val read:Boolean,val attachments:Boolean)
data class MailAttachment(val name:String,val url:String?)
data class MailDetail(val key:String,val title:String,val sender:String,val recipients:String,val date:String,val text:String,val attachments:List<MailAttachment>)
enum class MailFolder(val label:String,val endpoint:String) { INBOX("Odebrane","OdebraneSkrzynka"), SENT("Wysłane","WyslaneSkrzynka") }
data class MailState(val composing:Boolean=false,val draft:MailDraft?=null,val recipients:List<MailRecipient> = emptyList(),val info:String?=null,val folder:MailFolder=MailFolder.INBOX,val busy:Boolean=false,val boxes:List<Mailbox> = emptyList(),val box:String?=null,val items:List<MailItem> = emptyList(),val detail:MailDetail?=null,val more:Boolean=false,val error:String?=null,val loaded:Boolean=false)
class MessageFormatException:Exception("UnsupportedMessageFormat")
private fun JsonElement.objectRows():List<JsonObject> = (this as? JsonArray ?: throw MessageFormatException()).map { it as? JsonObject ?: throw MessageFormatException() }
private fun JsonObject.required(key:String)=text(key).ifBlank { throw MessageFormatException() }
internal fun parseMailboxes(data:JsonElement)=data.objectRows().map { Mailbox(it.required("globalKey"),plain(it.text("nazwa")).ifBlank { "Skrzynka" }) }.distinctBy { it.key }
internal fun parseInbox(data:JsonElement)=data.objectRows().map {
 MailItem(it.required("apiGlobalKey"),it.text("id").toIntOrNull() ?: throw MessageFormatException(),plain(it.text("temat")).ifBlank { "Bez tematu" },plain(it.text("korespondenci")),it.text("data"),it.flag("przeczytana"),it.flag("hasZalaczniki"))
}
internal fun attachmentUrl(raw:String):String? = try {
 val value=raw.trim()
 val resolved=if(value.startsWith("/") && !value.startsWith("//")) "https://wiadomosci.eduvulcan.pl$value" else value
 val url=Url(resolved)
 if(url.protocol==URLProtocol.HTTPS && url.host.isNotBlank() && url.user.isNullOrEmpty() && url.password.isNullOrEmpty()) url.toString() else null
} catch(e:Exception) { null }
internal fun messageText(html:String):String {
 val doc=Ksoup.parse(html)
 doc.select("script,style,iframe,object").remove()
 doc.select("br").forEach { it.after("\n") }
 doc.select("p,div,li,tr").forEach { it.after("\n") }
 return doc.body().wholeText().trim()
}
internal fun parseMailDetail(data:JsonElement,expectedKey:String):MailDetail {
 val row=data as? JsonObject ?: throw MessageFormatException()
 val key=row.required("apiGlobalKey")
 if(key!=expectedKey) throw MessageFormatException()
 val recipients=(row["odbiorcy"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.joinToString(", ")
 val attachments=(row["zalaczniki"] as? JsonArray).orEmpty().map { item ->
  val a=item as? JsonObject ?: throw MessageFormatException()
  MailAttachment(a.text("nazwaPliku").ifBlank { "Załącznik" },attachmentUrl(a.text("url")))
 }
 return MailDetail(key,plain(row.text("temat")).ifBlank { "Bez tematu" },plain(row.text("nadawca")),plain(recipients),row.text("data"),messageText(row.text("tresc")),attachments)
}
internal fun mergeInbox(old:List<MailItem>,page:List<MailItem>,append:Boolean):List<MailItem> {
 if(append && page.isNotEmpty() && page.none { next -> old.none { it.key==next.key } }) throw MessageFormatException()
 return ((if(append) old else emptyList())+page).distinctBy { it.key }
}
internal fun resolveMailbox(boxes:List<Mailbox>,savedKey:String?,accountKey:String?,accountName:String?):String? {
 fun key(value:String?)=value?.trim()?.removePrefix("{")?.removeSuffix("}")?.lowercase().orEmpty()
 fun findKey(value:String?):String? {
  val expected=key(value)
  return if(expected.isEmpty()) null else boxes.singleOrNull { key(it.key)==expected }?.key
 }
 findKey(savedKey)?.let { return it }
 findKey(accountKey)?.let { return it }
 // Exact mailbox metadata only, never a pupil-name substring or first mailbox.
 fun name(value:String?)=value.orEmpty().trim().lowercase().replace(Regex("\\s+")," ")
 val expected=name(accountName)
 return if(expected.isBlank()) null else boxes.singleOrNull { name(it.name)==expected }?.key
}
class MessagesController(private val scope:CoroutineScope,private val local:LocalDataStore=LocalDataStore(MemoryVault()),private val onMailboxChosen:(Student,String)->Unit={ _,_ -> }) {
 private val mutable=MutableStateFlow(MailState())
 val state=mutable.asStateFlow()
 private var student:Student?=null
 private var api:PrometheusMessagesApi?=null
 private var job:Job?=null
 private var draftSave:Job?=null
 private fun draftKey():String? { val pupil=student ?: return null;val box=mutable.value.box ?: return null;return scopeKey(listOf(studentKey(pupil),box)) }
 private fun flushDraft():Boolean { draftSave?.cancel();val draft=mutable.value.draft ?: return true;val key=draftKey() ?: return false;return local.saveDraft(key,draft) }
 fun cancel(preserveDraft:Boolean=true) { if(preserveDraft && !flushDraft()) mutable.value=mutable.value.copy(info="Nie zapisano szkicu na dysku. Pozostaje w pamięci tej sesji.");draftSave?.cancel();job?.cancel();mutable.value=mutable.value.copy(busy=false) }
 fun clear(preserveDraft:Boolean=true) { cancel(preserveDraft);api?.close();api=null;student=null;mutable.value=MailState() }
 fun use(value:Student) { clear();student=value }
 private fun client():PrometheusMessagesApi {
  api?.let { return it }
  val s=student ?: error("NoStudent")
  check(s.tenant!=null && s.cookies.isNotEmpty()) { "MissingMessagesSession" }
  return PrometheusMessagesApi(tenant=s.tenant,initialCookies=s.cookies.map { it.restore() }).also { api=it }
 }
 private fun request(action:suspend CoroutineScope.()->Unit) {
  cancel();mutable.value=mutable.value.copy(busy=true,error=null)
  job=scope.launch {
   try { action();ensureActive();mutable.value=mutable.value.copy(busy=false) }
   catch(e:CancellationException) { throw e }
   catch(e:Exception) {
    ensureActive()
    val code=(e as? io.github.szpontium.api.prometheus.MessagesAccessException)?.code ?: e::class.simpleName.orEmpty()
    api?.close();api=null
    mutable.value=mutable.value.copy(busy=false,error=if(mutable.value.draft?.uncertain==true) "Wynik wysłania jest nieznany ($code). Sprawdź folder Wysłane przed kolejną próbą. Szkic zachowano; nie ponawiamy automatycznie." else "Nie udało się pobrać wiadomości. Kod: $code. Ponów pobieranie. Jeśli błąd wraca, podaj ten kod — nie zawiera danych logowania. Oceny pozostają dostępne.")
   }
  }
 }
 fun refresh() {
  val s=student ?: return
  if(s.tenant==null || s.cookies.isEmpty()) {
   mutable.value=MailState(error="Ta sesja pochodzi ze starszej wersji. Wyloguj się i zaloguj ponownie, aby włączyć wiadomości.");return
  }
  val box=mutable.value.box
  if(box!=null) { inbox(box,false);return }
  request {
   val boxes=parseMailboxes(client().readPayload("Skrzynki"))
   ensureActive()
   val key=resolveMailbox(boxes,s.mailboxKey,s.account.messageBox?.globalKey,s.account.messageBox?.name)
   mutable.value=MailState(busy=true,boxes=boxes,loaded=true,box=key)
   if(key!=null) loadInbox(key,false)
  }
 }
 fun choose(key:String) {
  if(mutable.value.boxes.none { it.key==key }) return
  student?.let { onMailboxChosen(it,key);student=it.copy(mailboxKey=key) }
  cancel();mutable.value=mutable.value.copy(composing=false,draft=null,recipients=emptyList(),box=key,items=emptyList(),detail=null,more=false,loaded=false,error=null)
  inbox(key,false)
 }
 fun folder(value:MailFolder) {
  val box=mutable.value.box ?: return
  if(value==mutable.value.folder) return
  cancel();mutable.value=mutable.value.copy(composing=false,draft=null,recipients=emptyList(),folder=value,items=emptyList(),detail=null,more=false,loaded=false,error=null)
  inbox(box,false)
 }
 fun chooseAnother() { cancel();mutable.value=mutable.value.copy(composing=false,draft=null,recipients=emptyList(),box=null,items=emptyList(),detail=null,more=false,error=null) }
 fun backToInbox() { cancel();mutable.value=mutable.value.copy(detail=null,error=null) }
 fun more() { if(!mutable.value.busy && mutable.value.more) mutable.value.box?.let { inbox(it,true) } }
 private fun inbox(box:String,append:Boolean) { request { loadInbox(box,append) } }
 private suspend fun loadInbox(box:String,append:Boolean) {
  val before=mutable.value
  val cursor=if(append) before.items.lastOrNull()?.id ?: return else 0
   val page=parseInbox(client().readPayload(before.folder.endpoint,mapOf("globalKeySkrzynka" to box,"idLastWiadomosc" to cursor.toString(),"pageSize" to "50")))
   val items=mergeInbox(before.items,page,append)
   if(append && page.size>=50 && page.last().id==cursor) throw MessageFormatException()
   currentCoroutineContext().ensureActive();mutable.value=before.copy(busy=true,items=items,detail=null,more=page.size>=50,loaded=true,error=null)
 }
 fun open(key:String) {
  if(mutable.value.items.none { it.key==key }) return
  request {
   val detail=parseMailDetail(client().readPayload("WiadomoscSzczegoly",mapOf("apiGlobalKey" to key)),key)
   ensureActive();mutable.value=mutable.value.copy(detail=detail)
  }
 }
 @OptIn(ExperimentalUuidApi::class)
 fun compose(replyKey:String?=null) {
  val key=draftKey() ?: return
  if(mutable.value.busy) return
  val old=mutable.value.draft ?: local.draft(key)
  val draft=old ?: MailDraft(Uuid.random().toString(),Uuid.random().toString(),replyTo=replyKey)
  mutable.value=mutable.value.copy(composing=true,draft=draft,error=null,info=if(old!=null && replyKey!=null) "Otworzono istniejący szkic. Usuń go, aby rozpocząć inną odpowiedź." else null)
  request {
   val box=mutable.value.box ?: return@request
   var recipients=parseRecipients(client().readPayload("Pracownicy",mapOf("globalKeySkrzynka" to box)))
   if(draft.replyTo!=null) {
    val reply=parseReplyRecipient(client().readPayload("WiadomoscOdpowiedzPrzekaz",mapOf("apiGlobalKey" to draft.replyTo)),draft.replyTo,box)
    recipients=(recipients+reply.second).distinctBy { it.key }
    if(draft.title.isBlank() && draft.recipients.isEmpty()) mutable.value=mutable.value.copy(draft=draft.copy(title="Re: "+reply.first,recipients=listOf(reply.second)))
   }
   ensureActive();mutable.value=mutable.value.copy(recipients=recipients)
   if(!flushDraft()) mutable.value=mutable.value.copy(info="Nie udało się zapisać szkicu. Nie zamykaj aplikacji.")
  }
 }
 fun editDraft(title:String?=null,text:String?=null,recipient:MailRecipient?=null) {
  val current=mutable.value.draft ?: return
  if(mutable.value.busy || current.uncertain) return
  val recipients=if(recipient==null) current.recipients else if(current.recipients.any { it.key==recipient.key }) current.recipients.filter { it.key!=recipient.key } else if(mutable.value.recipients.any { it==recipient } && current.recipients.size<30) current.recipients+recipient else current.recipients
  mutable.value=mutable.value.copy(draft=current.copy(title=(title ?: current.title).take(200),text=(text ?: current.text).take(12000),recipients=recipients))
  draftSave?.cancel();draftSave=scope.launch { delay(500);if(!flushDraft()) mutable.value=mutable.value.copy(info="Nie zapisano szkicu na dysku.") }
 }
 fun closeComposer() { val ok=flushDraft();mutable.value=mutable.value.copy(composing=false,info=if(ok) "Szkic zachowano." else "Nie zapisano szkicu na dysku; pozostaje w pamięci tej sesji.") }
 fun discardDraft() {
  if(mutable.value.busy) return
  draftSave?.cancel();val key=draftKey() ?: return
  if(!local.deleteDraft(key)) { mutable.value=mutable.value.copy(error="Nie udało się usunąć szkicu.");return }
  mutable.value=mutable.value.copy(composing=false,draft=null,recipients=emptyList(),error=null,info="Szkic usunięto.")
 }
 fun sendDraft() {
  val state=mutable.value;val draft=state.draft ?: return;val box=state.box ?: return;val key=draftKey() ?: return
  if(state.busy || !canSendDraft(draft,state.recipients)) return
  // Persist unknown outcome before POST, so interruption never causes an automatic resend.
  val pending=draft.copy(uncertain=true)
  if(!local.saveDraft(key,pending)) { mutable.value=state.copy(error="Nie udało się zabezpieczyć szkicu przed wysłaniem. Spróbuj po odblokowaniu telefonu.");return }
  draftSave?.cancel();mutable.value=state.copy(draft=pending)
  request {
   client().sendMessage(PrometheusSendMessage(draft.id,draft.thread,box,draft.recipients.map { it.key },draft.title.trim(),draft.text))
   ensureActive()
   val removed=local.deleteDraft(key)
   mutable.value=mutable.value.copy(composing=false,draft=null,recipients=emptyList(),info=if(removed) "Serwer potwierdził przyjęcie wiadomości. Możesz sprawdzić folder Wysłane." else "Serwer przyjął wiadomość, ale nie usunięto szkicu. Nie wysyłaj go ponownie.")
  }
 }

}