import Foundation

@main struct AppSheetRouterTests {
    @MainActor static func main() {
        let router = AppSheetRouter()
        for destination in [AppSheetDestination.history(batch: nil), .settings, .about, .reminder(title: "Test", date: "2026-11-12")] {
            router.present(destination)
            precondition(router.active?.destination == destination)
            let id = router.active?.id
            router.present(destination)
            precondition(router.active?.id == id, "Repeated tap must not stack another sheet")
            router.requestDismiss()
            precondition(router.active == nil)
            router.didDismiss()
        }
        router.present(.settings)
        router.present(.history(batch: "notification-batch"))
        precondition(router.active == nil, "Dismiss current sheet before opening another")
        router.present(.about)
        router.didDismiss()
        precondition(router.active?.destination == .about, "Latest intent survives dismissal")
        router.requestDismiss()
        router.present(.history(batch: "received-while-closing"))
        precondition(router.active == nil)
        router.didDismiss()
        precondition(router.active?.destination == .history(batch: "received-while-closing"))
        router.requestDismiss()
        router.didDismiss()
        router.present(.settings)
        precondition(router.active?.destination == .settings)
        print("AppSheetRouter: all destinations, repeated taps, queued transitions and notification routing passed.")
    }
}
