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
    @ObservedObject private var history = NotificationHistoryRoute.shared
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .sheet(isPresented: $history.open) { NavigationStack { GradeHistoryView() } }
    }
}



