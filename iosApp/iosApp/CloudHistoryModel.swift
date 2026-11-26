import Foundation

struct CloudChange: Codable, Identifiable {
    struct Item: Codable { let title: String?; let details: String?; let date: String? }
    let id: String
    let student: String
    let category: String
    let at: String
    let batch: String?
    let before: Item?
    let after: Item
}
struct CloudHistory: Codable {
    let history: [CloudChange]
    let lastCheck: String?
    let lastError: String?
}
struct CloudReadState: Codable { var ids: Set<String> = [] }
func filteredCloudChanges(_ entries: [CloudChange], student: String, category: String, query: String, unreadOnly: Bool, readIDs: Set<String>, batch: String?) -> [CloudChange] {
    entries.filter {
        (batch == nil || $0.batch == batch) &&
        (student.isEmpty || $0.student == student) &&
        (category.isEmpty || $0.category == category) &&
        (!unreadOnly || !readIDs.contains($0.id)) &&
        (query.isEmpty || [$0.student, $0.category, $0.after.title ?? "", $0.after.details ?? ""].joined(separator: " ").localizedCaseInsensitiveContains(query))
    }.sorted { $0.at > $1.at }
}
