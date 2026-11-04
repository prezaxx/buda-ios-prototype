import Foundation
import SwiftUI
import UIKit
import UserNotifications
import BackgroundTasks
import ComposeApp

private struct PollResult: Decodable {
    let status: String
    var snapshot: String = ""
    var changes: Int = 0
    var accounts: Int = 0
    var failures: Int = 0
    enum CodingKeys: String, CodingKey { case status, snapshot, changes, accounts, failures }
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        status = try c.decode(String.self, forKey: .status)
        snapshot = try c.decodeIfPresent(String.self, forKey: .snapshot) ?? ""
        changes = try c.decodeIfPresent(Int.self, forKey: .changes) ?? 0
        accounts = try c.decodeIfPresent(Int.self, forKey: .accounts) ?? 0
        failures = try c.decodeIfPresent(Int.self, forKey: .failures) ?? 0
    }
}

private final class PollCallback: NSObject, GradePollCallback {
    let action: (String) -> Void
    init(_ action: @escaping (String) -> Void) { self.action = action; super.init() }
    func completed(result: String) { action(result) }
}

@MainActor final class GradeNotificationService: NSObject, ObservableObject, UNUserNotificationCenterDelegate {
    static let shared = GradeNotificationService()
    static let taskID = "pl.buda.ios.prototype.grades"
    @Published private(set) var enabled = UserDefaults.standard.bool(forKey: "gradeChecksEnabled")
    @Published private(set) var status = "Sprawdzanie ocen nie zostało jeszcze uruchomione."
    @Published private(set) var running = false
    @Published private(set) var diagnostics = "Odczytuję ustawienia iOS…"
    @Published private(set) var lastResult = UserDefaults.standard.string(forKey: "gradeLastResult") ?? "Brak zakończonego sprawdzenia."
    @Published private(set) var lastBackground = UserDefaults.standard.string(forKey: "gradeLastBackground") ?? "iOS nie uruchomił jeszcze sprawdzania w tle."
    func refreshDiagnostics() {
        let background: String
        switch UIApplication.shared.backgroundRefreshStatus {
        case .available: background = "dostępne"
        case .denied: background = "wyłączone w ustawieniach"
        case .restricted: background = "ograniczone przez system"
        @unknown default: background = "nieznane"
        }
        let energy = ProcessInfo.processInfo.isLowPowerModeEnabled ? "włączone" : "wyłączone"
        let session = KeychainVault().read()
        let sessionState = session == nil ? "brak zapisanej sesji" : (session == "KEYCHAIN_UNAVAILABLE" ? "chwilowo niedostępna" : "zapisana")
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let auth: String
            switch settings.authorizationStatus {
            case .authorized: auth = "udzielona"
            case .provisional: auth = "ciche dostarczanie"
            case .denied: auth = "odmowa"
            case .notDetermined: auth = "jeszcze nie wybrano"
            case .ephemeral: auth = "tymczasowa"
            @unknown default: auth = "nieznana"
            }
            let banner = settings.alertSetting == .enabled ? "włączone" : "wyłączone lub niedostępne"
            let sound = settings.soundSetting == .enabled ? "włączony" : "wyłączony lub niedostępny"
            DispatchQueue.main.async {
                self.diagnostics = "Zgoda: \(auth)\nAlerty: \(banner)\nDźwięk: \(sound)\nOdświeżanie w tle: \(background)\nOszczędzanie energii: \(energy)\nSesja: \(sessionState)"
            }
        }
    }
    private let baseline = KeychainVault(account: "grade-baseline-v1")
    private lazy var poller = GradeNotificationPoller(vault: KeychainVault(), baselineVault: baseline)
    private var completion: ((Bool) -> Void)?
    private var generation = 0
    private var permissionEpoch = 0
    private var timer: Timer?
    private var lastCheck = Date.distantPast

    func configure() {
        UNUserNotificationCenter.current().delegate = self
        let registered = BGTaskScheduler.shared.register(forTaskWithIdentifier: Self.taskID, using: .main) { task in
            guard let refresh = task as? BGAppRefreshTask else { task.setTaskCompleted(success: false); return }
            DispatchQueue.main.async {
                self.lastBackground = Date().formatted(date: .abbreviated, time: .shortened)
                UserDefaults.standard.set(self.lastBackground, forKey: "gradeLastBackground")
                self.schedule()
                refresh.expirationHandler = { DispatchQueue.main.async { self.stopCheck() } }
                self.check { success in refresh.setTaskCompleted(success: success) }
            }
        }
        if !registered { status = "Sprawdzanie w tle jest niedostępne. Możesz sprawdzać oceny w otwartej aplikacji." }
    }
    func setEnabled(_ value: Bool) {
        if !value { disable(); return }
        let epoch = permissionEpoch
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
            DispatchQueue.main.async {
                guard self.permissionEpoch == epoch else { return }
                self.enabled = granted
                UserDefaults.standard.set(granted, forKey: "gradeChecksEnabled")
                if granted {
                    let sessionReady = KeychainVault().setBackgroundAccess(true)
                    let baselineReady = self.baseline.setBackgroundAccess(true)
                    guard sessionReady && baselineReady else { self.disable(); self.status = "Nie udało się udostępnić zapisanej sesji do sprawdzania w tle. Odblokuj telefon i spróbuj ponownie."; return }
                    self.status = "Powiadomienia lokalne włączone. Pierwsze pobranie zapisuje punkt odniesienia."; self.schedule(); self.becameActive() }
                else { self.status = "Brak zgody na powiadomienia. Zmień ją w ustawieniach iOS." }
            }
        }
    }
    func disable() {
        permissionEpoch += 1
        enabled = false; UserDefaults.standard.set(false, forKey: "gradeChecksEnabled")
        timer?.invalidate(); timer = nil; stopCheck()
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.taskID)
        UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
            let ids = requests.filter { $0.identifier.hasPrefix("grades-") || $0.identifier == "grade-notification-test" }.map(\.identifier)
            UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ids)
        }
        let sessionProtected = KeychainVault().setBackgroundAccess(false)
        let baselineProtected = baseline.setBackgroundAccess(false)
        status = sessionProtected && baselineProtected ? "Sprawdzanie ocen wyłączone." : "Sprawdzanie wyłączone; nie udało się przywrócić dostępu do sesji tylko po odblokowaniu. Odblokuj telefon i wyłącz ponownie."
    }
    func clear() {
        disable()
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
        if !baseline.clear() { status = "Wyłączono sprawdzanie. Nie udało się usunąć historii porównań z pęku kluczy." }
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
    }
    func becameActive() {
        refreshDiagnostics()
        guard enabled else { return }
        if Date().timeIntervalSince(lastCheck) >= 60 { check() }
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 300, repeats: true) { _ in DispatchQueue.main.async { self.check() } }
    }
    func leftForeground() { timer?.invalidate(); timer = nil; stopCheck(); schedule() }
    func schedule() {
        guard enabled else { return }
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.taskID)
        let request = BGAppRefreshTaskRequest(identifier: Self.taskID)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 30 * 60)
        do { try BGTaskScheduler.shared.submit(request) }
        catch { status = "iOS nie przyjął sprawdzenia w tle. Sprawdzanie po otwarciu aplikacji pozostaje dostępne." }
    }
    func check(completed: ((Bool) -> Void)? = nil) {
        guard enabled, !running else { completed?(false); return }
        running = true; completion = completed; generation += 1
        let token = generation
        status = "Sprawdzam oceny zapisanych uczniów…"
        poller.start(callback: PollCallback { raw in DispatchQueue.main.async {
            guard self.running, self.generation == token else { return }
            guard let data = raw.data(using: .utf8), let result = try? JSONDecoder().decode(PollResult.self, from: data) else {
                self.status = "Nie udało się odczytać wyniku sprawdzania."; self.finish(false); return
            }
            guard result.status == "OK" else {
                switch result.status {
                case "NO_SAVED_SESSION": self.status = "Zaloguj się i włącz Zapamiętaj sesję, aby sprawdzać oceny."
                case "DEVICE_LOCKED": self.status = "Odblokuj iPhone'a po uruchomieniu, aby udostępnić chronioną sesję do sprawdzania."
                case "TIMEOUT": self.status = "Sprawdzanie trwało zbyt długo. Spróbuj ponownie."
                default: self.status = "Nie udało się sprawdzić ocen: \(result.status). Otwórz Oceny i sprawdź sesję."
                }
                self.finish(false); return
            }
            @MainActor func save(_ delivered: Bool) {
                guard self.running, self.generation == token else { return }
                guard delivered else { self.status = "iOS nie przyjął banera. Szczegóły zmiany są zapisane w Historii zmian ocen."; self.finish(false); return }

                self.lastCheck = Date()
                self.status = "Sprawdzono kont: \(result.accounts). Zmiany ocen: \(result.changes). Błędy kont: \(result.failures). Ostatnio: \(Date().formatted(date: .omitted, time: .shortened))."
                self.finish(result.failures == 0)
            }
            guard self.baseline.write(value: result.snapshot) else { self.status = "Nie udało się zapisać szczegółów zmiany. Nie wysłano banera."; self.finish(false); return }
            if result.changes == 0 { save(true); return }
            self.post(id: "grades-" + UUID().uuidString, title: "Buda — zmiany ocen", body: "Nowe lub zmienione oceny: \(result.changes). Otwórz aplikację, aby zobaczyć szczegóły.") { ok in DispatchQueue.main.async { save(ok) } }
        } })
    }
    private func finish(_ success: Bool) {
        lastResult = Date().formatted(date: .abbreviated, time: .shortened) + " — " + status
        UserDefaults.standard.set(lastResult, forKey: "gradeLastResult")
        running = false; let callback = completion; completion = nil; callback?(success) }
    private func stopCheck() { generation += 1; poller.cancel(); if running { status = "Sprawdzanie przerwane przez zmianę stanu aplikacji lub wyłączenie funkcji. Ponowimy je przy następnej okazji."; finish(false) } }
    private func post(id: String, title: String, body: String, delay: TimeInterval = 1, done: @escaping (Bool) -> Void) {
        let content = UNMutableNotificationContent(); content.title = title; content.body = body; content.sound = .default
        let request = UNNotificationRequest(identifier: id, content: content, trigger: UNTimeIntervalNotificationTrigger(timeInterval: delay, repeats: false))
        UNUserNotificationCenter.current().add(request) { error in done(error == nil) }
    }
    func testBanner(delay: TimeInterval = 1) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { granted, _ in
            DispatchQueue.main.async {
                self.refreshDiagnostics()
                guard granted else { self.status = "Brak zgody iOS. Otwórz Ustawienia iOS poniżej."; return }
                self.post(id: "grade-notification-test", title: "Buda — test powiadomienia", body: "Baner lokalny działa. Ten test nie potwierdza pobierania ocen w tle ani serwerowego push.", delay: delay) { ok in
                    DispatchQueue.main.async {
                        self.status = ok ? (delay > 1 ? "Test za 15 sekund. Zablokuj teraz iPhone’a. Tryb skupienia i ustawienia mogą wyciszyć baner." : "Zlecono test banera i dźwięku.") : "iOS nie przyjął testu powiadomienia."
                    }
                }
            }
        }
    }
    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        if response.notification.request.identifier.hasPrefix("grades-") {
            DispatchQueue.main.async { NotificationHistoryRoute.shared.open = true }
        }
        completionHandler()
    }
    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .list, .sound])
    }
}

