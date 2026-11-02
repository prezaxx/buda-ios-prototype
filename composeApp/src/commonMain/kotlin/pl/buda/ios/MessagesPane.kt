package pl.buda.ios
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable fun MessagesPane(model:MessagesController,modifier:Modifier=Modifier) {
 val state by model.state.collectAsState()
 val uri=LocalUriHandler.current
 var linkError by remember(state.detail?.key) { mutableStateOf(false) }
 Column(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)) {
  if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
  if(state.box!=null) {
   Text(state.boxes.firstOrNull { it.key==state.box }?.name.orEmpty(),style=MaterialTheme.typography.titleMedium)
   Row {
    if(state.detail!=null) TextButton(onClick={model.backToInbox()}) { Text("Do odebranych") }
    TextButton(onClick={model.chooseAnother()}) { Text("Wybierz skrzynkę") }
   }
  }
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)) {
   state.error?.let { item { Text(it,color=MaterialTheme.colorScheme.error) } }
   if(linkError) item { Text("Nie udało się otworzyć załącznika.",color=MaterialTheme.colorScheme.error) }
   if(state.box==null) {
    item { Text("Wybierz skrzynkę wiadomości. Skrzynki należą do zalogowanego konta i mogą obejmować więcej niż jednego ucznia.") }
    items(state.boxes,key={it.key}) { box -> OutlinedButton(onClick={model.choose(box.key)},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) { Text(box.name) } }
    if(state.loaded && !state.busy && state.boxes.isEmpty() && state.error==null) item { Text("Konto nie udostępniło żadnej skrzynki.") }
   } else {
    val detail=state.detail
    if(detail!=null) {
     item { Text(detail.title,style=MaterialTheme.typography.headlineSmall) }
     item { Text(listOf(detail.sender,detail.recipients,detail.date).filter { it.isNotBlank() }.joinToString("\n"),style=MaterialTheme.typography.bodySmall) }
     item { Text(detail.text.ifBlank { "Brak treści wiadomości." }) }
     items(detail.attachments) { attachment ->
      val url=attachment.url
      if(url!=null) OutlinedButton(onClick={try { uri.openUri(url) } catch(e:Exception) { linkError=true }}) { Text(attachment.name) }
      else Text("${attachment.name} — serwer nie udostępnił linku. Otwórz załącznik w portalu eduVULCAN.")
     }
     if(detail.attachments.isNotEmpty()) item {
      Text("Załączniki otwierają się w przeglądarce; może być wymagane logowanie do portalu.",style=MaterialTheme.typography.bodySmall)
      TextButton(onClick={try { uri.openUri("https://eduvulcan.pl") } catch(e:Exception) { linkError=true }}) { Text("Otwórz eduVULCAN") }
     }
    } else {
     items(state.items,key={it.key}) { mail ->
      OutlinedButton(onClick={model.open(mail.key)},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) {
       Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        if(!mail.read) Text("Nieprzeczytana",style=MaterialTheme.typography.labelSmall)
        Text(mail.title,style=MaterialTheme.typography.titleMedium)
        Text(mail.sender,style=MaterialTheme.typography.bodySmall)
        Text(mail.date,style=MaterialTheme.typography.bodySmall)
        if(mail.attachments) Text("Załączniki",style=MaterialTheme.typography.labelSmall)
       }
      }
     }
     if(state.more) item { Button(onClick={model.more()},enabled=!state.busy) { Text("Pobierz starsze") } }
     if(state.loaded && !state.busy && state.items.isEmpty() && state.error==null) item { Text("Brak odebranych wiadomości.") }
     item { Text("Odczyt bez zmiany statusu na serwerze. Wysyłanie odpowiedzi nie jest jeszcze dostępne.",style=MaterialTheme.typography.bodySmall) }
    }
   }
  }
 }
}