package pl.buda.ios
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable fun MessagesPane(model:MessagesController,modifier:Modifier=Modifier) {
 val state by model.state.collectAsState()
 if(state.composing) { MailComposer(model,state,modifier);return }
 var query by remember(state.box,state.folder) { mutableStateOf("") }
 var unreadOnly by remember(state.box,state.folder) { mutableStateOf(false) }
 val visible=state.items.filter { (state.folder!=MailFolder.INBOX || !unreadOnly || !it.read) && matchesSearch(query,it.title,it.sender,it.date) }
 val uri=LocalUriHandler.current
 var linkError by remember(state.detail?.key) { mutableStateOf(false) }
 LazyColumn(modifier,verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=24.dp)) {
  state.info?.let { item { Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary) } }
  if(state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
  if(state.box!=null) item {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    Column(Modifier.weight(1f)) {
     Text("SKRZYNKA UCZNIA",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
     Text(state.boxes.firstOrNull { it.key==state.box }?.name.orEmpty(),style=MaterialTheme.typography.titleSmall)
    }
    TextButton(onClick={model.chooseAnother()}) { Text("Zmień") }
   }
   FilledTonalButton(onClick={model.compose()},enabled=!state.busy) { Text("Nowa wiadomość / szkic") }
   if(state.detail!=null) TextButton(onClick={model.backToInbox()}) { Text("‹ Wróć do listy") }
  }
  state.error?.let { message -> item {
   Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer)) {
    Column(Modifier.padding(14.dp)) { Text(message);TextButton(onClick={model.refresh()},enabled=!state.busy) { Text("Ponów pobieranie") } }
   }
  } }
  if(linkError) item { Text("Nie udało się otworzyć załącznika.",color=MaterialTheme.colorScheme.error) }
  if(state.box==null) {
   if(!state.busy && state.boxes.isNotEmpty()) item { Text("Wybierz skrzynkę dla tego ucznia. Zapamiętamy wybór, aby następnym razem otworzyć ją automatycznie.") }
   items(state.boxes,key={it.key}) { box ->
    Card(onClick={model.choose(box.key)},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) { Text(box.name,Modifier.padding(18.dp),style=MaterialTheme.typography.titleMedium) }
   }
   if(state.loaded && !state.busy && state.boxes.isEmpty() && state.error==null) item { Text("Konto nie udostępniło żadnej skrzynki.") }
  } else {
   val detail=state.detail
   if(detail!=null) {
    item {
     Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
      Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
       SelectionContainer { Text(detail.title,style=MaterialTheme.typography.headlineSmall) }
       if(detail.sender.isNotBlank()) Text("Od: ${detail.sender}",style=MaterialTheme.typography.bodySmall)
       if(detail.recipients.isNotBlank()) Text("Do: ${detail.recipients}",style=MaterialTheme.typography.bodySmall)
       Text(detail.date,style=MaterialTheme.typography.labelSmall)
      }
     }
    }
    if(state.folder==MailFolder.INBOX) item { Button(onClick={model.compose(detail.key)},enabled=!state.busy) { Text("Odpowiedz nadawcy") } }
    item { SelectionContainer { Text(detail.text.ifBlank { "Brak treści wiadomości." },Modifier.padding(vertical=8.dp)) } }
    if(detail.attachments.isNotEmpty()) item { Text("Załączniki",style=MaterialTheme.typography.titleSmall) }
    items(detail.attachments) { attachment ->
     val url=attachment.url
     if(url!=null) OutlinedButton(onClick={try { uri.openUri(url) } catch(e:Exception) { linkError=true }},modifier=Modifier.fillMaxWidth()) { Text(attachment.name) }
     else Text("${attachment.name} — serwer nie udostępnił linku. Otwórz załącznik w portalu eduVULCAN.")
    }
    if(detail.attachments.isNotEmpty()) item {
     Text("Przeglądarka może wymagać logowania do portalu.",style=MaterialTheme.typography.bodySmall)
     TextButton(onClick={try { uri.openUri("https://eduvulcan.pl") } catch(e:Exception) { linkError=true }}) { Text("Otwórz eduVULCAN") }
    }
   } else {
    item {
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      MailFolder.entries.forEach { folder -> FilterChip(selected=state.folder==folder,onClick={model.folder(folder)},label={Text(folder.label)},modifier=Modifier.weight(1f)) }
     }
     OutlinedTextField(query,{query=it},label={Text("Temat lub korespondent")},singleLine=true,modifier=Modifier.fillMaxWidth(),trailingIcon={if(query.isNotEmpty()) TextButton(onClick={query=""}) { Text("Wyczyść") }})
     if(state.folder==MailFolder.INBOX) Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      FilterChip(selected=!unreadOnly,onClick={unreadOnly=false},label={Text("Wszystkie")})
      FilterChip(selected=unreadOnly,onClick={unreadOnly=true},label={Text("Nieprzeczytane")})
     }
     Text("${visible.size} z ${state.items.size} pobranych",style=MaterialTheme.typography.labelSmall)
    }
    items(visible,key={it.key}) { mail ->
     val unread=!mail.read && state.folder==MailFolder.INBOX
     Card(onClick={model.open(mail.key)},enabled=!state.busy,modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=if(unread) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
      Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
       if(unread) Text("● Nieprzeczytana",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
       Text(mail.title,style=MaterialTheme.typography.titleMedium)
       Text(mail.sender,style=MaterialTheme.typography.bodySmall)
       Text(mail.date,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
       if(mail.attachments) Text("Załączniki",style=MaterialTheme.typography.labelSmall)
      }
     }
    }
    if(visible.isEmpty() && state.items.isNotEmpty() && !state.busy) item { Text("Brak wyników w pobranych wiadomościach. Możesz pobrać starsze.") }
    if(state.more) item { Button(onClick={model.more()},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) { Text("Pobierz starsze") } }
    if(state.loaded && !state.busy && state.items.isEmpty() && state.error==null) item { Text("Brak wiadomości w tym folderze.") }
    item { Text("Odczyt bez zmiany statusu na serwerze. Nowe wiadomości wysyłasz po sprawdzeniu odbiorców i potwierdzeniu.",style=MaterialTheme.typography.bodySmall) }
   }
  }
 }
}