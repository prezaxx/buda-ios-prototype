import Foundation
import Combine

enum AppSheetDestination: Equatable {
    case history(batch: String?)
    case settings
    case about
    case reminder(title: String, date: String)
}
struct AppSheetRequest: Identifiable {
    let id = UUID()
    let destination: AppSheetDestination
}
/// The app's SwiftUI root owns presentation; Compose popup windows never present UIKit controllers.
@MainActor final class AppSheetRouter: ObservableObject {
    static let shared = AppSheetRouter()
    @Published private(set) var active: AppSheetRequest?
    private var pending: AppSheetDestination?
    private var dismissing = false

    func present(_ destination: AppSheetDestination) {
        if dismissing { pending = destination; return }
        if let current = active {
            guard current.destination != destination else { return }
            pending = destination
            requestDismiss()
        } else {
            active = AppSheetRequest(destination: destination)
        }
    }
    func requestDismiss() {
        guard active != nil else { return }
        dismissing = true
        active = nil
    }
    func didDismiss() {
        dismissing = false
        if let next = pending {
            pending = nil
            active = AppSheetRequest(destination: next)
        }
    }
}
