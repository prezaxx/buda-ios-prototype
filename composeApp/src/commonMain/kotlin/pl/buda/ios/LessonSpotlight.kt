package pl.buda.ios
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

internal data class LessonSpotlight(val row:DiaryRow,val active:Boolean,val minutes:Int,val progress:Float)
internal fun lessonSpotlight(rows:List<DiaryRow>,now:LocalDateTime):LessonSpotlight? {
 fun seconds(time:LocalTime)=time.hour*3600+time.minute*60+time.second
 val current=seconds(now.time)
 data class Valid(val row:DiaryRow,val start:Int,val end:Int)
 val valid=rows.mapNotNull { row ->
  val info=row.lesson ?: return@mapNotNull null
  if(info.cancelled || row.date.take(10)!=now.date.toString()) return@mapNotNull null
  val start=runCatching { seconds(LocalTime.parse(info.start)) }.getOrNull() ?: return@mapNotNull null
  val end=runCatching { seconds(LocalTime.parse(info.end)) }.getOrNull() ?: return@mapNotNull null
  if(end<=start || current>=end) null else Valid(row,start,end)
 }.sortedWith(compareBy({it.start},{it.row.key}))
 val selected=valid.firstOrNull { current>=it.start } ?: valid.firstOrNull() ?: return null
 val active=current>=selected.start
 val remaining=if(active) selected.end-current else selected.start-current
 return LessonSpotlight(selected.row,active,(remaining+59)/60,
  if(active) ((current-selected.start).toFloat()/(selected.end-selected.start)).coerceIn(0f,1f) else 0f)
}
