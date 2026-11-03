package pl.buda.ios
import kotlin.math.pow
import kotlin.test.*
class GradeColorsTest {
 @Test fun numericGradesAndModifiersKeepTheirFamily() {
  for(n in 1..6) for(value in listOf("$n","$n+","$n-","+$n","-$n"," $n ")) assertEquals(n,numericGrade(value),value)
 }
 @Test fun textPercentagesAndOtherScalesStayNeutral() {
  listOf("np","zw","bz","zal","100%","0","7","10","4,5","4/6","").forEach { assertNull(numericGrade(it),it) }
 }
 @Test fun redAndGreenAreDistinctAndTextIsNotAOne() {
  assertNotEquals(gradePalette("1",false),gradePalette("6",false))
  assertNotEquals(gradePalette("np",false),gradePalette("1",false))
  assertEquals(gradePalette("5",false),gradePalette("5+",false))
 }
 @Test fun everyGradeHasReadableContrastInBothThemes() {
  fun channel(v:Float):Double=if(v<=0.04045f) v/12.92 else ((v+0.055)/1.055).pow(2.4)
  fun luminance(c:androidx.compose.ui.graphics.Color)=0.2126*channel(c.red)+0.7152*channel(c.green)+0.0722*channel(c.blue)
  for(dark in listOf(false,true)) for(value in listOf("np","1","2","3","4","5","6")) {
   val p=gradePalette(value,dark);val a=luminance(p.background);val b=luminance(p.foreground)
   val contrast=(maxOf(a,b)+0.05)/(minOf(a,b)+0.05)
   assertTrue(contrast>=4.5,"$value dark=$dark contrast=$contrast")
  }
 }
}