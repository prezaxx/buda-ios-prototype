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
    private let site = URL(string: "https://buda-powiadomienia.amberline-0653.chatgpt.site")!
    var body: some View {
        List {
            Section {
                Label("Buda Powiadomienia", systemImage: "bell.badge.fill").font(.title2.bold()).foregroundStyle(.teal)
                Text("Dodatek na ekranie głównym iPhone’a odbiera Web Push niezależnie od otwarcia Budy. Wymaga połączenia sesji i zgody na powiadomienia. Harmonogram serwera sprawdza dziennik raz na godzinę; alerty nie są natychmiastowe.")
                Link("Otwórz prywatny dodatek", destination: site)
            }
            Section("Połączenie krok po kroku") {
                Text("1. Otwórz dodatek w Safari i dodaj go do ekranu początkowego.")
                Text("2. Zaloguj się, pobierz klucz połączenia i zapisz go w Plikach.")
                Text("3. Wybierz klucz poniżej. Buda przygotuje zaszyfrowany plik sesji.")
                Text("4. Wczytaj ten plik w dodatku, włącz powiadomienia i wyślij test.")
            }
            Section("Twoja prywatność") {
                Text("Eksport pozwoli serwerowi dodatku odczytywać dane zalogowanych uczniów. Hasło i pliki cookie wiadomości nie są eksportowane. Sesja zostanie zaszyfrowana przed zapisaniem pliku.")
                Text("Wybierz wyłącznie klucz pobrany ze swojej prywatnej strony Budy. Klucz wskazuje, który serwer będzie mógł odczytać sesję.")
                Toggle("Zgadzam się przekazać sesję do mojego dodatku powiadomień", isOn: $consent)
                Button("Wybierz klucz i zaszyfruj sesję", systemImage: "lock.doc") { selecting = true }.disabled(!consent)
                if !message.isEmpty { Text(message).font(.footnote) }
                Text("Dostęp można usunąć przyciskiem „Usuń dane i odłącz urządzenia” w dodatku. Wylogowanie w Budzie nie usuwa kopii sesji na serwerze.")
            }
            Section("Zakres wersji próbnej") {
                Text("Oceny, frekwencja, sprawdziany, zadania, uwagi i ogłoszenia. Wiadomości nie są jeszcze objęte tym dodatkiem.")
                Text("Pierwsze pobranie ustala stan początkowy bez alertów o starych danych. Powiadomienie otwiera historię w dodatku. Sprawdź test również po zablokowaniu iPhone’a.")
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
                exporting = true
            } catch { message = error.localizedDescription }
        }
        .fileExporter(isPresented: $exporting, document: document, contentType: .data, defaultFilename: "Buda-session.buda") { result in
            switch result {
            case .success: message = "Zapisano zaszyfrowaną sesję. Wczytaj ją teraz w dodatku."
            case .failure: message = "Nie zapisano pliku sesji."
            }
            document = nil
        }
    }
}
