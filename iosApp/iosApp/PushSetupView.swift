import SwiftUI
import UniformTypeIdentifiers

struct EncryptedSessionDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.data] }
    var data: Data
    init(data: Data) { self.data = data }
    init(configuration: ReadConfiguration) throws { data = configuration.file.regularFileContents ?? Data() }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper { FileWrapper(regularFileWithContents: data) }
}
@MainActor struct PushSetupView: View {
    @State private var selecting = false
    @State private var exporting = false
    @State private var document: EncryptedSessionDocument?
    @State private var message = ""
    @State private var consent = false
    private let site = URL(string: "https://buda-push-pilot.prezaxx-buda.workers.dev")!
    private let legacySite = URL(string: "https://buda-powiadomienia.amberline-0653.chatgpt.site")!
    var body: some View {
        ScrollViewReader { proxy in
        List {
            Section("Historia w aplikacji") {
                NavigationLink("Połącz i pokaż powiadomienia z banerów") { CloudNotificationView(batch: nil) }
            }
            Section {
                Label("Buda Powiadomienia", systemImage: "bell.badge.fill").font(.title2.bold()).foregroundStyle(.teal)
                Text("Dodatek na ekranie głównym iPhone’a odbiera Web Push niezależnie od otwarcia Budy. Wymaga połączenia sesji i zgody na powiadomienia. Nowy pilotaż nie wymaga konta ChatGPT. Ma limit pięciu kont. Odczyty odbywają się partiami, a pełny cykl dla dwóch uczniów może potrwać około 50 minut; alerty nie są natychmiastowe.")
                Link("Otwórz powiadomienia bez ChatGPT", destination: site)
                Text("buda-push-pilot.prezaxx-buda.workers.dev").font(.footnote).textSelection(.enabled)
            }
            if let prepared = document {
                Section("Plik sesji jest gotowy") {
                    Label("Klucz odczytany. Sesja została zaszyfrowana.", systemImage: "checkmark.shield.fill")
                        .foregroundStyle(.teal)
                    Button("Zapisz zaszyfrowaną sesję", systemImage: "square.and.arrow.down") {
                        document = prepared
                        exporting = true
                    }.disabled(selecting || exporting)
                    Text("Zapisz plik Buda-session.buda w Plikach, a następnie wczytaj go w dodatku Buda Powiadomienia.")
                        .font(.footnote)
                }
                .id("push-export-ready")
            }
            if !message.isEmpty {
                Section("Stan połączenia") {
                    Text(message).font(.callout).accessibilityIdentifier("pushExportStatus")
                        .id("push-export-status")
                }
            }
            Section("Połączenie krok po kroku") {
                Text("1. Otwórz dodatek w Safari i dodaj go do ekranu początkowego.")
                Text("2. Utwórz własne konto kluczem dostępu (Face ID lub kod iPhone’a). Zapisz kod odzyskiwania. Pobierz klucz połączenia z nowego dodatku.")
                Text("3. Wybierz klucz poniżej. Po zamknięciu wyboru pliku naciśnij Zapisz zaszyfrowaną sesję.")
                Text("4. Wczytaj ten plik w dodatku, włącz powiadomienia i wyślij test.")
            }
            Section("Twoja prywatność") {
                Text("Eksport pozwoli serwerowi dodatku odczytywać dane zalogowanych uczniów. Hasło nie jest eksportowane. Plik zawiera sesję wiadomości (cookies), aby dodatek mógł sprawdzać skrzynkę. Sesja zostanie zaszyfrowana przed zapisaniem pliku.")
                Text("Wybierz wyłącznie klucz pobrany po zalogowaniu na wybranej stronie Budy. Klucz wskazuje, który serwer będzie mógł odczytać sesję.")
                Toggle("Zgadzam się przekazać sesję do mojego dodatku powiadomień", isOn: $consent)
                Button("Wybierz klucz i zaszyfruj sesję", systemImage: "lock.doc") { document = nil; message = ""; selecting = true }.disabled(!consent || exporting)
                Text("Dostęp można usunąć przyciskiem odłączenia dziennika lub usunięcia konta w dodatku. Wylogowanie w Budzie nie usuwa kopii sesji na serwerze.")
            }
            Section("Przenosisz się ze starego dodatku?") {
                Text("Sesje obu serwerów są oddzielne. Pobierz nowy klucz, wyeksportuj sesję ponownie i wczytaj ją na nowej stronie. Samo zainstalowanie aktualizacji nie przenosi danych.")
                Text("Po sprawdzeniu nowego testu push wstrzymaj stary dodatek, aby uniknąć podwójnych alertów.")
                Link("Stary prywatny dodatek — wymaga ChatGPT", destination: legacySite)
            }
            Section("Zakres wersji próbnej") {
                Text("Oceny, wiadomości, frekwencja, sprawdziany, zadania, uwagi, ogłoszenia i zmiany planu. Po odnowieniu dostępu do wiadomości zapisz nową sesję i wczytaj ją w dodatku.")
                Text("Pierwsze pobranie ustala stan początkowy bez alertów o starych danych. Powiadomienie otwiera historię w dodatku. Sprawdź test również po zablokowaniu iPhone’a.")
            }
        }
        .onChange(of: message) { _ in
            withAnimation { proxy.scrollTo(document == nil ? "push-export-status" : "push-export-ready", anchor: .top) }
        }
        }.navigationTitle("Web Push")
        .fileImporter(isPresented: $selecting, allowedContentTypes: [.json], allowsMultipleSelection: false) { result in
            do {
                guard let url = try result.get().first else { return }
                let access = url.startAccessingSecurityScopedResource()
                defer { if access { url.stopAccessingSecurityScopedResource() } }
                guard let session = KeychainVault().read() else { throw PushSessionExport.ExportError.invalidSession }
                let encrypted = try PushSessionExport.encrypt(session: session, keyFile: Data(contentsOf: url))
                document = EncryptedSessionDocument(data: encrypted)
                message = "Sesja jest gotowa. Naciśnij Zapisz zaszyfrowaną sesję w sekcji powyżej."
            } catch {
                document = nil
                message = error.localizedDescription
            }
        }
        .fileExporter(isPresented: $exporting, document: document, contentType: .data, defaultFilename: "Buda-session.buda") { result in
            switch result {
            case .success: message = "Zapisano zaszyfrowaną sesję. Wczytaj ją teraz w dodatku."
            case .failure: message = "Nie zapisano pliku. Możesz ponownie nacisnąć Zapisz zaszyfrowaną sesję."
            }
        }
    }
}
