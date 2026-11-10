import Foundation
@main struct GradeHistoryModelTests {
    static func main() throws {
        let raw = """
        {"history":[
          {"id":"a","student":"Jan","accountKey":"account-a","period":"1","at":"2026-11-10T10:00:00.123Z","before":{"content":"4","subject":"Matematyka","description":"Test","comment":""},"after":{"content":"5","subject":"Matematyka","description":"Test","comment":"Poprawa"}},
          {"id":"b","student":"Jan","accountKey":"account-b","period":"1","at":"2026-11-09T10:00:00Z","after":{"content":"6","subject":"Polski","description":"","comment":""}}
        ]}
        """
        let entries = try HistoryEnvelope.entries(from: raw)
        precondition(entries.count == 2)
        precondition(entries.allSatisfy { $0.detectedDate != nil })
        precondition(entries[0].preview == "4 → 5")
        precondition(entries[0].changeLabel == "Ocena · Komentarz")
        precondition(entries[1].preview == "Nowa ocena: 6")
        let emptyLegacy = try HistoryEnvelope.entries(from: "{\"accounts\":{}}")
        precondition(emptyLegacy.isEmpty)
        let legacy = try HistoryEnvelope.entries(from: raw.replacingOccurrences(of: "\"accountKey\":\"account-a\",", with: ""))
        precondition(legacy[0].studentFilterKey == "Jan")
        var read = HistoryReadState()
        read.mark([entries[0]], read: true, retained: entries)
        let restored = try JSONDecoder().decode(HistoryReadState.self, from: JSONEncoder().encode(read))
        precondition(restored.ids == ["a"])
        precondition(filteredGradeHistory(entries, readIDs: read.ids, student: nil, unreadOnly: true, query: "", batch: nil).map(\.id) == ["b"])
        precondition(filteredGradeHistory(entries, readIDs: [], student: "account-b", unreadOnly: false, query: "", batch: nil).map(\.id) == ["b"])
        precondition(filteredGradeHistory(entries, readIDs: [], student: nil, unreadOnly: false, query: "poprawa", batch: nil).map(\.id) == ["a"])
        precondition(filteredGradeHistory(entries, readIDs: [], student: nil, unreadOnly: false, query: "", batch: entries[0].at).map(\.id) == ["a"])
        precondition(filteredGradeHistory(entries, readIDs: [], student: nil, unreadOnly: false, query: "", batch: "expired").isEmpty)
        read.mark([entries[0]], read: false, retained: entries)
        precondition(read.ids.isEmpty)
        read.mark(entries, read: true, retained: [entries[1]])
        precondition(read.ids == ["b"])
        let privateBanner = gradeBannerBody(entries: entries, total: 2, details: false)
        precondition(!privateBanner.contains("Jan") && !privateBanner.contains("Matematyka"))
        let detailedBanner = gradeBannerBody(entries: entries, total: 2, details: true)
        precondition(detailedBanner.contains("4 → 5") && !detailedBanner.contains("Poprawa"))
        precondition(detailedBanner.contains("Pozostałe zmiany: 1"))
        do { _ = try HistoryEnvelope.entries(from: "KEYCHAIN_UNAVAILABLE"); fatalError("Invalid history accepted") } catch {}
        print("Grade history model: migration, sibling isolation, unread persistence, batch routing, privacy and timestamps PASS")
    }
}
