package pl.buda.ios
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import kotlin.time.Clock

@Composable private fun liveClock():LocalDateTime {
 var now by remember { mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())) }
 LaunchedEffect(Unit) { while(true) { delay(30000);now=Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) } }
 return now
}
@Composable internal fun DashboardPane(state:ScreenState,modifier:Modifier,onSection:(Section)->Unit,onReminder:(DiaryRow)->Unit={},onHistory:()->Unit={},onPlanReminder:()->Unit={}) {
 val now=liveClock();val data=state.dashboard
 LazyColumn(modifier,verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=24.dp)) {
  item {
   Surface(shape=MaterialTheme.shapes.large) {
    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer,MaterialTheme.colorScheme.secondaryContainer))).padding(22.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
     Text(if(now.hour<18) "DZIEŃ DOBRY" else "DOBRY WIECZÓR",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
     Text(dayTitle(data?.today ?: now.date),style=MaterialTheme.typography.headlineSmall)
     Text("Sprawdź plan i zaplanuj to, co przed Tobą.",style=MaterialTheme.typography.bodyMedium)
    }
   }
  }
  item { DashboardShortcuts(onHistory,onPlanReminder) }
  if(data!=null && data.today==now.date && data.schedule.warning==null) {
   lessonSpotlight(data.schedule.rows,now)?.let { spotlight ->
    item { LessonSpotlightCard(spotlight) { onSection(Section.SCHEDULE) } }
   }
  }
  if(data!=null) item {
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    val end=data.today.plus(6,DateTimeUnit.DAY)
    DashboardCount("Lekcje dziś",if(data.schedule.warning==null) upcomingRows(data.schedule.rows,data.today,data.today).count { it.lesson?.cancelled!=true }.toString() else "—",Modifier.weight(1f)) { onSection(Section.SCHEDULE) }
    DashboardCount("Sprawdziany · 7 dni",if(data.exams.warning==null) upcomingRows(data.exams.rows,data.today,end).size.toString() else "—",Modifier.weight(1f)) { onSection(Section.EXAMS) }
    DashboardCount("Zadania · 7 dni",if(data.homework.warning==null) upcomingRows(data.homework.rows,data.today,end).size.toString() else "—",Modifier.weight(1f)) { onSection(Section.HOMEWORK) }
   }
  }
  state.error?.let { item { Text(it,color=MaterialTheme.colorScheme.error) } }
  if(data!=null) {
   if(data.today!=now.date) item { Text("Dane z ${data.today}. Odśwież widok, aby pobrać dzisiejszy plan.",color=MaterialTheme.colorScheme.tertiary) }
   item { Text(if(data.today==now.date) "Dzisiaj w planie" else "Plan na ${data.today}",style=MaterialTheme.typography.titleLarge) }
   data.schedule.warning?.let { item { Text(it,color=MaterialTheme.colorScheme.error) } }
   val lessons=upcomingRows(data.schedule.rows,data.today,data.today).sortedBy { it.lesson?.start.orEmpty() }
   items(lessons,key={"home:"+it.key}) { LessonCard(it,now) }
   if(lessons.isEmpty() && data.schedule.warning==null) item { EmptyCard("Brak zajęć w planie na dziś.") }
   for((section,result) in listOf(Section.EXAMS to data.exams,Section.HOMEWORK to data.homework)) {
    item(key="heading:$section") {
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
      Text(section.label,style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
      TextButton(onClick={onSection(section)}) { Text("Wszystkie ›") }
     }
     Text("Najbliższe 7 dni",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.secondary)
    }
    result.warning?.let { item(key="warning:$section") { Text(it,color=MaterialTheme.colorScheme.error) } }
    val rows=upcomingRows(result.rows,data.today,data.today.plus(6,DateTimeUnit.DAY))
    items(rows.take(8),key={"$section:${it.key}"}) { row ->
     Card(onClick={onSection(section)},modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
      Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
       Text(row.date.take(10),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
       Text(row.title,style=MaterialTheme.typography.titleMedium)
       Text(row.detail,maxLines=3,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
       TextButton(onClick={onReminder(row)}) { Text("Przypomnij mi") }
      }
     }
    }
    if(rows.isEmpty() && result.warning==null) item(key="empty:$section") { EmptyCard("Brak wpisów z terminem w najbliższych 7 dniach.") }
    if(rows.size>8) item { TextButton(onClick={onSection(section)}) { Text("Zobacz pozostałe (${rows.size-8})") } }
   }
  } else if(state.busy) item { Text("Pobieram Twój dzień…",style=MaterialTheme.typography.bodyMedium) }
 }
}
@Composable private fun DashboardCount(title:String,count:String,modifier:Modifier,onClick:()->Unit) {
 Card(onClick=onClick,modifier=modifier,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
   Text(count,style=MaterialTheme.typography.headlineMedium,color=MaterialTheme.colorScheme.primary)
   Text(title,style=MaterialTheme.typography.labelSmall)
  }
 }
}
@Composable private fun EmptyCard(text:String) {
 Surface(Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceVariant) { Text(text,Modifier.padding(16.dp),style=MaterialTheme.typography.bodyMedium) }
}
@Composable private fun LessonCard(row:DiaryRow,now:LocalDateTime) {
 val info=row.lesson;val phase=lessonPhase(row,now)
 val active=phase=="Teraz"
 Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=if(active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
  Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
   Column(Modifier.width(58.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
    Text(info?.start?.take(5).orEmpty().ifBlank { "—" },style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
    Text(info?.end?.take(5).orEmpty(),style=MaterialTheme.typography.labelMedium)
   }
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
    Text(row.title,style=MaterialTheme.typography.titleMedium)
    if(row.badge.isNotBlank()) Text(row.badge,style=MaterialTheme.typography.labelMedium,color=if(info?.cancelled==true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
    if(phase!=null) Text(phase,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)
    val timeLine=listOf(info?.start.orEmpty(),info?.end.orEmpty()).filter { it.isNotBlank() }.joinToString(" - ")
    val detail=if(timeLine.isNotBlank() && row.detail.substringBefore('\n')==timeLine) row.detail.substringAfter('\n',"") else row.detail
    if(detail.isNotBlank()) Text(detail,style=MaterialTheme.typography.bodySmall)
   }
  }
 }
}
@Composable internal fun SchedulePane(state:ScreenState,modifier:Modifier,onCurrentWeek:()->Unit) {
 val now=liveClock()
 var query by remember { mutableStateOf("") }
 var selectedDay by remember(state.from,state.to) { mutableStateOf<String?>(null) }
 var todayOnly by remember { mutableStateOf(false) }
 var collapsed by remember(state.from,state.to) { mutableStateOf(setOf<String>()) }
 val rows=state.rows.filter { matchesSearch(query,it.title,it.detail,it.badge) }
 val days=scheduleDays(rows,state.from,state.to)
 val dayFilter=if(todayOnly) now.date.toString() else selectedDay
 LazyColumn(modifier,verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=24.dp)) {
  item {
   Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    FilterChip(selected=dayFilter==null,onClick={todayOnly=false;selectedDay=null},label={Text("Cały zakres")})
    FilterChip(selected=todayOnly,onClick={todayOnly=true;if(days.none { it.date==now.date }) onCurrentWeek()},label={Text("Dzisiaj")})
    TextButton(onClick={todayOnly=false;selectedDay=null;onCurrentWeek()}) { Text("Bieżący tydzień") }
   }
   Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
    days.forEach { day -> FilterChip(selected=dayFilter==day.date.toString(),onClick={todayOnly=false;selectedDay=day.date.toString()},label={Text(dayTitle(day.date))}) }
   }
   OutlinedTextField(query,{query=it},singleLine=true,label={Text("Przedmiot, sala lub nauczyciel")},modifier=Modifier.fillMaxWidth(),trailingIcon={if(query.isNotEmpty()) TextButton(onClick={query=""}) { Text("Wyczyść") }})
   Row {
    TextButton(onClick={collapsed=emptySet()}) { Text("Rozwiń dni") }
    TextButton(onClick={collapsed=days.map { it.date.toString() }.toSet()}) { Text("Zwiń dni") }
   }
  }
  state.error?.let { item { Text(it,color=MaterialTheme.colorScheme.error) } }
  days.filter { dayFilter==null || it.date.toString()==dayFilter }.forEach { day ->
   val id=day.date.toString();val closed=id in collapsed
   item(key="day:$id") {
    Surface(Modifier.fillMaxWidth().clickable { collapsed=if(closed) collapsed-id else collapsed+id },shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceVariant) {
     Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      Column(Modifier.weight(1f)) {
       Text(dayTitle(day.date),style=MaterialTheme.typography.titleMedium)
       if(day.date==now.date) Text("DZISIAJ",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
       if(!state.busy) Text("${day.rows.size} wpisów"+if(state.error!=null) " • dane niepełne" else "",style=MaterialTheme.typography.labelSmall)
      }
      Text(if(closed) "+" else "−",style=MaterialTheme.typography.titleLarge)
     }
    }
   }
   if(!closed) {
    items(day.rows,key={it.key}) { LessonCard(it,now) }
    if(day.rows.isEmpty() && !state.busy && state.error==null) item(key="empty:$id") { EmptyCard(if(query.isBlank()) "Brak zajęć w tym dniu." else "Brak pasujących zajęć.") }
   }
  }
  if(todayOnly && days.none { it.date==now.date }) item { Text("Dzisiaj jest poza pobranym zakresem. Wybierz bieżący tydzień.") }
  val other=rows.filter { row -> days.none { it.date.toString()==row.date.take(10) } }
  if(other.isNotEmpty() && dayFilter==null) {
   item { Text("Wpisy spoza zakresu lub bez daty",style=MaterialTheme.typography.titleMedium) }
   items(other,key={it.key}) { row -> Column { Text(row.date);LessonCard(row,now) } }
  }
 }
}
@Composable internal fun GradesPane(state:ScreenState,modifier:Modifier) {
 var query by remember { mutableStateOf("") };var grouped by remember { mutableStateOf(true) }
 var closed by remember { mutableStateOf(setOf<String>()) }
 var detail by remember { mutableStateOf<GradeRow?>(null) }
 val grades=state.grades.filter { matchesSearch(query,it.subject,it.description,it.comment,it.content) }
 detail?.let { grade -> AlertDialog(onDismissRequest={detail=null},title={Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { GradeBadge(grade.content,teacherRgb=grade.teacherRgb);Text(grade.subject,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge) }},text={SelectionContainer { Text(listOf(grade.description,grade.comment,grade.createdAt).filter { it.isNotBlank() }.joinToString("\n\n")) }},confirmButton={TextButton(onClick={detail=null}) { Text("Zamknij") }}) }
 LazyColumn(modifier,verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=24.dp)) {
  item {
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    FilterChip(selected=grouped,onClick={grouped=true},label={Text("Przedmioty")})
    FilterChip(selected=!grouped,onClick={grouped=false},label={Text("Najnowsze")})
   }
   OutlinedTextField(query,{query=it},singleLine=true,label={Text("Przedmiot lub opis oceny")},modifier=Modifier.fillMaxWidth(),trailingIcon={if(query.isNotEmpty()) TextButton(onClick={query=""}) { Text("Wyczyść") }})
  }
  item { GradeLegend() }
  state.error?.let { item { Text(it,color=MaterialTheme.colorScheme.error) } }
  if(!state.busy && state.error==null && grades.isEmpty()) item { EmptyCard("Brak ocen pasujących do widoku.") }
  if(grouped) gradeGroups(grades).forEach { (subject,list) ->
   item(key="subject:$subject") {
    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
     Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
      Row(Modifier.fillMaxWidth().clickable { closed=if(subject in closed) closed-subject else closed+subject },horizontalArrangement=Arrangement.SpaceBetween) {
       Text(subject,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
       Text("${list.size}  "+if(subject in closed) "+" else "−",color=MaterialTheme.colorScheme.primary)
      }
      if(subject !in closed) {
       list.chunked(4).forEach { chunk -> Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        chunk.forEach { grade -> GradeBadge(grade.content,Modifier.weight(1f),teacherRgb=grade.teacherRgb,onClick={detail=grade}) }
        repeat(4-chunk.size) { Spacer(Modifier.weight(1f)) }
       } }
       Text("Dotknij oceny, aby zobaczyć opis.",style=MaterialTheme.typography.labelSmall)
      }
     }
    }
   }
  } else items(grades.sortedByDescending { it.createdAt },key={it.id}) { grade ->
   Card(onClick={detail=grade},modifier=Modifier.fillMaxWidth()) {
    Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
     GradeBadge(grade.content,Modifier.widthIn(min=60.dp,max=100.dp),teacherRgb=grade.teacherRgb)
     Column(Modifier.weight(1f)) { Text(grade.subject,style=MaterialTheme.typography.titleMedium);Text(grade.description);Text(grade.createdAt.take(10),style=MaterialTheme.typography.labelSmall) }
    }
   }
  }
 }
}