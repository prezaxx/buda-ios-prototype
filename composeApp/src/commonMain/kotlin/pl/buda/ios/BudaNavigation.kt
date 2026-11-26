package pl.buda.ios
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable internal fun BudaNavigation(selected:Section,order:List<Section> = defaultMenuOrder,onOrder:(List<Section>)->Unit={},onSection:(Section)->Unit) {
 val unread by NativeNavigation.unreadCount.collectAsState()
 var editing by remember { mutableStateOf(false) }
 var more by remember { mutableStateOf(false) }
 val main=listOf(Section.HOME,Section.SCHEDULE,Section.NOTIFICATIONS,Section.MESSAGES)
 if(more) AlertDialog(onDismissRequest={more=false},title={Text("Twój dziennik")},text={
  Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
   TextButton(onClick={editing=!editing}) { Text(if(editing) "Zakończ dostosowanie" else "Dostosuj menu") }
   order.forEachIndexed { index,item ->
    Row {
     FilledTonalButton(onClick={more=false;onSection(item)},modifier=Modifier.weight(1f)) { Text(item.label) }
     if(editing) {
      TextButton(enabled=index>0,onClick={val next=order.toMutableList();val previous=next[index-1];next[index-1]=item;next[index]=previous;onOrder(next)}) { Text("↑") }
      TextButton(enabled=index<order.lastIndex,onClick={val next=order.toMutableList();val after=next[index+1];next[index+1]=item;next[index]=after;onOrder(next)}) { Text("↓") }
     }
    }
   }
  }
 },confirmButton={TextButton(onClick={more=false}){Text("Zamknij")}})
 NavigationBar(modifier=Modifier.padding(bottom=4.dp).clip(MaterialTheme.shapes.large),containerColor=MaterialTheme.colorScheme.surface,tonalElevation=0.dp,windowInsets=WindowInsets(0,0,0,0)) {
  (main+listOf(null)).forEachIndexed { index,section ->
   val label=when(section) { Section.SCHEDULE->"Plan";Section.MESSAGES->"Wiadomości";null->"Więcej";else->section.label }
   NavigationBarItem(selected=if(section==null) selected !in main else selected==section,
    onClick={if(section==null) more=true else onSection(section)},label={Text(label,maxLines=1,fontSize=10.sp)},icon={BadgedBox(badge={if(section==Section.NOTIFICATIONS && unread>0) Badge { Text(if(unread>99) "99+" else unread.toString()) }}) { NavGlyph(index) }})
  }
 }
}
@Composable private fun NavGlyph(index:Int) {
 val color=LocalContentColor.current
 Canvas(Modifier.size(23.dp)) {
  val w=size.width;val h=size.height;val stroke=Stroke(1.8.dp.toPx())
  fun line(x:Float,y:Float,xx:Float,yy:Float)=drawLine(color,Offset(w*x,h*y),Offset(w*xx,h*yy),stroke.width)
  when(index) {
   0->{line(.1f,.45f,.5f,.1f);line(.5f,.1f,.9f,.45f);line(.23f,.36f,.23f,.88f);line(.77f,.36f,.77f,.88f);line(.23f,.88f,.77f,.88f);line(.5f,.58f,.5f,.88f)}
   1->{drawRect(color,Offset(w*.14f,h*.2f),Size(w*.72f,h*.68f),style=stroke);line(.14f,.4f,.86f,.4f);line(.32f,.1f,.32f,.3f);line(.68f,.1f,.68f,.3f);line(.3f,.6f,.7f,.6f)}
   2->{drawArc(color,180f,180f,false,Offset(w*.2f,h*.15f),Size(w*.6f,h*.7f),style=stroke);line(.2f,.5f,.17f,.78f);line(.8f,.5f,.83f,.78f);line(.17f,.78f,.83f,.78f);drawCircle(color,w*.07f,Offset(w*.5f,h*.9f))}
   3->{drawRect(color,Offset(w*.1f,h*.2f),Size(w*.8f,h*.6f),style=stroke);line(.1f,.2f,.5f,.55f);line(.5f,.55f,.9f,.2f)}
   else->listOf(.2f,.5f,.8f).forEach { drawCircle(color,w*.07f,Offset(w*it,h*.5f)) }
  }
 }
}
