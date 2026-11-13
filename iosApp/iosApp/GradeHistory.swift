import SwiftUI

@MainActor final class NotificationHistoryRoute: ObservableObject {
    static let shared = NotificationHistoryRoute()
    @Published var batch: String?
}
@MainActor struct GradeHistoryView: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
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
                }.padding(16)
                    .background(RoundedRectangle(cornerRadius: 20).fill(
                        LinearGradient(colors: [Color.teal.opacity(0.16), Color.blue.opacity(0.08)], startPoint: .topLeading, endPoint: .bottomTrailing)))

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
            if visible.isEmpty && ready {
                Section {
                    VStack(spacing: 10) {
                        Image(systemName: "checkmark.circle").font(.largeTitle).foregroundStyle(.teal)
                        Text("Brak pasujących zmian").font(.headline)
                        Text(route.batch != nil
                             ? "Brak wpisów pasujących do tego powiadomienia. Zmień filtry lub pokaż całą historię."
                             : "Brak zmian pasujących do filtrów. Pierwsze sprawdzenie zapisuje stan początkowy bez alertów o starych ocenach.")
                            .font(.subheadline).foregroundStyle(.secondary).multilineTextAlignment(.center)
                    }.frame(maxWidth: .infinity).padding(.vertical, 20)
                }
            }
            ForEach(groupedGradeHistory(visible)) { group in
                Section {
                    ForEach(group.entries) { entry in
                        Button {
                            selected = entry
                            mark([entry], read: true)
                        } label: {
                            HistoryTimelineRow(entry: entry, unread: !reads.ids.contains(entry.id))
                        }.buttonStyle(.plain)
                        .swipeActions {
                            Button(reads.ids.contains(entry.id) ? "Nieprzeczytane" : "Przeczytane") {
                                mark([entry], read: !reads.ids.contains(entry.id))
                            }.tint(.teal)
                        }
                    }
                } header: {
                    HStack {
                        Text(group.title)
                        Spacer()
                        Text("\(group.entries.count)")
                    }.font(.subheadline.weight(.semibold)).textCase(nil)
                }
            }
            if !visible.isEmpty {
                Section {
                    Button("Oznacz widoczne jako przeczytane") { mark(visible, read: true) }.disabled(!ready)
                }
            }
            Section {
                Text("Ostatnie 100 wykrytych zmian. Szczegóły są zapisane lokalnie i dostępne bez internetu. Czas oznacza wykrycie zmiany, nie moment wpisania przez nauczyciela. Przeczytanie dotyczy tylko historii Budy. Wylogowanie usuwa historię.")
                    .font(.footnote).foregroundStyle(.secondary)
            }
        }
        .listStyle(.insetGrouped)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.18), value: unreadOnly)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.18), value: student)
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

private struct HistoryTimelineRow: View {
    let entry: GradeHistoryEntry
    let unread: Bool
    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            VStack(spacing: 6) {
                Image(systemName: entry.before == nil ? "plus" : "arrow.triangle.2.circlepath")
                    .font(.caption.bold())
                    .frame(width: 30, height: 30)
                    .foregroundStyle(unread ? Color.teal : Color.secondary)
                    .background(Circle().fill(Color.teal.opacity(unread ? 0.14 : 0.06)))
                RoundedRectangle(cornerRadius: 1).fill(Color.teal.opacity(0.15)).frame(width: 2, height: 44)
            }
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .firstTextBaseline) {
                    Text(entry.after.subject).font(.headline).foregroundStyle(.primary)
                    Spacer(minLength: 8)
                    if unread {
                        Text("NOWE").font(.caption2.bold()).foregroundStyle(.teal)
                            .padding(.horizontal, 8).padding(.vertical, 4)
                            .background(Capsule().fill(Color.teal.opacity(0.12)))
                    }
                }
                HStack(spacing: 10) {
                    if let before = entry.before, before.content != entry.after.content {
                        Text(before.content).font(.title3.weight(.semibold)).foregroundStyle(.secondary)
                            .padding(10).background(RoundedRectangle(cornerRadius: 12).fill(Color.secondary.opacity(0.08)))
                        Image(systemName: "arrow.right").font(.caption).foregroundStyle(.secondary)
                    }
                    Text(entry.after.content).font(.title2.bold()).foregroundStyle(.primary)
                        .padding(.horizontal, 15).padding(.vertical, 9)
                        .background(RoundedRectangle(cornerRadius: 12).fill(Color.teal.opacity(0.12)))
                    Text(entry.changeLabel).font(.caption).foregroundStyle(.secondary)
                }
                Text(entry.student).font(.subheadline).foregroundStyle(.secondary)
                if !entry.after.description.isEmpty {
                    Text(entry.after.description).font(.caption).foregroundStyle(.secondary).lineLimit(2)
                }
                if let date = entry.detectedDate {
                    Text(date.formatted(date: .omitted, time: .shortened))
                        .font(.caption.monospacedDigit()).foregroundStyle(.secondary)
                }
            }
        }.padding(.vertical, 9).accessibilityElement(children: .combine)
    }
}
