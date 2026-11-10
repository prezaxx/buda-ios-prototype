import SwiftUI

@MainActor final class NotificationHistoryRoute: ObservableObject {
    static let shared = NotificationHistoryRoute()
    @Published var open = false
    @Published var batch: String?
}
@MainActor struct GradeHistoryView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var service = GradeNotificationService.shared
    @ObservedObject private var route = NotificationHistoryRoute.shared
    @State private var entries: [GradeHistoryEntry] = []
    @State private var reads = HistoryReadState()
    @State private var error: String?
    @State private var query = ""
    @State private var student = ""
    @State private var unreadOnly = false
    @State private var selected: GradeHistoryEntry?
    @State private var ready = false
    private let readVault = KeychainVault(account: "grade-history-read-v1")
    private var visible: [GradeHistoryEntry] {
        filteredGradeHistory(entries, readIDs: reads.ids, student: student.isEmpty ? nil : student,
                             unreadOnly: unreadOnly, query: query, batch: route.batch)
    }
    private var students: [GradeHistoryEntry] {
        var seen = Set<String>()
        return entries.filter { seen.insert($0.studentFilterKey).inserted }.sorted { $0.student < $1.student }
    }
    private func load() {
        do {
            let raw = KeychainVault(account: "grade-baseline-v1").read()
            guard raw != "KEYCHAIN_UNAVAILABLE" else { throw HistoryError.locked }
            entries = try raw.map { try HistoryEnvelope.entries(from: $0) } ?? []
            let readRaw = readVault.read()
            guard readRaw != "KEYCHAIN_UNAVAILABLE" else { throw HistoryError.locked }
            reads = try readRaw.map { try JSONDecoder().decode(HistoryReadState.self, from: Data($0.utf8)) } ?? HistoryReadState()
            error = nil; ready = true
        } catch {
            self.error = "Nie udało się odczytać historii. Odblokuj iPhone’a i spróbuj ponownie."
            ready = false
        }
    }
    private func mark(_ items: [GradeHistoryEntry], read: Bool) {
        guard ready else { return }
        var next = reads
        next.mark(items, read: read, retained: entries)
        guard let data = try? JSONEncoder().encode(next), let text = String(data: data, encoding: .utf8),
              readVault.write(value: text) else {
            error = "Nie udało się zapisać oznaczenia. Spróbuj ponownie po odblokowaniu telefonu."; return
        }
        reads = next; error = nil
    }
    var body: some View {
        List {
            Section {
                HStack(spacing: 12) {
                    Image(systemName: "bell.badge.fill").font(.largeTitle).foregroundStyle(.teal)
                    VStack(alignment: .leading) {
                        Text("Centrum zmian").font(.title2.bold())
                        Text("Nieprzeczytane: \(entries.filter { !reads.ids.contains($0.id) }.count) · Zapisane: \(entries.count)")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                }.padding(.vertical, 6)
                Text(service.lastResult).font(.caption).foregroundStyle(.secondary)
                Button(service.running ? "Sprawdzanie…" : "Sprawdź oceny teraz", systemImage: "arrow.clockwise") { service.check() }
                    .disabled(!service.enabled || service.running)
                if !service.enabled { Text("Włącz sprawdzanie ocen w Dzwonku, aby wykrywać kolejne zmiany.").font(.footnote) }
            }
            if let error {
                Section { Text(error).foregroundStyle(.red); Button("Ponów odczyt historii") { load() } }
            }
            Section("Filtry") {
                Picker("Uczeń", selection: $student) {
                    Text("Wszyscy uczniowie").tag("")
                    ForEach(students, id: \.studentFilterKey) { entry in Text(entry.student).tag(entry.studentFilterKey) }
                }
                Toggle("Tylko nieprzeczytane", isOn: $unreadOnly)
                if route.batch != nil {
                    Label("Zmiany z wybranego powiadomienia", systemImage: "bell")
                    Button("Pokaż całą historię") { route.batch = nil }
                }
            }
            Section("Co się zmieniło") {
                if visible.isEmpty && ready {
                    Text(route.batch != nil
                         ? "Brak pasujących wpisów. Zmień filtry lub pokaż całą historię. Starsze zmiany mogły wypaść z limitu 100 wpisów."
                         : "Brak zmian pasujących do filtrów. Pierwsze sprawdzenie zapisuje punkt odniesienia bez powiadamiania o starych ocenach.")
                        .foregroundStyle(.secondary)
                }
                ForEach(visible) { entry in
                    Button {
                        selected = entry
                        mark([entry], read: true)
                    } label: {
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: entry.before == nil ? "plus.circle.fill" : "arrow.triangle.2.circlepath")
                                .foregroundStyle(reads.ids.contains(entry.id) ? Color.secondary : Color.teal)
                            VStack(alignment: .leading, spacing: 5) {
                                Text(entry.student).font(.headline)
                                Text(entry.after.subject).font(.subheadline)
                                Text(entry.preview).font(.title3.bold()).foregroundStyle(.primary)
                                HStack {
                                    Text(entry.changeLabel)
                                    Spacer()
                                    if !reads.ids.contains(entry.id) { Text("NOWE").fontWeight(.bold).foregroundStyle(.teal) }
                                }.font(.caption)
                                if let date = entry.detectedDate {
                                    Text(date.formatted(date: .abbreviated, time: .shortened)).font(.caption).foregroundStyle(.secondary)
                                }
                            }
                        }.padding(.vertical, 6)
                    }.buttonStyle(.plain)
                    .swipeActions {
                        Button(reads.ids.contains(entry.id) ? "Nieprzeczytane" : "Przeczytane") {
                            mark([entry], read: !reads.ids.contains(entry.id))
                        }.tint(.teal)
                    }
                }
                if !visible.isEmpty {
                    Button("Oznacz widoczne jako przeczytane") { mark(visible, read: true) }.disabled(!ready)
                }
            }
            Section {
                Text("Ostatnie 100 wykrytych zmian. Szczegóły są zapisane lokalnie i dostępne bez internetu. Czas oznacza wykrycie zmiany, nie moment wpisania przez nauczyciela. Przeczytanie dotyczy tylko historii Budy. Wylogowanie usuwa historię.")
                    .font(.footnote).foregroundStyle(.secondary)
            }
        }
        .searchable(text: $query, prompt: "Uczeń, przedmiot, ocena lub komentarz")
        .navigationTitle("Zmiany ocen")
        .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Gotowe") { dismiss() } } }
        .onAppear { load() }
        .onChange(of: service.running) { running in if !running { load() } }
        .onChange(of: route.batch) { _ in selected = nil; student = ""; unreadOnly = false; query = ""; load() }
        .sheet(item: $selected) { entry in NavigationStack { GradeChangeDetail(entry: entry) } }
    }
    private enum HistoryError: Error { case locked }
}
private struct GradeChangeDetail: View {
    let entry: GradeHistoryEntry
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        List {
            Section {
                Text(entry.student).font(.title2.bold())
                Text(entry.after.subject).font(.headline)
                Text(entry.preview).font(.title.bold()).foregroundStyle(.teal)
                Text(entry.changeLabel).font(.subheadline).foregroundStyle(.secondary)
            }
            if let old = entry.before {
                Section("Wcześniej") { fields(old) }
                Section("Po zmianie") { fields(entry.after) }
            } else { Section("Szczegóły nowej oceny") { fields(entry.after) } }
            Section {
                Text("Semestr: \(entry.period)")
                if let date = entry.detectedDate {
                    Text("Wykryto: " + date.formatted(date: .long, time: .shortened))
                } else { Text("Wykryto: \(entry.at)") }
                Text("To zapis z chwili sprawdzenia. Bieżące dane znajdziesz w Ocenach.").font(.footnote)
            }
        }.textSelection(.enabled).navigationTitle("Szczegóły zmiany")
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Gotowe") { dismiss() } } }
    }
    @ViewBuilder private func fields(_ grade: HistoricalGrade) -> some View {
        LabeledContent("Ocena", value: grade.content)
        LabeledContent("Przedmiot", value: grade.subject)
        VStack(alignment: .leading) { Text("Opis").font(.caption).foregroundStyle(.secondary); Text(grade.description.isEmpty ? "Brak" : grade.description) }
        VStack(alignment: .leading) { Text("Komentarz").font(.caption).foregroundStyle(.secondary); Text(grade.comment.isEmpty ? "Brak" : grade.comment) }
    }
}
