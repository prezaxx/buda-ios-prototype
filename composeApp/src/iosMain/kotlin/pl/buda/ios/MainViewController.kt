package pl.buda.ios
import androidx.compose.ui.window.ComposeUIViewController
fun MainViewController(vault:SessionVault,notifications:NotificationControls) = ComposeUIViewController { BudaApp(vault,notifications) }
