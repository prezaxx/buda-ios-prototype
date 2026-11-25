import SwiftUI
import AuthenticationServices
import CryptoKit

struct CloudChange: Decodable, Identifiable {
    struct Item: Decodable { let title: String?; let details: String?; let date: String? }
    let id: String
    let student: String
    let category: String
    let at: String
    let batch: String?
    let before: Item?
    let after: Item
}
struct CloudHistory: Decodable {
    let history: [CloudChange]
    let lastCheck: String?
    let lastError: String?
}
@MainActor final class CloudNotifications: NSObject, ObservableObject, ASWebAuthenticationPresentationContextProviding {
    static let shared = CloudNotifications()
    static let origin = "https://buda-push-pilot.prezaxx-buda.workers.dev"
    @Published var entries: [CloudChange] = []
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
                    self.entries = []; self.connected = true; self.busy = false
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
                _ = vault.clear(); connected = false; entries = []; message = "Dostęp wygasł. Połącz historię ponownie."; return
            }
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw URLError(.badServerResponse) }
            let result = try JSONDecoder().decode(CloudHistory.self, from: data)
            entries = result.history; lastCheck = result.lastCheck; message = result.lastError ?? ""; connected = true
        } catch {
            if generation == attempt { message = "Nie udało się odświeżyć historii. Sprawdź połączenie i spróbuj ponownie." }
        }
    }
    func disconnect() {
        let old = validToken
        generation = UUID(); authentication?.cancel(); authentication = nil
        _ = vault.clear(); entries = []; connected = false; busy = false; message = ""; lastCheck = nil
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
    @Environment(\.scenePhase) private var phase
    @State var batch: String?
    @State private var query = ""
    @State private var student = ""
    private var filtered: [CloudChange] {
        service.entries.filter {
            (batch == nil || $0.batch == batch) &&
            (student.isEmpty || $0.student == student) &&
            (query.isEmpty || [$0.student, $0.category, $0.after.title ?? "", $0.after.details ?? ""].joined(separator: " ").localizedCaseInsensitiveContains(query))
        }
    }
    private func date(_ raw: String) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return (formatter.date(from: raw) ?? ISO8601DateFormatter().date(from: raw))?.formatted(date: .abbreviated, time: .shortened) ?? raw
    }
    var body: some View {
        List {
            Section {
                Label("Powiadomienia z serwera", systemImage: "bell.badge.fill").font(.title2.bold()).foregroundStyle(.indigo)
                Text("Ta sama historia, z której pochodzą banery — również gdy Buda była zamknięta.").font(.footnote)
                if !service.connected {
                    Button("Połącz historię z Budą") { service.pair() }.disabled(service.busy)
                    Text("Zaloguj się istniejącym kluczem dostępu do powiadomień i potwierdź połączenie. Nie twórz nowego konta.").font(.footnote)
                } else {
                    Button("Odśwież historię", systemImage: "arrow.clockwise") { Task { await service.refresh() } }.disabled(service.busy)
                    if let last = service.lastCheck { Text("Ostatni odczyt serwera: " + date(last)).font(.caption) }
                }
                if service.busy { ProgressView("Pobieranie…") }
                if !service.message.isEmpty { Text(service.message).foregroundStyle(.orange) }
            }
            if service.connected {
                Section("Filtry") {
                    Picker("Uczeń", selection: $student) {
                        Text("Wszyscy").tag("")
                        ForEach(Array(Set(service.entries.map(\.student))).sorted(), id: \.self) { Text($0).tag($0) }
                    }
                    if batch != nil { Button("Pokaż wszystkie powiadomienia") { batch = nil } }
                }
                if filtered.isEmpty && !service.busy {
                    Section {
                        Text(batch == nil ? "Brak zapisanych zdarzeń pasujących do filtrów." : "To zdarzenie nie jest dostępne w ostatnich 100 zmianach tego konta. Pokaż całą historię lub sprawdź połączone konto.")
                    }
                }
                ForEach(filtered) { entry in
                    Section {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(entry.student + " • " + entry.category).font(.caption.bold()).foregroundStyle(.indigo)
                            Text(entry.after.title ?? entry.category).font(.headline)
                            Text(entry.after.details ?? "").font(.body).textSelection(.enabled)
                            if let before = entry.before {
                                Text("Przed zmianą: " + [before.title, before.details].compactMap { $0 }.joined(separator: " • ")).font(.footnote).foregroundStyle(.secondary)
                            }
                            Text("Wykryto: " + date(entry.at)).font(.caption).foregroundStyle(.secondary)
                        }.padding(.vertical, 6)
                    }
                }
                Section {
                    Button("Odłącz historię na tym iPhonie", role: .destructive) { service.disconnect() }
                    Text("Odłącza tylko dostęp do historii w aplikacji. Nie wyłącza powiadomień Web Push.").font(.caption)
                }
            }
        }
        .navigationTitle("Powiadomienia")
        .searchable(text: $query, prompt: "Uczeń, przedmiot lub temat")
        .task { await service.refresh() }
        .refreshable { await service.refresh() }
        .onChange(of: phase) { value in if value == .active { Task { await service.refresh() } } }
    }
}
