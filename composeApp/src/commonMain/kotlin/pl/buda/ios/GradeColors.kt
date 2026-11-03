package pl.buda.ios
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.pow

internal fun numericGrade(value:String):Int? {
 val normalized=value.trim().replace('−','-')
 return Regex("^[+-]?([1-6])[+-]?$").matchEntire(normalized)?.groupValues?.get(1)?.toIntOrNull()
}
internal data class GradePalette(val background:Color,val foreground:Color)
internal fun gradePalette(value:String,dark:Boolean,teacherRgb:Int?=null):GradePalette {
 if(teacherRgb!=null && teacherRgb in 0..0xFFFFFF) {
  val color=Color(0xFF000000L or teacherRgb.toLong())
  fun linear(v:Float)=if(v<=0.04045f) v/12.92 else ((v+0.055)/1.055).pow(2.4)
  val luminance=0.2126*linear(color.red)+0.7152*linear(color.green)+0.0722*linear(color.blue)
  val ink=if((luminance+0.05)/0.05 >= 1.05/(luminance+0.05)) Color.Black else Color.White
  return GradePalette(color,ink)
 }
 val n=numericGrade(value)
 val light=listOf(0xFFE6EAF0,0xFFFFDAD6,0xFFFFDFC5,0xFFFFEDAD,0xFFDCEBA7,0xFFBAEDC5,0xFF9DE5C3)
 val ink=listOf(0xFF344054,0xFF8C1D18,0xFF763D00,0xFF604B00,0xFF344C00,0xFF12552C,0xFF004C35)
 val background=listOf(0xFF303B49,0xFF651A1A,0xFF61330F,0xFF544400,0xFF354400,0xFF174D2B,0xFF00513A)
 val i=n ?: 0
 return GradePalette(Color(if(dark) background[i] else light[i]),Color(if(dark) light[i] else ink[i]))
}
@Composable internal fun GradeBadge(value:String,modifier:Modifier=Modifier,teacherRgb:Int?=null,onClick:(()->Unit)?=null) {
 val colors=gradePalette(value,isSystemInDarkTheme(),teacherRgb)
 if(onClick!=null) Surface(onClick=onClick,modifier=modifier.heightIn(min=48.dp),shape=MaterialTheme.shapes.small,color=colors.background,contentColor=colors.foreground) {
  Box(Modifier.padding(horizontal=8.dp,vertical=10.dp),contentAlignment=androidx.compose.ui.Alignment.Center) { Text(value,style=MaterialTheme.typography.titleMedium) }
 } else Surface(modifier=modifier.heightIn(min=48.dp),shape=MaterialTheme.shapes.small,color=colors.background,contentColor=colors.foreground) {
  Box(Modifier.padding(horizontal=12.dp,vertical=10.dp),contentAlignment=androidx.compose.ui.Alignment.Center) { Text(value,style=MaterialTheme.typography.titleLarge) }
 }
}
@Composable internal fun GradeLegend() {
 Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { (6 downTo 1).forEach { GradeBadge(it.toString(),Modifier.weight(1f)) } }
  Text("Najpierw kolor z dziennika nadany przez nauczyciela. Powyżej skala zastępcza, używana tylko bez koloru. Wpisy tekstowe bez koloru są neutralne.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}