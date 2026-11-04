package pl.buda.ios
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable internal fun MailComposer(model:MessagesController,state:MailState,modifier:Modifier) {
 val draft=state.draft ?: return
 var confirm by remember { mutableStateOf(false) };var discard by remember { mutableStateOf(false) }
 var select by remember { mutableStateOf(false) };var query by remember { mutableStateOf("") }
 val sender=state.boxes.firstOrNull { it.key==state.box }?.name ?: "Wybrana skrzynka"
 if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Wysłać wiadomość?")},text={Column(Modifier.verticalScroll(rememberScrollState())) {
  Text("Od: $sender");Text("Do: "+draft.recipients.joinToString { it.name });Text("Temat: "+draft.title)
 }},confirmButton={TextButton(onClick={confirm=false;model.sendDraft()},enabled=!state.busy && canSendDraft(draft,state.recipients)){Text("Wyślij")}},dismissButton={TextButton(onClick={confirm=false}){Text("Wróć do edycji")}})
 if(discard) AlertDialog(onDismissRequest={discard=false},title={Text("Usunąć szkic?")},text={Text(if(draft.uncertain) "Najpierw sprawdź Wysłane. Usunięcie szkicu nie cofnie wiadomości, którą serwer mógł już przyjąć." else "Treść szkicu zostanie usunięta z tego urządzenia.")},confirmButton={TextButton(onClick={discard=false;model.discardDraft()}){Text("Usuń")}},dismissButton={TextButton(onClick={discard=false}){Text("Anuluj")}})
 LazyColumn(modifier,verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=20.dp)) {
  item { Text(if(draft.replyTo==null) "Nowa wiadomość" else "Odpowiedź",style=MaterialTheme.typography.headlineSmall);Text("Od: $sender",style=MaterialTheme.typography.bodySmall) }
  if(state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
  state.error?.let { item { Text(it,color=MaterialTheme.colorScheme.error) } }
  state.info?.let { item { Text(it,style=MaterialTheme.typography.bodySmall) } }
  if(draft.uncertain) item { Text("Wynik wysyłki jest nieznany. Szkic jest zablokowany, aby uniknąć podwójnego wysłania. Zamknij edytor i sprawdź Wysłane.",color=MaterialTheme.colorScheme.error) }
  if(draft.replyTo!=null) item { Text("Odpowiedź trafi jako nowa wiadomość do nadawcy. Nie dodajemy automatycznie pozostałych adresatów ani załączników.",style=MaterialTheme.typography.bodySmall) }
  item {
   Text("Do: "+draft.recipients.joinToString { it.name }.ifBlank { "wybierz odbiorców" })
   OutlinedButton(onClick={select=!select},enabled=!state.busy && !draft.uncertain) { Text(if(select) "Zamknij listę" else "Wybierz odbiorców") }
  }
  if(select && !draft.uncertain) {
   item { OutlinedTextField(query,{query=it},label={Text("Szukaj nauczyciela")},singleLine=true,modifier=Modifier.fillMaxWidth()) }
   val recipients=state.recipients.filter { matchesSearch(query,it.name) }
   items(recipients.take(50),key={it.key}) { recipient ->
    Row(Modifier.fillMaxWidth()) { Checkbox(draft.recipients.any { it.key==recipient.key },{model.editDraft(recipient=recipient)},enabled=!state.busy);Text(recipient.name,Modifier.weight(1f).padding(top=12.dp)) }
   }
   if(recipients.size>50) item { Text("Zawęź wyszukiwanie — pokazujemy pierwszych 50 wyników.") }
   if(state.recipients.isEmpty() && !state.busy) item { Text("Nie pobrano adresatów.");TextButton(onClick={model.compose()}){Text("Ponów pobieranie adresatów") } }
  }
  item { OutlinedTextField(draft.title,{model.editDraft(title=it)},label={Text("Temat")},singleLine=true,enabled=!state.busy && !draft.uncertain,modifier=Modifier.fillMaxWidth()) }
  item { OutlinedTextField(draft.text,{model.editDraft(text=it)},label={Text("Treść")},minLines=6,enabled=!state.busy && !draft.uncertain,modifier=Modifier.fillMaxWidth());Text("${draft.text.length}/12000 znaków",style=MaterialTheme.typography.labelSmall) }
  item { Button(onClick={confirm=true},enabled=!state.busy && canSendDraft(draft,state.recipients),modifier=Modifier.fillMaxWidth()) { Text("Sprawdź i wyślij") } }
  item { Row { TextButton(onClick={model.closeComposer()},enabled=!state.busy) { Text("Zachowaj i zamknij") };TextButton(onClick={discard=true},enabled=!state.busy) { Text("Usuń szkic") } } }
  item { Text("Szkic zapisujemy lokalnie dla tego ucznia i skrzynki. Trwały zapis wymaga włączonego Zapamiętaj sesję. Załączniki do wysyłania nie są jeszcze obsługiwane.",style=MaterialTheme.typography.bodySmall) }
 }
}
