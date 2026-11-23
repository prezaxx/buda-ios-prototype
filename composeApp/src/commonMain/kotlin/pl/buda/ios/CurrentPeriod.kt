package pl.buda.ios
import io.github.szpontium.api.hebe.models.Period
import kotlinx.datetime.LocalDate
internal fun currentPeriod(periods:List<Period>,today:LocalDate):Period? {
 val dated=periods.filter { today>=it.start && today<=it.end }
 return dated.singleOrNull() ?: dated.firstOrNull { it.current==true }
  ?: periods.firstOrNull { it.current==true } ?: periods.maxByOrNull { it.start }
}
