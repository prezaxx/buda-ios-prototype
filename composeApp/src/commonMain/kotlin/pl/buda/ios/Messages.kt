package pl.buda.ios

import com.fleeksoft.ksoup.Ksoup
import io.github.szpontium.api.prometheus.PrometheusMessagesApi
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

data class Mailbox(val key:String,val name:String)
data class MailItem(val key:String,val id:Int,val title:String,val sender:String,val date:String,val read:Boolean,val attachments:Boolean)
data class MailAttachment(val name:String,val url:String?)
data class MailDetail(val key:String,val title:String,val sender:String,val recipients:String,val date:String,val text:String,val attachments:List<MailAttachment>)
data class MailState(val busy:Boolean=false,val boxes:List<Mailbox> = emptyList(),val box:String?=null,val items:List<MailItem> = emptyList(),val detail:MailDetail?=null,val more:Boolean=false,val error:String?=null,val loaded:Boolean=false)
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
class MessagesController(private val scope:CoroutineScope) {
 private val mutable=MutableStateFlow(MailState())
 val state=mutable.asStateFlow()
 private var student:Student?=null
 private var api:PrometheusMessagesApi?=null
 private var job:Job?=null
 fun cancel() { job?.cancel();mutable.value=mutable.value.copy(busy=false) }
 fun clear() { cancel();api?.close();api=null;student=null;mutable.value=MailState() }
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
    mutable.value=mutable.value.copy(busy=false,error="Nie udało się pobrać wiadomości. Kod: $code. Ponów pobieranie. Jeśli błąd wraca, podaj ten kod — nie zawiera danych logowania. Oceny pozostają dostępne.")
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
   ensureActive();mutable.value=MailState(busy=true,boxes=boxes,loaded=true)
  }
 }
 fun choose(key:String) {
  if(mutable.value.boxes.none { it.key==key }) return
  cancel();mutable.value=mutable.value.copy(box=key,items=emptyList(),detail=null,more=false,loaded=false,error=null)
  inbox(key,false)
 }
 fun chooseAnother() { cancel();mutable.value=mutable.value.copy(box=null,items=emptyList(),detail=null,more=false,error=null) }
 fun backToInbox() { cancel();mutable.value=mutable.value.copy(detail=null,error=null) }
 fun more() { if(!mutable.value.busy && mutable.value.more) mutable.value.box?.let { inbox(it,true) } }
 private fun inbox(box:String,append:Boolean) {
  val before=mutable.value
  val cursor=if(append) before.items.lastOrNull()?.id ?: return else 0
  request {
   val page=parseInbox(client().readPayload("OdebraneSkrzynka",mapOf("globalKeySkrzynka" to box,"idLastWiadomosc" to cursor.toString(),"pageSize" to "50")))
   val items=mergeInbox(before.items,page,append)
   if(append && page.size>=50 && page.last().id==cursor) throw MessageFormatException()
   ensureActive();mutable.value=before.copy(busy=true,items=items,detail=null,more=page.size>=50,loaded=true,error=null)
  }
 }
 fun open(key:String) {
  if(mutable.value.items.none { it.key==key }) return
  request {
   val detail=parseMailDetail(client().readPayload("WiadomoscSzczegoly",mapOf("apiGlobalKey" to key)),key)
   ensureActive();mutable.value=mutable.value.copy(detail=detail)
  }
 }
}