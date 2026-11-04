import SwiftUI
import UserNotifications

private struct PlannedReminder: Identifiable {
    let id: String
    let label: String
    let date: Date?
}
@MainActor private final class ReminderStore: ObservableObject {
    @Published var reminders: [PlannedReminder] = []
    @Published var busy = false
    @Published var message = ""
    func refresh() {
        UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
            let items = requests.filter { $0.identifier.hasPrefix("reminder-") }.map {
                PlannedReminder(id: $0.identifier, label: $0.content.userInfo["label"] as? String ?? "Przypomnienie", date: ($0.trigger as? UNCalendarNotificationTrigger)?.nextTriggerDate())
            }.sorted { ($0.date ?? .distantFuture) < ($1.date ?? .distantFuture) }
            DispatchQueue.main.async { self.reminders = items }
        }
    }
    func remove(_ id: String) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [id])
        reminders.removeAll { $0.id == id }
    }
    func add(label: String, date: Date, reveal: Bool) {
        let title = label.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !title.isEmpty, title.count <= 100 else { message = "Wpisz tytuł od 1 do 100 znaków."; return }
        guard date.timeIntervalSinceNow >= 60 else { message = "Wybierz termin co najmniej minutę od teraz."; return }
        guard !busy else { return }; busy = true
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { granted, _ in
            guard granted else { DispatchQueue.main.async { self.busy = false; self.message = "Włącz powiadomienia Budy w ustawieniach iOS." }; return }
            UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
                guard requests.count < 40 else { DispatchQueue.main.async { self.busy = false; self.message = "Masz już wiele przypomnień. Usuń część przed dodaniem kolejnych." }; return }
                guard date.timeIntervalSinceNow > 0 else { DispatchQueue.main.async { self.busy = false; self.message = "Wybrany termin już minął." }; return }
                let content = UNMutableNotificationContent()
                content.title = "Buda — przypomnienie"
                content.body = reveal ? title : "Czas na zaplanowane zadanie."
                content.sound = .default
                content.userInfo = ["label": title]
                let parts = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)
                let request = UNNotificationRequest(identifier: "reminder-" + UUID().uuidString, content: content, trigger: UNCalendarNotificationTrigger(dateMatching: parts, repeats: false))
                UNUserNotificationCenter.current().add(request) { error in
                    DispatchQueue.main.async { self.busy = false; self.message = error == nil ? "Przypomnienie zaplanowane. Wyświetlenie zależy od ustawień iOS i trybu skupienia." : "Nie udało się zaplanować przypomnienia."; self.refresh() }
                }
            }
        }
    }
}
@MainActor struct ReminderListView: View {
    @Environment(\.dismiss) private var dismiss
    init(title: String = "", date: String = "") {
        _title = State(initialValue: String(title.prefix(100)))
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        let due = formatter.date(from: date)
        let evening = due.flatMap { Calendar.current.date(byAdding: .day, value: -1, to: $0) }.flatMap { Calendar.current.date(bySettingHour: 18, minute: 0, second: 0, of: $0) }
        _date = State(initialValue: evening.flatMap { $0.timeIntervalSinceNow > 60 ? $0 : nil } ?? Date().addingTimeInterval(3600))
    }
    @StateObject private var store = ReminderStore()
    @State private var title = ""
    @State private var date = Date().addingTimeInterval(3600)
    @State private var reveal = false
    var body: some View {
        Form {
            Section("Nowe przypomnienie") {
                TextField("Np. powtórka z matematyki", text: $title)
                DatePicker("Termin", selection: $date, in: Date()..., displayedComponents: [.date, .hourAndMinute])
                Toggle("Pokaż tytuł na ekranie blokady", isOn: $reveal)
                Button("Zaplanuj przypomnienie") { store.add(label: title, date: date, reveal: reveal) }.disabled(store.busy || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                if !store.message.isEmpty { Text(store.message).font(.footnote) }
            }
            Section("Zaplanowane") {
                if store.reminders.isEmpty { Text("Brak oczekujących przypomnień.").foregroundStyle(.secondary) }
                ForEach(store.reminders) { item in
                    VStack(alignment: .leading, spacing: 6) {
                        Text(item.label).font(.headline)
                        if let date = item.date { Text(date.formatted(date: .abbreviated, time: .shortened)).font(.subheadline) }
                        Button("Usuń", role: .destructive) { store.remove(item.id) }
                    }
                }
            }
            Section {
                Text("Przypomnienia są zapisane w systemie tego iPhone’a. Wylogowanie lub wyłączenie zapamiętywania sesji usuwa je. Lista pokazuje tylko przyszłe przypomnienia; dostarczone znajdziesz w centrum powiadomień.").font(.footnote)
            }
        }.navigationTitle("Moje przypomnienia").toolbar { ToolbarItem(placement: .confirmationAction) { Button("Gotowe") { dismiss() } } }.onAppear { store.refresh() }
    }
}
