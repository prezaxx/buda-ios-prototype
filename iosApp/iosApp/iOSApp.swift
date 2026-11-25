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
        WindowGroup {
            ContentView().onOpenURL { url in
                guard url.scheme == "budadiary", url.host == "notifications" else { return }
                let raw = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems?.first(where: { $0.name == "batch" })?.value
                let batch = raw.flatMap { UUID(uuidString: $0) != nil ? $0 : nil }
                AppSheetRouter.shared.present(.cloudHistory(batch: batch))
            }
        }
            .onChange(of: phase) { value in
                if value == .active { GradeNotificationService.shared.becameActive() }
                else if value == .background { GradeNotificationService.shared.leftForeground() }
            }
    }
}