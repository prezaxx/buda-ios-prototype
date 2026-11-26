package pl.buda.ios
import kotlinx.coroutines.flow.MutableStateFlow
object NativeNavigation {
 val unreadCount=MutableStateFlow(0)
 fun setUnreadCount(count:Int) { unreadCount.value=count.coerceAtLeast(0) }
 val notificationRequests=MutableStateFlow(0)
 fun openNotifications() { notificationRequests.value+=1 }
}
