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
 BudaTheme {
  if(logout) AlertDialog(onDismissRequest={logout=false},title={Text("Wylogować?")},text={Text("Usuniemy zapisaną sesję z tego iPhone’a.")},confirmButton={TextButton(onClick={logout=false;model.logout()}){Text("Wyloguj")}},dismissButton={TextButton(onClick={logout=false}){Text("Anuluj")}})
  Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
   Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Surface(Modifier.fillMaxWidth().padding(top=8.dp),shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.primaryContainer) {
     Row(Modifier.padding(horizontal=18.dp,vertical=12.dp),horizontalArrangement=Arrangement.SpaceBetween) {
      Column { Text("Buda",style=MaterialTheme.typography.headlineMedium);Text("Twój szkolny dzień",style=MaterialTheme.typography.labelMedium) }
      Text("0.6",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
     }
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
      var pupils by remember { mutableStateOf(false) }
      Box {
       FilledTonalButton(onClick={pupils=true}) { Text("Zmień ucznia") }
       DropdownMenu(pupils,{pupils=false}) {
        state.students.forEachIndexed { index,item ->
         DropdownMenuItem(text={Column { Text(item.title);Text(item.account.classDisplay.orEmpty()+" · "+item.school,style=MaterialTheme.typography.bodySmall) }},onClick={pupils=false;model.switchStudent(index)})
        }
        HorizontalDivider()
        DropdownMenuItem(text={Text("Lista uczniów i ustawienia sesji")},onClick={pupils=false;model.back()})
       }
      }
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
     if(state.section==Section.HOME) {
      DashboardPane(state,Modifier.weight(1f),onSection={model.section(it)})
     } else if(state.section==Section.SCHEDULE) {
      key(state.selected) { SchedulePane(state,Modifier.weight(1f),onCurrentWeek={model.section(Section.SCHEDULE)}) }
     } else if(state.section==Section.GRADES) {
      key(state.selected,state.periodId) { GradesPane(state,Modifier.weight(1f)) }
     } else if(state.section==Section.MESSAGES) {
      MessagesPane(model.messages,Modifier.weight(1f))
     } else {
     var query by remember(state.section,state.selected) { mutableStateOf("") }
     OutlinedTextField(query,{query=it},label={Text("Szukaj w pobranych wpisach")},singleLine=true,modifier=Modifier.fillMaxWidth(),trailingIcon={if(query.isNotEmpty()) TextButton(onClick={query=""}) { Text("Wyczyść") }})
     val shownRows=state.rows.filter { matchesSearch(query,it.title,it.detail,it.badge,it.date) }
     val uri=LocalUriHandler.current
     var linkError by remember(state.section,state.selected) { mutableStateOf(false) }
     LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=20.dp)) {
      state.error?.let { message -> item { Text(message,color=MaterialTheme.colorScheme.error) } }
      if(linkError) item { Text("Nie udało się otworzyć załącznika.",color=MaterialTheme.colorScheme.error) }
      if(!state.busy && state.error==null && shownRows.isEmpty()) item { Text(if(query.isBlank()) "Brak wpisów w wybranym zakresie." else "Brak wyników wyszukiwania.") }
      items(shownRows,key={it.key}) { row ->
       DiaryEntryCard(row) { link -> try { uri.openUri(link.url) } catch(e:Exception) { linkError=true } }
      }
     }
     }
    }
   }
  }
 }
}

@Composable internal fun DiaryEntryCard(row:DiaryRow,open:(DiaryLink)->Unit) {
 Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
   if(row.badge.isNotBlank()) Text(row.badge,color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge)
   Text(row.title,style=MaterialTheme.typography.titleMedium)
   if(row.date.isNotBlank()) Text(row.date.take(10),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)
   Text(row.detail)
   row.links.forEach { link -> TextButton(onClick={open(link)}) { Text(link.title) } }
  }
 }
}
