import SwiftUI
import UIKit

enum BudaBuild {
    static var version: String { Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "Nieznana" }
    static var number: String { Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "Nieznany" }
}
@MainActor struct AboutView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var service = GradeNotificationService.shared
    @AppStorage("budaAppearance") private var appearance = "system"
    @State private var copied = false
    var body: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: 12) {
                    Image(systemName: "graduationcap.fill")
                        .font(.system(size: 38)).foregroundStyle(.teal)
                    Text("Buda").font(.largeTitle.bold())
                    Text("Twój dzień. Twój rytm.").font(.headline).foregroundStyle(.secondary)
                    Text("Wersja \(BudaBuild.version) · kompilacja \(BudaBuild.number)")
                        .font(.subheadline.monospacedDigit()).textSelection(.enabled)
                    Text("Nieoficjalny klient eduVULCAN na iPhone’a.")
                }.padding(.vertical, 12)
            }
            Section("Wygląd") {
                Picker("Motyw", selection: $appearance) {
                    Label("Jak iPhone", systemImage: "circle.lefthalf.filled").tag("system")
                    Label("Jasny", systemImage: "sun.max").tag("light")
                    Label("Ciemny", systemImage: "moon.stars").tag("dark")
                }
                Text("Twój wybór obejmuje dziennik, oceny i ustawienia. Kolory ocen nauczyciela są zachowane.").font(.footnote)
            }
            Section("Nowe w 0.15.1") {
                Label("Ręczne sprawdzanie niezależne od przełącznika powiadomień", systemImage: "arrow.clockwise")
                Label("Główny dodatek push bez konta ChatGPT", systemImage: "person.badge.key")
                Label("Jasny i ciemny motyw do wyboru", systemImage: "circle.lefthalf.filled")
                Label("Odnowienie dostępu do wiadomości bez usuwania ocen", systemImage: "key")
                Label("Zapisywanie odświeżonej sesji skrzynki", systemImage: "arrow.trianglehead.2.clockwise")
                Label("Powiadomienia o różnych zmianach dziennika", systemImage: "bell.badge")
            }
            Section("Twoje powiadomienia") {
                NavigationLink("Powiadomienia Web Push") { PushSetupView() }
                LabeledContent("Sprawdzanie dziennika", value: service.enabled ? "Włączone" : "Wyłączone")
                Text(service.lastResult).font(.footnote)
                NavigationLink("Centrum zmian") {
                    GradeHistoryView().onAppear { NotificationHistoryRoute.shared.batch = nil }
                }
                Text("Lokalne sprawdzanie zależy od iOS. Oddzielny dodatek Web Push wymaga połączenia sesji i zgody na powiadomienia. Nowy pilotaż odczytuje kategorie partiami. Alerty otwierają historię dodatku, oddzielną od lokalnego Centrum zmian.").font(.footnote)
            }
            Section("Diagnostyka") {
                Text(service.diagnostics).font(.footnote).textSelection(.enabled)
                Button(copied ? "Skopiowano informacje" : "Kopiuj informacje diagnostyczne", systemImage: "doc.on.doc") {
                    UIPasteboard.general.string = """
                    Buda \(BudaBuild.version) (\(BudaBuild.number))
                    iOS \(UIDevice.current.systemVersion)
                    \(service.diagnostics)
                    Ostatnie sprawdzenie: \(service.lastResult)
                    Ostatnie uruchomienie w tle: \(service.lastBackground)
                    """
                    copied = true
                }
                Text("Kopiowane są wersje i stan działania. Bez loginu, hasła, nazw uczniów, ocen i treści wiadomości.").font(.caption).foregroundStyle(.secondary)
                Button("Odśwież diagnostykę") { copied = false; service.refreshDiagnostics() }
            }
            Section("Dane i prywatność") {
                Text("Hasło nie jest zapisywane. Zapamiętana sesja oraz lokalne dane są przechowywane w pęku kluczy tego iPhone’a. Opcjonalny dodatek Web Push otrzymuje sesję tylko po świadomym eksporcie i wczytaniu zaszyfrowanego pliku.")
                Text("Historia ocen obejmuje ostatnie 100 wykrytych zmian. Oznaczenie przeczytania działa lokalnie. Wylogowanie usuwa zapisane dane aplikacji.")
                Text("Offline dostępne są tylko wcześniej zapisane dane. Połączenie ze szkołą jest potrzebne do pobrania nowych wpisów.")
            }
            Section("O projekcie") {
                Text("Buda nie jest oficjalną aplikacją VULCAN ani Etną. Dostępność funkcji zależy od konta, szkoły i odpowiedzi API.")
                Text("Projekt korzysta z Kotlin Multiplatform, Compose Multiplatform, SwiftUI oraz kodu API opisanego w NOTICE.txt i LICENSE-UPSTREAM w repozytorium.")
                Link("Repozytorium projektu", destination: URL(string: "https://github.com/prezaxx/buda-ios-prototype")!)
                Text("Repozytorium może wymagać zalogowania i uprawnień.").font(.caption).foregroundStyle(.secondary)
            }
        }.navigationTitle("O aplikacji")
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Gotowe") { dismiss() } } }
            .onAppear { service.refreshDiagnostics() }
    }
}
