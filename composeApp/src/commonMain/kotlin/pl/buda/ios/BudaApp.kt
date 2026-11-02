package pl.buda.ios
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable fun BudaApp() {
 val scope=rememberCoroutineScope()
 val model=remember { Prototype(scope) }
 DisposableEffect(model) { onDispose { model.close() } }
 val state by model.state.collectAsState()
 var email by remember { mutableStateOf("") }
 var password by remember { mutableStateOf("") }
 MaterialTheme {
  Surface(Modifier.fillMaxSize()) {
   Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    Text("Buda iOS",style=MaterialTheme.typography.headlineLarge)
    Text("Prototyp 0.1.1 • eduVULCAN",style=MaterialTheme.typography.labelLarge)
    if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    state.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
    if(state.students.isEmpty()) {
     OutlinedTextField(email,{email=it},label={Text("E-mail")},singleLine=true,enabled=!state.busy,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),modifier=Modifier.fillMaxWidth())
     OutlinedTextField(password,{password=it},label={Text("Hasło")},singleLine=true,enabled=!state.busy,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
     Button(onClick={model.login(email,password);password=""},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) { Text("Zaloguj się") }
     Text("Nieoficjalny prototyp. Logowanie bezpośrednio do eduVULCAN. Sesja tylko w pamięci; bez zapisywania hasła. Ta wersja nie wysyła powiadomień.",style=MaterialTheme.typography.bodySmall)
    } else if(state.selected==null) {
     Text("Wybierz ucznia",style=MaterialTheme.typography.titleLarge)
     LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
      items(state.students.size) { index -> val student=state.students[index]
       OutlinedButton(onClick={model.select(index)},modifier=Modifier.fillMaxWidth()) { Column { Text(student.title);Text(student.school);Text(student.account.classDisplay.orEmpty()) } }
      }
     }
     TextButton(onClick={model.logout();password=""}) { Text("Wyloguj") }
    } else {
     val student=state.students[state.selected!!]
     Text(student.title,style=MaterialTheme.typography.titleLarge)
     Text(student.school,style=MaterialTheme.typography.bodySmall)
     var expanded by remember { mutableStateOf(false) }
     Box {
      OutlinedButton(onClick={expanded=true},enabled=!state.busy) { Text("Wybierz semestr") }
      DropdownMenu(expanded,{expanded=false}) {
       student.account.periods.orEmpty().forEach { p -> DropdownMenuItem(text={Text("${p.start} – ${p.end}")},onClick={expanded=false;model.period(p.id)}) }
      }
     }
     Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
      Button(onClick={model.refresh()},enabled=!state.busy) { Text("Odśwież") }
      TextButton(onClick={model.back()}) { Text("Zmień ucznia") }
     }
     if(!state.busy && state.error==null && state.grades.isEmpty()) Text("Brak ocen w wybranym okresie.")
     LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
      items(state.grades,key={it.id}) { grade ->
       Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(grade.subject,style=MaterialTheme.typography.titleMedium)
        Text(grade.content,style=MaterialTheme.typography.headlineMedium)
        Text(grade.description)
        if(grade.comment.isNotBlank()) Text(grade.comment)
        Text(grade.createdAt.take(10),style=MaterialTheme.typography.bodySmall)
       } }
      }
     }
    }
   }
  }
 }
}
