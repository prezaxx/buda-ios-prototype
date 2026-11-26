import SwiftUI
import AuthenticationServices
import CryptoKit
import Security
import ComposeApp

@MainActor final class CloudNotificationRoute: ObservableObject {
    static let shared = CloudNotificationRoute()
    @Published var batch: String?
}
@MainActor final class CloudNotifications: NSObject, ObservableObject, ASWebAuthenticationPresentationContextProviding {
    static let shared = CloudNotifications()
    static let origin = "https://buda-push-pilot.prezaxx-buda.workers.dev"
    @Published var entries: [CloudChange] = []
    @Published var readIDs: Set<String> = []
    private let readsVault = KeychainVault(account: "cloud-history-reads-v1")
    private let cacheVault = KeychainVault(account: "cloud-history-cache-v1")
    var unreadCount: Int { entries.filter { !readIDs.contains($0.id) }.count }
    func markRead(_ changes: [CloudChange]) {
        let next = readIDs.union(changes.map(\.id)).intersection(Set(entries.map(\.id)))
        guard let data = try? JSONEncoder().encode(CloudReadState(ids: next)),
              let text = String(data: data, encoding: .utf8), readsVault.write(value: text) else {
            message = "Nie udało się zapisać oznaczenia przeczytania."; return
        }
        readIDs = next; updateBadge()
    }
    private func updateBadge() { NativeNavigation.shared.setUnreadCount(count: Int32(unreadCount)) }
    private func clearHistory() {
        entries = []; readIDs = []; _ = readsVault.clear(); _ = cacheVault.clear(); updateBadge()
    }
    @Published var busy = false
    @Published var message = ""
    @Published var connected = false
    @Published var lastCheck: String?
    private let vault = KeychainVault(account: "cloud-history-token-v1")
    private var authentication: ASWebAuthenticationSession?
    private var generation = UUID()
    override init() {
        super.init()
        connected = validToken != nil
        if connected {
            if let raw = readsVault.read(), let state = try? JSONDecoder().decode(CloudReadState.self, from: Data(raw.utf8)) { readIDs = state.ids }
            if let raw = cacheVault.read(), let state = try? JSONDecoder().decode(CloudHistory.self, from: Data(raw.utf8)) { entries = state.history; lastCheck = state.lastCheck }
        }
        updateBadge()
    }
    private var validToken: String? {
        guard let t = vault.read(), t.count == 43, t != "KEYCHAIN_UNAVAILABLE" else { return nil }
        return t
    }
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows).first(where: \.isKeyWindow) ?? ASPresentationAnchor()
    }
    private func random() -> String {
        var bytes = [UInt8](repeating: 0, count: 32)
        guard SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes) == errSecSuccess else { return "" }
        return encode(Data(bytes))
    }
    private func encode(_ data: Data) -> String {
        data.base64EncodedString().replacingOccurrences(of: "+", with: "-").replacingOccurrences(of: "/", with: "_").replacingOccurrences(of: "=", with: "")
    }
    func pair() {
        guard !busy else { return }
        let verifier = random(), state = random()
        guard !verifier.isEmpty, !state.isEmpty else { message = "Nie udało się rozpocząć połączenia."; return }
        let challenge = encode(Data(SHA256.hash(data: Data(verifier.utf8))))
        let url = URL(string: Self.origin + "/?native=1&challenge=" + challenge + "&state=" + state)!
        busy = true; message = ""
        let attempt = generation
        let session = ASWebAuthenticationSession(url: url, callbackURLScheme: "budadiary") { [weak self] url, error in
            Task { @MainActor in
                guard let self, self.generation == attempt else { return }
                self.authentication = nil
                guard let url, url.host == "paired",
                      let parts = URLComponents(url: url, resolvingAgainstBaseURL: false),
                      parts.queryItems?.first(where: { $0.name == "state" })?.value == state,
                      let code = parts.queryItems?.first(where: { $0.name == "code" })?.value else {
                    self.busy = false; self.message = "Łączenie anulowano. Możesz spróbować ponownie."; return
                }
                do {
                    var request = URLRequest(url: URL(string: Self.origin + "/api/native/token")!)
                    request.httpMethod = "POST"
                    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
                    request.httpBody = try JSONSerialization.data(withJSONObject: ["code": code, "verifier": verifier])
                    let (data, response) = try await URLSession.shared.data(for: request)
                    guard self.generation == attempt else { return }
                    struct Grant: Decodable { let token: String }
                    guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw URLError(.userAuthenticationRequired) }
                    let grant = try JSONDecoder().decode(Grant.self, from: data)
                    guard grant.token.count == 43, self.vault.write(value: grant.token) else { throw URLError(.cannotWriteToFile) }
                    self.clearHistory(); self.connected = true; self.busy = false
                    await self.refresh()
                } catch {
                    self.busy = false; self.message = "Nie udało się połączyć historii. Spróbuj ponownie po odblokowaniu iPhone’a."
                }
            }
        }
        session.presentationContextProvider = self
        session.prefersEphemeralWebBrowserSession = false
        authentication = session
        if !session.start() { busy = false; authentication = nil; message = "Nie udało się otworzyć logowania." }
    }
    func refresh() async {
        guard !busy else { return }
        guard let token = validToken else { connected = false; return }
        busy = true
        let attempt = generation
        defer { if generation == attempt { busy = false } }
        do {
            var request = URLRequest(url: URL(string: Self.origin + "/api/native/history")!)
            request.setValue("Bearer " + token, forHTTPHeaderField: "Authorization")
            request.timeoutInterval = 25
            request.cachePolicy = .reloadIgnoringLocalCacheData
            let (data, response) = try await URLSession.shared.data(for: request)
            guard generation == attempt else { return }
            if (response as? HTTPURLResponse)?.statusCode == 401 {
                _ = vault.clear(); connected = false; clearHistory(); message = "Dostęp wygasł. Połącz historię ponownie."; return
            }
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw URLError(.badServerResponse) }
            let result = try JSONDecoder().decode(CloudHistory.self, from: data)
            entries = result.history; lastCheck = result.lastCheck; message = result.lastError ?? ""; connected = true
            if let encoded = try? JSONEncoder().encode(result), let text = String(data: encoded, encoding: .utf8) {
                if !cacheVault.write(value: text) { message = "Historia pobrana, ale zapis offline się nie udał." }
            }
            updateBadge()
        } catch {
            if generation == attempt { message = "Nie udało się odświeżyć historii. Sprawdź połączenie i spróbuj ponownie." }
        }
    }
    func disconnect() {
        let old = validToken
        generation = UUID(); authentication?.cancel(); authentication = nil
        _ = vault.clear(); clearHistory(); connected = false; busy = false; message = ""; lastCheck = nil
        if let old {
            Task {
                var request = URLRequest(url: URL(string: Self.origin + "/api/native/revoke")!)
                request.httpMethod = "POST"; request.setValue("Bearer " + old, forHTTPHeaderField: "Authorization")
                request.timeoutInterval = 15
                _ = try? await URLSession.shared.data(for: request)
            }
        }
    }
}
@MainActor struct CloudNotificationView: View {
    @ObservedObject private var service = CloudNotifications.shared
    @ObservedObject private var route = CloudNotificationRoute.shared
    @Environment(\.scenePhase) private var phase
    @State var batch: String?
    @State private var query = ""
    @State private var student = ""
    @State private var category = ""
    @State private var unreadOnly = false
    @State private var filters = false
    private var filtered: [CloudChange] {
        filteredCloudChanges(service.entries, student: student, category: category, query: query, unreadOnly: unreadOnly, readIDs: service.readIDs, batch: batch)
    }
    private var groups: [(String,[CloudChange])] {
        Dictionary(grouping: filtered) { cloudDate($0.at, time: false) }.map { ($0.key,$0.value) }.sorted { ($0.1.first?.at ?? "") > ($1.1.first?.at ?? "") }
    }
    var body: some View {
        List {
            if !service.connected {
                Section {
                    Label("Połącz powiadomienia", systemImage: "bell.badge.fill").font(.headline)
                    Button("Połącz historię z Budą") { service.pair() }.disabled(service.busy)
                    Text("Użyj istniejącego klucza dostępu (Face ID) dodatku powiadomień. To nie jest plik JSON.").font(.footnote)
                }
            }
            if service.busy { ProgressView("Odświeżanie…") }
            if !service.message.isEmpty { Text(service.message).font(.footnote).foregroundStyle(.orange) }
            if service.connected {
                Section {
                    HStack {
                        Label("\(service.unreadCount) nowych", systemImage: "bell.badge").foregroundStyle(.indigo)
                        Spacer()
                        Button("Przeczytane") { service.markRead(filtered) }.disabled(filtered.isEmpty)
                    }
                    if let at = service.lastCheck { Text("Odczyt dziennika: " + cloudDate(at)).font(.caption).foregroundStyle(.secondary) }
                    if batch != nil { Button("Pokaż wszystkie powiadomienia") { batch = nil; route.batch = nil } }
                }
                if filters {
                    Section("Filtry") {
                        Picker("Uczeń", selection: $student) {
                            Text("Wszyscy").tag("")
                            ForEach(Array(Set(service.entries.map(\.student))).sorted(), id: \.self) { Text($0).tag($0) }
                        }
                        Picker("Rodzaj", selection: $category) {
                            Text("Wszystkie").tag("")
                            ForEach(Array(Set(service.entries.map(\.category))).sorted(), id: \.self) { Text($0).tag($0) }
                        }
                        Toggle("Tylko nieprzeczytane", isOn: $unreadOnly)
                        Button("Wyczyść filtry") { student = ""; category = ""; query = ""; unreadOnly = false; batch = nil; route.batch = nil }
                    }
                }
                if filtered.isEmpty && !service.busy {
                    Text(batch == nil ? "Brak zdarzeń pasujących do filtrów." : "To powiadomienie nie znajduje się w ostatnich 100 zmianach połączonego konta. Pokaż całą historię.").foregroundStyle(.secondary)
                }
                ForEach(groups, id: \.0) { group in
                    Section(group.0) {
                        ForEach(group.1) { entry in
                            NavigationLink {
                                CloudChangeDetail(entry: entry).onAppear { service.markRead([entry]) }
                            } label: {
                                HStack(alignment: .top, spacing: 12) {
                                    Image(systemName: cloudIcon(entry.category)).font(.title3).foregroundStyle(.indigo)
                                        .frame(width: 40, height: 40).background(Color.indigo.opacity(0.12), in: Circle())
                                    VStack(alignment: .leading, spacing: 5) {
                                        HStack {
                                            Text(entry.category).font(.headline)
                                            if !service.readIDs.contains(entry.id) { Circle().fill(Color.indigo).frame(width: 7, height: 7) }
                                            Spacer()
                                        }
                                        Text((entry.before == nil ? "Nowe: " : "Zmiana: ") + [entry.after.title, entry.after.details].compactMap { $0 }.joined(separator: " • ")).lineLimit(3)
                                        Text(entry.student + " • " + cloudDate(entry.at)).font(.caption).foregroundStyle(.secondary)
                                    }
                                }.padding(.vertical, 5)
                            }
                        }
                    }
                }
            }
            Section("Historia i ustawienia") {
                NavigationLink("Zmiany wykryte na iPhonie") { GradeHistoryView() }
                NavigationLink("Ustawienia powiadomień") { NotificationSettingsView() }
                if service.connected { Button("Odłącz historię na tym iPhonie", role: .destructive) { service.disconnect() } }
                Text("Historia obejmuje ostatnie 100 zmian serwera. Odłączenie historii nie wyłącza Web Push.").font(.caption)
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Powiadomienia").navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { filters.toggle() } label: { Image(systemName: "line.3.horizontal.decrease.circle") }.accessibilityLabel("Filtry powiadomień")
            }
        }
        .searchable(text: $query, prompt: "Uczeń, przedmiot lub temat")
        .task { await service.refresh() }
        .refreshable { await service.refresh() }
        .onChange(of: route.batch) { value in batch = value; student = ""; category = ""; unreadOnly = false; query = "" }
        .onChange(of: phase) { value in if value == .active { Task { await service.refresh() } } }
    }
}
private func cloudDate(_ raw: String, time: Bool = true) -> String {
    let parser = ISO8601DateFormatter(); parser.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    guard let date = parser.date(from: raw) ?? ISO8601DateFormatter().date(from: raw) else { return raw }
    return date.formatted(date: .abbreviated, time: time ? .shortened : .omitted)
}
private func cloudIcon(_ category: String) -> String {
    let value = category.lowercased()
    if value.contains("ocen") { return "graduationcap" }
    if value.contains("wiadomo") { return "envelope" }
    if value.contains("plan") || value.contains("zaj") { return "calendar" }
    if value.contains("frekw") { return "person.badge.clock" }
    if value.contains("sprawdz") { return "checklist" }
    return "bell"
}
private struct CloudChangeDetail: View {
    let entry: CloudChange
    var body: some View {
        List {
            Section(entry.category) {
                Text(entry.student).font(.headline)
                Text(entry.after.title ?? "").font(.title3.bold())
                Text(entry.after.details ?? "").textSelection(.enabled)
                if let date = entry.after.date, !date.isEmpty { Text("Termin / data wpisu: " + date).font(.footnote) }
            }
            if let before = entry.before {
                Section("Przed zmianą") {
                    Text(before.title ?? "")
                    Text(before.details ?? "").textSelection(.enabled)
                }
            }
            Section { Text("Wykryto: " + cloudDate(entry.at)) }
        }.navigationTitle("Szczegóły").navigationBarTitleDisplayMode(.inline)
    }
}
