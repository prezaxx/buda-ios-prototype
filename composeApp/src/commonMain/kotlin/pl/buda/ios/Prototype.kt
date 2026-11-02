package pl.buda.ios

import io.github.szpontium.api.hebe.*
import io.github.szpontium.api.hebe.credentials.RsaCredential
import io.github.szpontium.api.hebe.models.Account
import io.github.szpontium.api.hebe.models.Grade
import io.github.szpontium.api.prometheus.PrometheusLoginHelper
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Each pupil keeps the API instance associated with its own tenant credential.
data class Student(val account: Account, val api: SzpontHebeCeApi) {
 val title get() = "${account.pupil.firstName} ${account.pupil.surname}"
 val school get() = account.unit.displayName.ifBlank { account.unit.name }
}
data class ScreenState(
 val busy: Boolean = false, val students: List<Student> = emptyList(),
 val selected: Int? = null, val periodId: Int? = null,
 val grades: List<Grade> = emptyList(), val error: String? = null
)
class Prototype(private val scope: CoroutineScope) {
 private val client = HttpClient { install(HttpTimeout) { requestTimeoutMillis = 45000 } }
 private val mutable = MutableStateFlow(ScreenState())
 val state = mutable.asStateFlow()
 private var job: Job? = null
 fun login(email: String, password: String) {
  if(email.isBlank() || password.isBlank()) { mutable.value=ScreenState(error="Wpisz e-mail i hasło.");return }
  job?.cancel()
  job=scope.launch {
   mutable.value=ScreenState(busy=true)
   val helper=PrometheusLoginHelper(autoAcceptConsent=false)
   try {
    val result=helper.login(email.trim(),password,deviceModel="iPhone")
    val tenants=result.tokensByTenant.ifEmpty { mapOf((result.tenantTokens.keys.firstOrNull() ?: "eduvulcan") to result.tokens) }
    val students=mutableListOf<Student>()
    for((tenant,tokens) in tenants) {
     if(tokens.isEmpty()) continue
     val credential=withContext(Dispatchers.Default) { RsaCredential.createNew(deviceOs="iOS",deviceModel="iPhone") }
     val api=SzpontHebeCeApi(credential,client)
     api.registerByJwt(tokens,tenant)
     students += api.getAccounts().map { Student(it,api) }
    }
    mutable.value=ScreenState(students=students,error=if(students.isEmpty()) "Nie znaleziono uczniów." else null)
   } catch(e: CancellationException) { throw e }
     catch(e: Exception) { mutable.value=ScreenState(error=safeError(e)) }
   finally { helper.close() }
  }
 }
 fun select(index: Int) {
  val s=mutable.value.students.getOrNull(index) ?: return
  job?.cancel()
  val period=s.account.periods?.firstOrNull { it.current==true } ?: s.account.periods?.lastOrNull()
  mutable.value=mutable.value.copy(selected=index,periodId=period?.id,grades=emptyList(),error=null,busy=false)
  if(period==null) { mutable.value=mutable.value.copy(error="Konto nie zwróciło żadnego okresu oceniania.");return }
  refresh()
 }
 fun period(id: Int) { job?.cancel();mutable.value=mutable.value.copy(periodId=id,grades=emptyList());refresh() }
 fun refresh() {
  val before=mutable.value
  val s=before.selected?.let { before.students.getOrNull(it) } ?: return
  val period=before.periodId ?: return
  job?.cancel()
  job=scope.launch {
   mutable.value=before.copy(busy=true,grades=emptyList(),error=null)
   try {
    val grades=loadPages { last -> s.api.getGrades(s.account.unit.restUrl,s.account.unit.id,s.account.pupil.id,period,lastId=last,pageSize=500) }
    ensureActive()
    mutable.value=mutable.value.copy(busy=false,grades=grades.sortedByDescending { it.createdAt })
   } catch(e: CancellationException) { throw e }
     catch(e: Exception) { mutable.value=mutable.value.copy(busy=false,error=safeError(e)) }
  }
 }
 fun back() { job?.cancel();mutable.value=mutable.value.copy(selected=null,periodId=null,grades=emptyList(),error=null,busy=false) }
 fun logout() { job?.cancel();mutable.value=ScreenState() }
 fun close() { job?.cancel();client.close() }
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
private suspend fun loadPages(fetch:suspend(Int)->List<Grade>)=collectPages(500,{ g:Grade->g.id },fetch)
private fun safeError(e:Exception):String {
 // Do not display server bodies, URLs, tokens, or passwords in diagnostics.
 val type=e::class.simpleName ?: "UnknownError"
 return "Nie udało się pobrać danych ($type). Sprawdź logowanie w eduvulcan.pl i ewentualne wymagane zgody. Spróbuj ponownie."
}
