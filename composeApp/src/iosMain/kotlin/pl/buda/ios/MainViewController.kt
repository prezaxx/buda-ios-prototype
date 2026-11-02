package pl.buda.ios
import androidx.compose.ui.window.ComposeUIViewController
fun MainViewController(vault:SessionVault) = ComposeUIViewController { BudaApp(vault) }
