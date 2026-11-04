import SwiftUI

@MainActor final class NotificationHistoryRoute: ObservableObject {
    static let shared = NotificationHistoryRoute()
    @Published var open = false
}
private struct HistoricalGrade: Decodable {
    var content: String
    var subject: String
    var description: String
    var comment: String
}
private struct GradeHistoryEntry: Decodable, Identifiable {
    let id: String
    let student: String
    let period: String
    let at: String
    let before: HistoricalGrade?
    let after: HistoricalGrade
}
private struct HistoryEnvelope: Decodable { let history: [GradeHistoryEntry]? }
@MainActor struct GradeHistoryView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var entries: [GradeHistoryEntry] = []
    @State private var error: String?
    private func load() {
        guard let text = KeychainVault(account: "grade-baseline-v1").read() else { entries = []; return }
        guard let data = text.data(using: .utf8), let decoded = try? JSONDecoder().decode(HistoryEnvelope.self, from: data) else { error = "Nie udało się odczytać historii. Odblokuj iPhone’a i spróbuj ponownie."; return }
        entries = decoded.history ?? []; error = nil
    }
    var body: some View {
        List {
            if let error { Text(error).foregroundStyle(.red); Button("Ponów") { load() } }
            if entries.isEmpty && error == nil { Text("Brak zapisanych zmian. Historia obejmuje alerty wykryte od wersji 0.9. Starsze banery nie zawierają szczegółów.") }
            ForEach(entries) { entry in
                VStack(alignment: .leading, spacing: 8) {
                    Text(entry.student).font(.headline)
                    Text(entry.after.subject).font(.subheadline).foregroundStyle(.secondary)
                    if let old = entry.before {
                        if old.content != entry.after.content { Text("Ocena: \(old.content) → \(entry.after.content)").font(.title3.bold()) }
                        else { Text("Ocena \(entry.after.content) — zmieniono szczegóły").font(.title3.bold()) }
                        if old.subject != entry.after.subject { Text("Przedmiot: \(old.subject) → \(entry.after.subject)") }
                        if old.description != entry.after.description { Text("Opis wcześniej: \(old.description.isEmpty ? "brak" : old.description)"); Text("Opis teraz: \(entry.after.description.isEmpty ? "brak" : entry.after.description)") }
                        if old.comment != entry.after.comment { Text("Komentarz wcześniej: \(old.comment.isEmpty ? "brak" : old.comment)");Text("Komentarz teraz: \(entry.after.comment.isEmpty ? "brak" : entry.after.comment)") }
                    } else {
                        Text("Nowa ocena: \(entry.after.content)").font(.title3.bold())
                        if !entry.after.description.isEmpty { Text(entry.after.description) }
                        if !entry.after.comment.isEmpty { Text(entry.after.comment) }
                    }
                    Text("Semestr: \(entry.period)").font(.caption)
                    Text("Wykryto: " + entry.at.prefix(19).replacingOccurrences(of: "T", with: " ") + " UTC").font(.caption).foregroundStyle(.secondary)
                }.padding(.vertical, 5).textSelection(.enabled)
            }
            Section { Text("Ostatnie 100 wykrytych zmian. To zapis z chwili sprawdzenia, nie aktualny widok dziennika. Zmiana komentarza lub opisu także może wywołać alert. Wylogowanie usuwa historię.").font(.footnote) }
        }.navigationTitle("Zmiany ocen").toolbar { ToolbarItem(placement: .confirmationAction) { Button("Gotowe") { dismiss() } } }.onAppear { load() }
    }
}
