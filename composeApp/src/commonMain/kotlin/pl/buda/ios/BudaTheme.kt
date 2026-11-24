package pl.buda.ios
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable fun BudaTheme(content:@Composable ()->Unit) {
 val colors=if(isSystemInDarkTheme()) darkColorScheme(
  primary=Color(0xFFA8B3FF),onPrimary=Color(0xFF1F2359),primaryContainer=Color(0xFF30376B),onPrimaryContainer=Color(0xFFE0E3FF),
  secondary=Color(0xFF7DE0D0),onSecondary=Color(0xFF00382F),secondaryContainer=Color(0xFF164D44),onSecondaryContainer=Color(0xFFBBF2E5),
  background=Color(0xFF0D101B),onBackground=Color(0xFFEBEDFA),surface=Color(0xFF191D2C),surfaceVariant=Color(0xFF2B3045),
  onSurface=Color(0xFFEBEDFA),onSurfaceVariant=Color(0xFFBDC3DB)
 ) else lightColorScheme(
  primary=Color(0xFF4B52B6),onPrimary=Color.White,primaryContainer=Color(0xFFE3E5FF),onPrimaryContainer=Color(0xFF1F2359),
  secondary=Color(0xFF006C60),onSecondary=Color.White,secondaryContainer=Color(0xFFC8F2E6),onSecondaryContainer=Color(0xFF00382F),
  background=Color(0xFFF4F5FC),onBackground=Color(0xFF1A2039),surface=Color.White,surfaceVariant=Color(0xFFE8EBF6),
  onSurface=Color(0xFF1A2039),onSurfaceVariant=Color(0xFF515A75)
 )
 MaterialTheme(colorScheme=colors,shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(22.dp),large=RoundedCornerShape(28.dp)),content=content)
}