import SwiftUI
import UIKit

@MainActor final class BudaAppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        GradeNotificationService.shared.configure()
        return true
    }
}
@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(BudaAppDelegate.self) private var delegate
    @Environment(\.scenePhase) private var phase
    var body: some Scene {
        WindowGroup { ContentView() }
            .onChange(of: phase) { value in
                if value == .active { GradeNotificationService.shared.becameActive() }
                else if value == .background { GradeNotificationService.shared.leftForeground() }
            }
    }
}