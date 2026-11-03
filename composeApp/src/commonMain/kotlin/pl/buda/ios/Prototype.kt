package pl.buda.ios
import io.github.szpontium.api.hebe.*
import io.github.szpontium.api.hebe.credentials.RsaCredential
import io.github.szpontium.api.hebe.models.Account
import io.github.szpontium.api.prometheus.PrometheusLoginHelper
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.*
import kotlinx.serialization.SerializationException
import kotlin.time.Clock

data class Student(val account:Account,val api:SzpontHebeCeApi,val credential:RsaCredential,val tenant:String?=null,val cookies:List<StoredCookie> = emptyList(),val mailboxKey:String?=null) {
 val title get()="${account.pupil.firstName} ${account.pupil.surname}"
 val school get()=account.unit.displayName.ifBlank { account.unit.name }
}
data class ScreenState(
 val busy:Boolean=false,val students:List<Student> = emptyList(),val selected:Int?=null,
 val periodId:Int?=null,val grades:List<GradeRow> = emptyList(),val error:String?=null,
 val section:Section=Section.GRADES,val rows:List<DiaryRow> = emptyList(),
 val from:String="",val to:String="",val remember:Boolean=true,val notice:String?=null
)
class Prototype(private val scope:CoroutineScope,private val vault:SessionVault) {
 private val client=HttpClient { install(HttpTimeout) { requestTimeoutMillis=45000 } }
 private val mutable=MutableStateFlow(ScreenState())
 val state=mutable.asStateFlow()
 private var job:Job?=null
 val messages=MessagesController(scope,onMailboxChosen={ source,key ->
  mutable.value=mutable.value.copy(students=mutable.value.students.map {
   if(it.credential.deviceId==source.credential.deviceId && it.account==source.account) it.copy(mailboxKey=key) else it
  })
  save()
 })
 init {
  try {
   vault.read()?.let { value ->
    val stored=decodeSession(value)
    val students=stored.students.map { item -> val c=item.credential.restore();Student(item.account,SzpontHebeCeApi(c,client),c,item.tenant,item.cookies,item.mailboxKey) }
    mutable.value=ScreenState(students=students)
    stored.selected?.let { select(it) }
   }
  } catch(e:Exception) { mutable.value=ScreenState(notice="Nie udało się odczytać zapisanej sesji. Zaloguj się ponownie; hasło nie było zapisywane.") }
 }
 fun remember(enabled:Boolean) {
  if(!enabled && !vault.clear()) { mutable.value=mutable.value.copy(notice="Nie udało się usunąć sesji z pęku kluczy.");return }
  mutable.value=mutable.value.copy(remember=enabled,notice=null)
  if(enabled) save()
 }
 private fun save() {
  val s=mutable.value
  if(!s.remember || s.students.isEmpty()) return
  val value=encodeSession(StoredSession(students=s.students.map { StoredStudent(it.account,StoredCredential.from(it.credential),it.tenant,it.cookies,it.mailboxKey) },selected=s.selected))
  if(!vault.write(value)) mutable.value=s.copy(notice="Sesja działa, ale nie udało się zapisać jej w pęku kluczy. Po zamknięciu aplikacji może być potrzebne logowanie.")
 }
 fun login(email:String,password:String) {
  if(email.isBlank() || password.isBlank()) { mutable.value=mutable.value.copy(error="Wpisz e-mail i hasło.");return }
  messages.clear();job?.cancel();val remember=mutable.value.remember
  job=scope.launch {
   mutable.value=ScreenState(busy=true,remember=remember)
   val helper=PrometheusLoginHelper(autoAcceptConsent=false)
   try {
    val result=helper.login(email.trim(),password,deviceModel="iPhone")
    val tenants=result.tokensByTenant.ifEmpty { mapOf((result.tenantTokens.keys.firstOrNull() ?: "eduvulcan") to result.tokens) }
    val students=mutableListOf<Student>()
    for((tenant,tokens) in tenants) {
     if(tokens.isEmpty()) continue
     val c=withContext(Dispatchers.Default) { RsaCredential.createNew(deviceOs="iOS",deviceModel="iPhone") }
     val api=SzpontHebeCeApi(c,client);api.registerByJwt(tokens,tenant)
     students+=api.getAccounts().map { Student(it,api,c,tenant,result.cookies.map(StoredCookie::from)) }
    }
    ensureActive()
    mutable.value=ScreenState(students=students,remember=remember,error=if(students.isEmpty()) "Nie znaleziono uczniów." else null)
    save()
   } catch(e:CancellationException) { throw e }
     catch(e:Exception) { currentCoroutineContext().ensureActive();mutable.value=ScreenState(remember=remember,error=safeError(e)) }
   finally { helper.close() }
  }
 }
 fun select(index:Int) {
  val student=mutable.value.students.getOrNull(index) ?: return
  job?.cancel()
  messages.use(student)
  val period=student.account.periods?.firstOrNull { it.current==true } ?: student.account.periods?.lastOrNull()
  mutable.value=mutable.value.copy(selected=index,periodId=period?.id,section=Section.GRADES,grades=emptyList(),rows=emptyList(),error=null,busy=false)
  save();refresh()
 }
 fun switchStudent(index:Int) {
  val section=mutable.value.section
  select(index)
  if(section!=Section.GRADES) section(section)
 }
 fun period(id:Int) {
  val account=mutable.value.selected?.let { mutable.value.students[it].account } ?: return
  if(account.periods.orEmpty().none { it.id==id }) return
  job?.cancel();mutable.value=mutable.value.copy(periodId=id,grades=emptyList(),rows=emptyList());refresh()
 }
 fun section(section:Section) {
  job?.cancel();messages.cancel()
  val today=Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
  val from=if(section==Section.SCHEDULE) today.minus(today.dayOfWeek.ordinal,DateTimeUnit.DAY) else today.minus(30,DateTimeUnit.DAY)
  val to=if(section==Section.SCHEDULE) from.plus(6,DateTimeUnit.DAY) else if(section==Section.PRESENCE) today else today.plus(60,DateTimeUnit.DAY)
  mutable.value=mutable.value.copy(section=section,from=from.toString(),to=to.toString(),grades=emptyList(),rows=emptyList(),error=null)
  refresh()
 }
 fun dates(from:String,to:String) {
  try {
   val a=LocalDate.parse(from);val b=LocalDate.parse(to)
   require(a<=b && a.daysUntil(b)<=366)
  } catch(e:Exception) { mutable.value=mutable.value.copy(error="Podaj daty RRRR-MM-DD. Zakres może obejmować najwyżej 366 dni.");return }
  mutable.value=mutable.value.copy(from=from,to=to);refresh()
 }
 fun shiftDays(days:Int) {
  val s=mutable.value
  dates(LocalDate.parse(s.from).plus(days,DateTimeUnit.DAY).toString(),LocalDate.parse(s.to).plus(days,DateTimeUnit.DAY).toString())
 }
 fun refresh() {
  val before=mutable.value;val student=before.selected?.let { before.students.getOrNull(it) } ?: return
  job?.cancel()
  if(before.section==Section.MESSAGES) { mutable.value=before.copy(busy=false);messages.refresh();return }
  job=scope.launch {
   mutable.value=before.copy(busy=true,grades=emptyList(),rows=emptyList(),error=null)
   try {
    if(before.section==Section.GRADES) {
     val period=before.periodId
     if(period==null) { mutable.value=mutable.value.copy(busy=false,error="Konto nie podało okresu oceniania.");return@launch }
     val grades=loadPages { last -> parseGrades(student.api.getGradePayload(student.account.unit.restUrl,student.account.unit.id,student.account.pupil.id,period,lastId=last,pageSize=500),student.account.pupil.id) }
     ensureActive();mutable.value=mutable.value.copy(busy=false,grades=grades.sortedByDescending { it.createdAt })
    } else {
     val result=fetchDiary(student.api,student.account,before.section,before.periodId,before.from,before.to)
     ensureActive();mutable.value=mutable.value.copy(busy=false,rows=result.rows,error=result.warning)
    }
   } catch(e:CancellationException) { throw e }
     catch(e:Exception) { currentCoroutineContext().ensureActive();mutable.value=mutable.value.copy(busy=false,error=safeError(e,before.section.label)) }
  }
 }
 fun back() { messages.clear();job?.cancel();mutable.value=mutable.value.copy(selected=null,periodId=null,grades=emptyList(),rows=emptyList(),error=null,busy=false);save() }
 fun logout() {
  if(!vault.clear()) { mutable.value=mutable.value.copy(notice="Nie udało się usunąć zapisanej sesji. Spróbuj ponownie po odblokowaniu telefonu.");return }
  messages.clear();job?.cancel();mutable.value=ScreenState()
 }
 fun close() { messages.clear();job?.cancel();client.close() }
}
internal suspend fun <T> collectPages(pageSize: Int, id: (T)->Int, fetch: suspend(Int)->List<T>):List<T> {
 val collected=linkedMapOf<Int,T>();var last=Int.MIN_VALUE
 repeat(100) {
  val page=fetch(last)
  page.forEach { collected[id(it)]=it }
  if(page.size<pageSize) return collected.values.toList()
  val next=page.maxOf(id)
  check(next>last) { "PaginationNoProgress" }
  last=next
 }
 error("PaginationLimit")
}
private suspend fun loadPages(fetch:suspend(Int)->List<GradeRow>)=collectPages(500,{ g:GradeRow->g.id },fetch)
internal fun safeError(e:Exception, stage:String="logowanie"):String {
 if(e is GradeFormatException) return "Nieobsługiwany format ocen: ${e.field}. Pozostajesz na koncie ucznia; możesz wrócić do listy lub ponowić pobieranie."
 if(e is SerializationException) return "Nieobsługiwany format odpowiedzi ($stage). To nie potwierdza błędnego hasła ani braku zgód. Kod: ${e::class.simpleName}."
 // Do not display server bodies, URLs, tokens, or passwords in diagnostics.
 val type=e::class.simpleName ?: "UnknownError"
 return "Nie udało się pobrać danych ($stage, $type). Spróbuj ponownie. Sesja ucznia nie została usunięta."
}
