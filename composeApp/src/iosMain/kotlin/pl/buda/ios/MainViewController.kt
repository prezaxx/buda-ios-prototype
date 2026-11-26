package pl.buda.ios
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIViewController
@OptIn(ExperimentalForeignApi::class)
fun MainViewController(vault:SessionVault,notifications:NotificationControls,dataVault:SessionVault,notificationController:()->UIViewController) =
 ComposeUIViewController {
  BudaApp(vault,notifications,dataVault,notificationPane={
   UIKitViewController(factory=notificationController,modifier=Modifier.fillMaxSize())
  })
 }
