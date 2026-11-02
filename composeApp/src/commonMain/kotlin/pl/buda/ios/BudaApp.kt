package pl.buda.ios
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable fun BudaApp(vault:SessionVault) {
 val scope=rememberCoroutineScope();val model=remember(vault) { Prototype(scope,vault) }
 DisposableEffect(model) { onDispose { model.close() } }
 val state by model.state.collectAsState()
 var email by remember { mutableStateOf("") };var password by remember { mutableStateOf("") }
 var logout by remember { mutableStateOf(false) }
 MaterialTheme {
  if(logout) AlertDialog(onDismissRequest={logout=false},title={Text("Wylogować?")},text={Text("Usuniemy zapisaną sesję z tego iPhone’a.")},confirmButton={TextButton(onClick={logout=false;model.logout()}){Text("Wyloguj")}},dismissButton={TextButton(onClick={logout=false}){Text("Anuluj")}})
  Surface(Modifier.fillMaxSize()) {
   Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     Text("Buda",style=MaterialTheme.typography.headlineLarge)
     Text("0.2",style=MaterialTheme.typography.labelLarge)
    }
    if(state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    state.notice?.let { Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.tertiary) }
    if(state.students.isEmpty()) {
     state.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
     OutlinedTextField(email,{email=it},label={Text("E-mail eduVULCAN")},singleLine=true,enabled=!state.busy,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),modifier=Modifier.fillMaxWidth())
     OutlinedTextField(password,{password=it},label={Text("Hasło")},singleLine=true,enabled=!state.busy,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
     Row { Checkbox(state.remember,{model.remember(it)},enabled=!state.busy);Text("Zapamiętaj sesję na tym iPhonie",Modifier.padding(top=12.dp)) }
     Button(onClick={model.login(email,password);password=""},enabled=!state.busy,modifier=Modifier.fillMaxWidth()) { Text("Zaloguj się") }
     Text("Nieoficjalny klient eduVULCAN. Hasło nie jest zapisywane. Sesję chroni pęk kluczy iPhone’a.",style=MaterialTheme.typography.bodySmall)
    } else if(state.selected==null) {
     Text("Wybierz ucznia",style=MaterialTheme.typography.titleLarge)
     LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
      items(state.students.size) { index -> val student=state.students[index]
       OutlinedButton(onClick={model.select(index)},modifier=Modifier.fillMaxWidth()) { Column { Text(student.title);Text(student.school);Text(student.account.classDisplay.orEmpty()) } }
      }
     }
     Row { Checkbox(state.remember,{model.remember(it)});Text("Zapamiętaj sesję",Modifier.padding(top=12.dp)) }
     TextButton(onClick={logout=true}) { Text("Wyloguj") }
    } else {
     val student=state.students[state.selected!!]
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
      Column(Modifier.weight(1f)) { Text(student.title,style=MaterialTheme.typography.titleMedium);Text(student.account.classDisplay.orEmpty(),style=MaterialTheme.typography.bodySmall) }
      TextButton(onClick={model.back()}) { Text("Zmień ucznia") }
     }
     Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
      Section.entries.forEach { section -> FilterChip(selected=state.section==section,onClick={model.section(section)},label={Text(section.label)}) }
     }
     if(state.section in listOf(Section.GRADES,Section.SUMMARY)) {
      var expanded by remember { mutableStateOf(false) }
      Box {
       val period=student.account.periods.orEmpty().firstOrNull { it.id==state.periodId }
       TextButton(onClick={expanded=true}) { Text(period?.let { "${it.start} – ${it.end}" } ?: "Wybierz semestr") }
       DropdownMenu(expanded,{expanded=false}) { student.account.periods.orEmpty().forEach { p -> DropdownMenuItem(text={Text("${p.start} – ${p.end}")},onClick={expanded=false;model.period(p.id)}) } }
      }
     }
     if(state.section.dated) {
      var dates by remember { mutableStateOf(false) }
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
       if(state.section==Section.SCHEDULE) TextButton(onClick={model.shiftDays(-7)}) { Text("‹") }
       TextButton(onClick={dates=true}) { Text("${state.from} – ${state.to}",style=MaterialTheme.typography.labelMedium) }
       if(state.section==Section.SCHEDULE) TextButton(onClick={model.shiftDays(7)}) { Text("›") }
      }
      if(dates) {
       var from by remember { mutableStateOf(state.from) };var to by remember { mutableStateOf(state.to) }
       AlertDialog(onDismissRequest={dates=false},title={Text("Zakres dat")},text={Column {
        OutlinedTextField(from,{from=it},label={Text("Od: RRRR-MM-DD")},singleLine=true)
        OutlinedTextField(to,{to=it},label={Text("Do: RRRR-MM-DD")},singleLine=true)
       }},confirmButton={TextButton(onClick={model.dates(from,to);dates=false}){Text("Pobierz")}},dismissButton={TextButton(onClick={dates=false}){Text("Anuluj")}})
      }
     }
     Row { TextButton(onClick={model.refresh()},enabled=!state.busy) { Text("Odśwież") };TextButton(onClick={logout=true}) { Text("Wyloguj") } }
     val uri=LocalUriHandler.current
     var linkError by remember(state.section,state.selected) { mutableStateOf(false) }
     LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)) {
      state.error?.let { message -> item { Text(message,color=MaterialTheme.colorScheme.error) } }
      if(linkError) item { Text("Nie udało się otworzyć załącznika.",color=MaterialTheme.colorScheme.error) }
      if(!state.busy && state.error==null && state.grades.isEmpty() && state.rows.isEmpty()) item { Text("Brak wpisów w wybranym zakresie.") }
      if(state.section==Section.GRADES) items(state.grades,key={it.id}) { grade ->
       Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(grade.subject,style=MaterialTheme.typography.titleMedium);Text(grade.content,style=MaterialTheme.typography.headlineMedium)
        Text(grade.description);if(grade.comment.isNotBlank()) Text(grade.comment);Text(grade.createdAt.take(10),style=MaterialTheme.typography.bodySmall)
       } }
      } else items(state.rows,key={it.key}) { row ->
       Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        if(row.badge.isNotBlank()) Text(row.badge,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge)
        Text(row.title,style=MaterialTheme.typography.titleMedium)
        if(row.date.isNotBlank()) Text(row.date.take(10),style=MaterialTheme.typography.labelMedium)
        Text(row.detail)
        row.links.forEach { link -> TextButton(onClick={try { uri.openUri(link.url) } catch(e:Exception) { linkError=true }}) { Text(link.title) } }
       } }
      }
     }
    }
   }
  }
 }
}
