package pl.buda.ios
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*
import kotlin.time.Clock
data class WeekSlot(val row:DiaryRow,val start:Int,val end:Int,val lane:Int)
internal fun lessonMinutes(value:String):Int?=try { LocalTime.parse(value).let { it.hour*60+it.minute } } catch(e:Exception) { null }
internal fun weekSlots(rows:List<DiaryRow>):List<WeekSlot> {
 val valid=rows.mapNotNull { row ->
  val info=row.lesson ?: return@mapNotNull null
  val start=lessonMinutes(info.start) ?: return@mapNotNull null
  val end=lessonMinutes(info.end) ?: return@mapNotNull null
  if(end<=start) null else Triple(row,start,end)
 }.sortedWith(compareBy({it.second},{it.third},{it.first.key}))
 val ends=mutableListOf<Int>()
 return valid.map { (row,start,end) ->
  val free=ends.indexOfFirst { it<=start };val lane=if(free<0) ends.size else free
  if(free<0) ends+=end else ends[lane]=end
  WeekSlot(row,start,end,lane)
 }
}
@Composable internal fun SchedulePane(state:ScreenState,modifier:Modifier,onCurrentWeek:()->Unit,onReminder:(DiaryRow)->Unit={}) {
 var week by remember { mutableStateOf(true) }
 Column(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   FilterChip(selected=!week,onClick={week=false},label={Text("Dni / lista")})
   FilterChip(selected=week,onClick={week=true},label={Text("Tydzień")})
   TextButton(onClick=onCurrentWeek) { Text("Dzisiaj") }
  }
  if(week) WeekSchedule(state,Modifier.weight(1f),onReminder)
  else ScheduleListPane(state,Modifier.weight(1f),onCurrentWeek)
 }
}
@Composable private fun WeekSchedule(state:ScreenState,modifier:Modifier,onReminder:(DiaryRow)->Unit) {
 val range=scheduleDays(state.rows,state.from,state.to)
 val days=range.take(7)
 val layouts=days.map { weekSlots(it.rows) }
 val slots=layouts.flatten()
 val first=((slots.minOfOrNull { it.start } ?: 480)/60)*60
 val last=(((slots.maxOfOrNull { it.end } ?: 960)+59)/60)*60
 val dayWidth=124.dp;val axis=45.dp
 val now=Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
 var selected by remember { mutableStateOf<DiaryRow?>(null) }
 selected?.let { row ->
  AlertDialog(onDismissRequest={selected=null},title={Text(row.title)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text(row.date.take(10));if(row.badge.isNotBlank()) Text(row.badge);Text(row.detail)
  }},confirmButton={TextButton(onClick={selected=null}) { Text("Zamknij") }},
   dismissButton={TextButton(onClick={onReminder(row);selected=null}) { Text("Przypomnij") }})
 }
 Column(modifier,verticalArrangement=Arrangement.spacedBy(6.dp)) {
  state.error?.let { Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall) }
  if(range.size>7) Text("Siatka pokazuje pierwsze 7 dni zakresu. Cały zakres znajdziesz w widoku Dni / lista.",style=MaterialTheme.typography.labelSmall)
  Text("Przesuń plan w bok. Dotknij lekcji, aby zobaczyć szczegóły.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  if(state.busy && state.rows.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
  Column(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
   Row {
    Spacer(Modifier.width(axis))
    days.forEach { day ->
     Surface(Modifier.width(dayWidth).padding(2.dp),shape=MaterialTheme.shapes.medium,color=if(day.date==now.date) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
      Text(dayTitle(day.date),Modifier.padding(8.dp),style=MaterialTheme.typography.labelMedium,maxLines=2)
     }
    }
   }
   Column(Modifier.verticalScroll(rememberScrollState())) {
    Box(Modifier.width(axis+dayWidth*days.size).height((last-first).dp)) {
     for(minute in first until last step 60) {
      Row(Modifier.offset(y=(minute-first).dp).fillMaxWidth(),verticalAlignment=Alignment.Top) {
       Text((minute/60).toString()+":00",Modifier.width(axis),style=MaterialTheme.typography.labelSmall)
       HorizontalDivider(Modifier.weight(1f))
      }
     }
     days.forEachIndexed { index,day ->
      val layout=layouts[index];val lanes=(layout.maxOfOrNull { it.lane } ?: 0)+1
      val laneWidth=dayWidth/lanes
      layout.forEach { slot ->
       val row=slot.row;val cancelled=row.lesson?.cancelled==true
       Card(onClick={selected=row},
        modifier=Modifier.offset(x=axis+dayWidth*index+laneWidth*slot.lane,y=(slot.start-first).dp).width(laneWidth).height((slot.end-slot.start).dp).padding(2.dp),
        colors=CardDefaults.cardColors(containerColor=if(cancelled) MaterialTheme.colorScheme.surfaceVariant else if(row.badge.isNotBlank()) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(5.dp)) {
         Text(row.title,style=MaterialTheme.typography.labelMedium,maxLines=2,overflow=TextOverflow.Ellipsis,textDecoration=if(cancelled) TextDecoration.LineThrough else null)
         Text(row.lesson?.start?.take(5).orEmpty()+if(row.lesson?.room?.isNotBlank()==true) " · "+row.lesson.room else "",style=MaterialTheme.typography.labelSmall,maxLines=1)
        }
       }
      }
      val minute=now.hour*60+now.minute
      if(day.date==now.date && minute in first until last) Box(Modifier.offset(x=axis+dayWidth*index,y=(minute-first).dp).width(dayWidth).height(2.dp).background(MaterialTheme.colorScheme.error))
     }
    }
    val located=slots.map { it.row.key }.toSet()
    val unknown=days.flatMap { it.rows }.filter { it.key !in located }
    if(unknown.isNotEmpty()) {
     Text("Zajęcia bez poprawnej godziny",Modifier.padding(8.dp),style=MaterialTheme.typography.titleSmall)
     unknown.forEach { row -> TextButton(onClick={selected=row}) { Text(row.date.take(10)+" · "+row.title) } }
    }
   }
  }
 }
}
