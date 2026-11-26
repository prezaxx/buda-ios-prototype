import Foundation
@main struct CloudHistoryTests {
 static func main() throws {
  let raw = #"{"history":[{"id":"a","student":"Jan","category":"Oceny","at":"2026-11-26T10:00:00Z","batch":"one","before":{"details":"4"},"after":{"title":"Matematyka","details":"5"}},{"id":"b","student":"Anna","category":"Wiadomości","at":"2026-11-26T11:00:00Z","batch":"two","after":{"title":"Zebranie","details":"Wychowawca"}}],"lastCheck":null,"lastError":null}"#
  let data = try JSONDecoder().decode(CloudHistory.self, from: Data(raw.utf8))
  func filter(student: String = "", category: String = "", query: String = "", unread: Bool = false, batch: String? = nil) -> [String] {
   filteredCloudChanges(data.history, student: student, category: category, query: query, unreadOnly: unread, readIDs: ["a"], batch: batch).map(\.id)
  }
  precondition(filter() == ["b","a"])
  precondition(filter(student: "Jan") == ["a"])
  precondition(filter(category: "Wiadomości") == ["b"])
  precondition(filter(query: "matematyka") == ["a"])
  precondition(filter(unread: true) == ["b"])
  precondition(filter(student: "Jan", unread: true).isEmpty)
  precondition(filter(batch: "one") == ["a"])
  precondition(filter(batch: "gone").isEmpty)
  let read = try JSONDecoder().decode(CloudReadState.self, from: JSONEncoder().encode(CloudReadState(ids: ["a"])))
  precondition(read.ids == ["a"])
  let roundTrip = try JSONDecoder().decode(CloudHistory.self, from: JSONEncoder().encode(data))
  precondition(roundTrip.history[0].before?.details == "4")
  print("CloudHistory: decoding, filters, unread state, chronological ordering and batch isolation passed.")
 }
}
