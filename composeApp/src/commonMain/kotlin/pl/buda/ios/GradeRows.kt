package pl.buda.ios
import kotlinx.serialization.json.*

// Only decode data actually needed by this screen. Unused author/weight/colour
// metadata must not make a pupil's entire grade list unreadable.
data class GradeRow(val id:Int, val content:String, val subject:String,
 val description:String, val comment:String, val createdAt:String)
class GradeFormatException(val field:String):Exception("Unsupported grade field")
internal fun parseGrades(payload:JsonElement, expectedPupil:Int):List<GradeRow> {
 val rows=payload as? JsonArray ?: throw GradeFormatException("Envelope: oczekiwano listy")
 return rows.map { item ->
  val row=item as? JsonObject ?: throw GradeFormatException("ocena: oczekiwano obiektu")
  val id=row["Id"].number() ?: throw GradeFormatException("Id")
  val pupil=row["PupilId"]
  if(pupil!=null && pupil !is JsonNull) {
   val returned=pupil.number() ?: throw GradeFormatException("PupilId")
   if(returned!=expectedPupil) throw GradeFormatException("PupilId: niezgodny uczeń")
  }
  val content=row["Content"].text().ifBlank { row["ContentRaw"].text() }
  if(content.isBlank()) throw GradeFormatException("Content / ContentRaw")
  val column=row["Column"] as? JsonObject
  val subject=column?.get("Subject") as? JsonObject
  GradeRow(id,content,subject?.get("Name").text().ifBlank { "Nie podano przedmiotu" },
   column?.get("Name").text(),row["Comment"].text(),row["CreatedAt"].text())
 }
}
private fun JsonElement?.text():String = (this as? JsonPrimitive)?.contentOrNull.orEmpty()
private fun JsonElement?.number():Int? = (this as? JsonPrimitive)?.intOrNull
