package pl.buda.ios
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable fun BudaTheme(content:@Composable ()->Unit) {
 val colors=if(isSystemInDarkTheme()) darkColorScheme(
  primary=Color(0xFFB5C4FF),onPrimary=Color(0xFF16265F),primaryContainer=Color(0xFF293B76),onPrimaryContainer=Color(0xFFDEE4FF),
  secondary=Color(0xFFB9C9EF),background=Color(0xFF10131D),surface=Color(0xFF191E2C),surfaceVariant=Color(0xFF2B3245),onSurface=Color(0xFFE4E8F4)
 ) else lightColorScheme(
  primary=Color(0xFF4359B8),onPrimary=Color.White,primaryContainer=Color(0xFFE4E9FF),onPrimaryContainer=Color(0xFF1C2B64),
  secondary=Color(0xFF586588),background=Color(0xFFF3F5FB),surface=Color(0xFFFFFFFF),surfaceVariant=Color(0xFFE8ECF5),onSurface=Color(0xFF1B2438)
 )
 MaterialTheme(colorScheme=colors,shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(24.dp)),content=content)
}