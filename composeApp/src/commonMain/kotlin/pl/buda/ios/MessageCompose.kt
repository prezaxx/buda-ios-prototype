package pl.buda.ios
import kotlinx.serialization.json.*
internal fun parseRecipients(data:JsonElement):List<MailRecipient> {
 val rows=data as? JsonArray ?: throw MessageFormatException()
 return rows.map { raw ->
  val row=raw as? JsonObject ?: throw MessageFormatException()
  val key=row.text("skrzynkaGlobalKey");val name=plain(row.text("nazwa"))
  if(key.isBlank() || name.isBlank()) throw MessageFormatException()
  MailRecipient(key,name)
 }.distinctBy { it.key }
}
internal fun parseReplyRecipient(data:JsonElement,expectedMessage:String,expectedBox:String):Pair<String,MailRecipient> {
 val row=data as? JsonObject ?: throw MessageFormatException()
 if(row.text("apiGlobalKey")!=expectedMessage || row.text("uzytkownikSkrzynkaGlobalKey")!=expectedBox) throw MessageFormatException()
 val key=row.text("nadawcaSkrzynkaGlobalKey");val name=plain(row.text("nadawcaSkrzynkaNazwa"))
 if(key.isBlank() || name.isBlank() || key==expectedBox) throw MessageFormatException()
 return plain(row.text("temat")) to MailRecipient(key,name)
}
internal fun canSendDraft(draft:MailDraft,available:List<MailRecipient>)= !draft.uncertain && draft.title.isNotBlank() && draft.title.length<=200 && draft.text.isNotBlank() && draft.text.length<=12000 && draft.recipients.isNotEmpty() && draft.recipients.size<=30 && draft.recipients.all { selected -> available.any { it==selected } }
