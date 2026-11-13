package pl.buda.ios
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate

@Composable internal fun MyDayHero(today:LocalDate,student:String) {
 val palette=MaterialTheme.colorScheme
 Surface(shape=MaterialTheme.shapes.large,color=palette.primaryContainer) {
  Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(palette.primaryContainer,palette.secondaryContainer)))) {
   Canvas(Modifier.matchParentSize()) {
    drawCircle(palette.primary.copy(alpha=0.06f),radius=size.width*0.35f,center=Offset(size.width,size.height*0.2f))
    drawCircle(palette.primary.copy(alpha=0.05f),radius=size.width*0.23f,center=Offset(size.width*0.95f,size.height*0.9f))
   }
   Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text("TWÓJ DZIEŃ, TWÓJ RYTM",style=MaterialTheme.typography.labelSmall,color=palette.onPrimaryContainer)
    Text("Mój dzień",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.ExtraBold,color=palette.onPrimaryContainer)
    Text(dayTitle(today),style=MaterialTheme.typography.titleMedium,color=palette.onPrimaryContainer)
    if(student.isNotBlank()) Text(student,style=MaterialTheme.typography.bodyMedium,color=palette.onPrimaryContainer)
    Text("Plan, ważne terminy i chwila na oddech.",style=MaterialTheme.typography.bodySmall,color=palette.onPrimaryContainer)
   }
  }
 }
}
@Composable internal fun DayLoadingCards() {
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("Układam Twój dzień…",style=MaterialTheme.typography.labelLarge)
  repeat(3) { index ->
   Surface(Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surface) {
    Row(Modifier.padding(18.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
     Surface(Modifier.size(44.dp),shape=MaterialTheme.shapes.small,color=MaterialTheme.colorScheme.surfaceVariant) {}
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(9.dp)) {
      Surface(Modifier.fillMaxWidth(if(index==1) 0.6f else 0.8f).height(14.dp),shape=MaterialTheme.shapes.small,color=MaterialTheme.colorScheme.surfaceVariant) {}
      Surface(Modifier.fillMaxWidth(0.45f).height(10.dp),shape=MaterialTheme.shapes.small,color=MaterialTheme.colorScheme.surfaceVariant) {}
     }
    }
   }
  }
 }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun GradeDetailSheet(grade:GradeRow,onClose:()->Unit) {
 ModalBottomSheet(onDismissRequest=onClose,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
  Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
    GradeBadge(grade.content,Modifier.widthIn(min=70.dp,max=110.dp),teacherRgb=grade.teacherRgb)
    Column(Modifier.weight(1f)) {
     Text("SZCZEGÓŁY OCENY",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
     Text(grade.subject,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
    }
   }
   HorizontalDivider()
   SelectionContainer {
    Column(verticalArrangement=Arrangement.spacedBy(18.dp)) {
     DetailField("Opis",grade.description.ifBlank { "Brak opisu" })
     DetailField("Komentarz nauczyciela",grade.comment.ifBlank { "Brak komentarza" })
     if(grade.createdAt.isNotBlank()) DetailField("Data w dzienniku",grade.createdAt)
    }
   }
   FilledTonalButton(onClick=onClose,modifier=Modifier.fillMaxWidth()) { Text("Gotowe") }
  }
 }
}
@Composable private fun DetailField(label:String,value:String) {
 Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
  Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Text(value,style=MaterialTheme.typography.bodyLarge)
 }
}
@Composable internal fun GradeChronologyCard(grade:GradeRow,onOpen:()->Unit) {
 Card(onClick=onOpen,modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
  Row(Modifier.padding(18.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically) {
   GradeBadge(grade.content,Modifier.widthIn(min=64.dp,max=100.dp),teacherRgb=grade.teacherRgb)
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
    Text(grade.subject,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
    Text(grade.description.ifBlank { "Zobacz szczegóły oceny" },maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    if(grade.createdAt.isNotBlank()) Text(grade.createdAt.take(10),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
   }
   Text("›",style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.primary)
  }
 }
}
