package pl.buda.ios
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable fun BudaTheme(content:@Composable ()->Unit) {
 val colors=if(isSystemInDarkTheme()) darkColorScheme(
  primary=Color(0xFF80D7C5),onPrimary=Color(0xFF00382F),primaryContainer=Color(0xFF175347),onPrimaryContainer=Color(0xFFACF1DF),
  secondary=Color(0xFFB9C9EF),background=Color(0xFF101917),surface=Color(0xFF16221F),surfaceVariant=Color(0xFF263A34),onSurface=Color(0xFFE0EDE7)
 ) else lightColorScheme(
  primary=Color(0xFF146B58),onPrimary=Color.White,primaryContainer=Color(0xFFD8F1E7),onPrimaryContainer=Color(0xFF123E32),
  secondary=Color(0xFF45618C),background=Color(0xFFF3F7F4),surface=Color(0xFFFCFEFC),surfaceVariant=Color(0xFFE6EFE9),onSurface=Color(0xFF182C23)
 )
 MaterialTheme(colorScheme=colors,shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(18.dp),large=RoundedCornerShape(24.dp)),content=content)
}