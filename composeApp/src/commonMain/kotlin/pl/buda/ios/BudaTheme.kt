package pl.buda.ios
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable fun BudaTheme(content:@Composable ()->Unit) {
 val colors=if(isSystemInDarkTheme()) darkColorScheme(
  primary=Color(0xFF72DEC4),onPrimary=Color(0xFF00382F),primaryContainer=Color(0xFF124E43),onPrimaryContainer=Color(0xFFA0F2DA),
  secondary=Color(0xFFB6C7F5),onSecondary=Color(0xFF253252),secondaryContainer=Color(0xFF2C3B59),onSecondaryContainer=Color(0xFFDAE3FF),
  background=Color(0xFF0D1719),onBackground=Color(0xFFE2EEEB),surface=Color(0xFF182426),surfaceVariant=Color(0xFF2B3A3C),
  onSurface=Color(0xFFE2EEEB),onSurfaceVariant=Color(0xFFB7CAC6)
 ) else lightColorScheme(
  primary=Color(0xFF006B57),onPrimary=Color.White,primaryContainer=Color(0xFFC4F2E3),onPrimaryContainer=Color(0xFF00382F),
  secondary=Color(0xFF435B8B),onSecondary=Color.White,secondaryContainer=Color(0xFFE3EBFF),onSecondaryContainer=Color(0xFF243B65),
  background=Color(0xFFF3F7F6),onBackground=Color(0xFF142D29),surface=Color.White,surfaceVariant=Color(0xFFE5EFEC),
  onSurface=Color(0xFF142D29),onSurfaceVariant=Color(0xFF4E635E)
 )
 MaterialTheme(colorScheme=colors,shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(24.dp)),content=content)
}