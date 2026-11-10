import Foundation

struct HistoricalGrade: Decodable {
    let content: String
    let subject: String
    let description: String
    let comment: String
}
struct GradeHistoryEntry: Decodable, Identifiable {
    let id: String
    let student: String
    let period: String
    let at: String
    let before: HistoricalGrade?
    let after: HistoricalGrade
    var accountKey: String?
    var studentFilterKey: String { accountKey ?? student }
    var detectedDate: Date? {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = formatter.date(from: at) { return date }
        formatter.formatOptions = [.withInternetDateTime]
        return formatter.date(from: at)
    }
    var changeLabel: String {
        guard let old = before else { return "Nowa ocena" }
        var parts: [String] = []
        if old.content != after.content { parts.append("Ocena") }
        if old.subject != after.subject { parts.append("Przedmiot") }
        if old.description != after.description { parts.append("Opis") }
        if old.comment != after.comment { parts.append("Komentarz") }
        return parts.isEmpty ? "Zmiana szczegółów" : parts.joined(separator: " · ")
    }
    var preview: String {
        guard let old = before else { return "Nowa ocena: \(after.content)" }
        return old.content == after.content ? "Ocena \(after.content) · \(changeLabel)" : "\(old.content) → \(after.content)"
    }
}
struct HistoryEnvelope: Decodable {
    let history: [GradeHistoryEntry]?
    static func entries(from text: String) throws -> [GradeHistoryEntry] {
        try JSONDecoder().decode(Self.self, from: Data(text.utf8)).history ?? []
    }
}
struct HistoryReadState: Codable {
    var ids: Set<String> = []
    mutating func mark(_ selected: [GradeHistoryEntry], read: Bool, retained: [GradeHistoryEntry]) {
        let selectedIDs = Set(selected.map(\.id))
        if read { ids.formUnion(selectedIDs) } else { ids.subtract(selectedIDs) }
        ids.formIntersection(Set(retained.map(\.id)))
    }
}
func filteredGradeHistory(_ entries: [GradeHistoryEntry], readIDs: Set<String>, student: String?,
                          unreadOnly: Bool, query: String, batch: String?) -> [GradeHistoryEntry] {
    let search = query.trimmingCharacters(in: .whitespacesAndNewlines)
    return entries.filter { entry in
        (student == nil || entry.studentFilterKey == student) &&
        (!unreadOnly || !readIDs.contains(entry.id)) &&
        (batch == nil || entry.at == batch) &&
        (search.isEmpty || [entry.student, entry.after.subject, entry.after.content,
                           entry.after.description, entry.after.comment, entry.before?.content ?? "",
                           entry.before?.description ?? "", entry.before?.comment ?? ""]
            .contains { $0.localizedStandardContains(search) })
    }
}
func gradeBannerBody(entries: [GradeHistoryEntry], total: Int, details: Bool) -> String {
    guard details, let first = entries.first else {
        return "Wykryto zmian ocen: \(total). Dotknij, aby zobaczyć zapisane szczegóły."
    }
    // Do not include comments or descriptions on the lock screen.
    let summary = "\(first.student) · \(first.after.subject): \(first.preview)"
    return String(summary.prefix(180)) + (total > 1 ? " · Pozostałe zmiany: \(total - 1)." : "")
}
