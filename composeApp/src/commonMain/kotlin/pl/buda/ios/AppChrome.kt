package pl.buda.ios
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable internal fun BudaHeader(version:String,notifications:NotificationControls,onLogout:(()->Unit)?) {
 var menu by remember { mutableStateOf(false) }
 Row(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
  Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.primary) {
   Box(Modifier.size(44.dp),contentAlignment=Alignment.Center) { Text("B",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black,color=MaterialTheme.colorScheme.onPrimary) }
  }
  Column(Modifier.weight(1f).clickable { notifications.showAbout() }) {
   Text("Buda",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
   Text("v$version · O aplikacji",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
  Box {
   TextButton(onClick={menu=true}) { Text("Menu") }
   DropdownMenu(menu,{menu=false}) {
    DropdownMenuItem(text={Text("Centrum zmian ocen")},onClick={menu=false;notifications.showHistory()})
    DropdownMenuItem(text={Text("Powiadomienia i przypomnienia")},onClick={menu=false;notifications.showSettings()})
    DropdownMenuItem(text={Text("O aplikacji i nowości")},onClick={menu=false;notifications.showAbout()})
    if(onLogout!=null) { HorizontalDivider();DropdownMenuItem(text={Text("Wyloguj")},onClick={menu=false;onLogout()}) }
   }
  }
 }
}
@Composable internal fun DashboardShortcuts(onHistory:()->Unit,onReminder:()->Unit) {
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
  ShortcutCard("Zmiany ocen","Twoja oś czasu",Modifier.weight(1f),onHistory)
  ShortcutCard("Przypomnienie","Zaplanuj spokojnie",Modifier.weight(1f),onReminder)
 }
}
@Composable private fun ShortcutCard(title:String,detail:String,modifier:Modifier,onClick:()->Unit) {
 Card(onClick=onClick,modifier=modifier,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)) {
  Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
   Text(title,style=MaterialTheme.typography.titleSmall,color=MaterialTheme.colorScheme.onSecondaryContainer)
   Text(detail,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSecondaryContainer)
  }
 }
}
@Composable internal fun LessonSpotlightCard(value:LessonSpotlight,onOpen:()->Unit) {
 Card(onClick=onOpen,modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
  Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer,MaterialTheme.colorScheme.secondaryContainer))).padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text(if(value.active) "TERAZ W PLANIE" else "NASTĘPNA LEKCJA",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
   Text(value.row.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
   val info=value.row.lesson!!
   Text(info.start.take(5)+" – "+info.end.take(5)+(if(info.room.isNotBlank()) " · sala "+info.room else ""),style=MaterialTheme.typography.bodyMedium)
   Text(if(value.active) "Do końca około ${value.minutes} min" else "Początek za około ${value.minutes} min",style=MaterialTheme.typography.titleSmall)
   if(value.active) LinearProgressIndicator(progress={value.progress},modifier=Modifier.fillMaxWidth())
   Text("Według pobranego planu · dotknij, aby zobaczyć szczegóły",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}
