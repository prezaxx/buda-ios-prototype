import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    var appearance: String = "system"
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(vault: KeychainVault(), notifications: NativeNotificationControls(), dataVault: KeychainVault(account: "offline-and-drafts-v1"))
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        uiViewController.overrideUserInterfaceStyle = appearance == "dark" ? .dark : (appearance == "light" ? .light : .unspecified)
    }
}

@MainActor struct ContentView: View {
    @AppStorage("budaAppearance") private var appearance = "system"
    @ObservedObject private var router = AppSheetRouter.shared
    var body: some View {
        ComposeView(appearance: appearance)
            .ignoresSafeArea()
            .preferredColorScheme(appearance == "dark" ? .dark : (appearance == "light" ? .light : nil))
            .sheet(item: Binding(
                get: { router.active },
                set: { if $0 == nil { router.requestDismiss() } }
            ), onDismiss: { router.didDismiss() }) { request in
                switch request.destination {
                case .history(let batch):
                    NavigationStack {
                        GradeHistoryView().onAppear { NotificationHistoryRoute.shared.batch = batch }
                    }
                case .cloudHistory(let batch):
                    NavigationStack { CloudNotificationView(batch: batch) }
                case .settings:
                    NotificationSettingsView()
                case .about:
                    NavigationStack { AboutView() }
                case .reminder(let title, let date):
                    NavigationStack { ReminderListView(title: title, date: date) }
                }
            }
    }
}