final class NativeNotificationControls: NSObject, NotificationControls {
    func showSettings() {
        DispatchQueue.main.async {
            let root = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.flatMap { $0.windows }.first { $0.isKeyWindow }?.rootViewController
            var presenter = root
            while let presented = presenter?.presentedViewController { presenter = presented }
            presenter?.present(UIHostingController(rootView: NotificationSettingsView()), animated: true)
        }
    }
    func planReminder(title: String, date: String) {
        DispatchQueue.main.async {
            let root = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.flatMap { $0.windows }.first { $0.isKeyWindow }?.rootViewController
            var presenter = root
            while let presented = presenter?.presentedViewController { presenter = presented }
            presenter?.present(UIHostingController(rootView: NavigationStack { ReminderListView(title: title, date: date) }), animated: true)
        }
    }
    func clear_() { DispatchQueue.main.async { GradeNotificationService.shared.clear() } }
}
@MainActor private struct NotificationSettingsView: View {
    @Environment(\.scenePhase) private var phase
    @ObservedObject private var service = GradeNotificationService.shared
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationStack {
            Form {
                Section("Historia") {
                    NavigationLink("Historia zmian ocen") { GradeHistoryView() }
                    Text("Szczegóły nowych alertów: uczeń, przedmiot, ocena i zmieniony opis lub komentarz.").font(.footnote)
                }
                Section("Powiadomienia o ocenach") {
                    Toggle("Włącz lokalne powiadomienia", isOn: Binding(get: { service.enabled }, set: { service.setEnabled($0) }))
                    Text("Sprawdzamy oceny zapisanych uczniów. Pierwsze pobranie zapisuje punkt odniesienia bez wysyłania starych ocen jako nowych.")
                    Button("Sprawdź teraz") { service.check() }.disabled(!service.enabled || service.running)
                    Button("Test banera i dźwięku") { service.testBanner() }
                    Button("Test za 15 sekund — zablokuj ekran") { service.testBanner(delay: 15) }
                    Text(service.status).font(.footnote)
                }
                Section("Moje przypomnienia") {
                    NavigationLink("Zaplanuj naukę lub zadanie") { ReminderListView() }
                    Text("Wybierz datę i godzinę. Zaplanowane przypomnienie nie wymaga pobierania ocen ani połączenia z internetem.").font(.footnote)
                }
                Section("Diagnostyka") {
                    Text(service.diagnostics).font(.footnote)
                    Button("Odśwież diagnostykę") { service.refreshDiagnostics() }
                    LabeledContent("Ostatnie uruchomienie w tle", value: service.lastBackground)
                    Text(service.lastResult).font(.footnote)
                }
                Section("Jak to działa") {
                    Text("Po otwarciu i co 5 minut w aktywnej aplikacji. W tle iOS sam decyduje, czy i kiedy uruchomi sprawdzenie. Nie gwarantujemy terminów ani pracy po wymuszonym zamknięciu aplikacji.")
                    Text("Po włączeniu sesja i historia porównań pozostają w pęku kluczy tego iPhone’a, ale są dostępne dla Budy także pod blokadą po pierwszym odblokowaniu od restartu. Wyłączenie przywraca dostęp tylko po odblokowaniu. Włącz Zapamiętaj sesję i odświeżanie w tle w iOS.")
                    Text("To powiadomienia lokalne, nie APNs push. Serwerowy push nie jest uruchomiony w tej wersji.").fontWeight(.semibold)
                    Button("Ustawienia iOS") { if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) } }
                }
            }.onAppear { service.refreshDiagnostics() }.onChange(of: phase) { value in if value == .active { service.refreshDiagnostics() } }.navigationTitle("Dzwonek").toolbar { ToolbarItem(placement: .confirmationAction) { Button("Gotowe") { dismiss() } } }
        }
    }
}