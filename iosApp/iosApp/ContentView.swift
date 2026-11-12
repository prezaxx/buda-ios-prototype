import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(vault: KeychainVault(), notifications: NativeNotificationControls(), dataVault: KeychainVault(account: "offline-and-drafts-v1"))
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@MainActor struct ContentView: View {
    @ObservedObject private var router = AppSheetRouter.shared
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .sheet(item: Binding(
                get: { router.active },
                set: { if $0 == nil { router.requestDismiss() } }
            ), onDismiss: { router.didDismiss() }) { request in
                switch request.destination {
                case .history(let batch):
                    NavigationStack {
                        GradeHistoryView().onAppear { NotificationHistoryRoute.shared.batch = batch }
                    }
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
