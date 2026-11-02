package pl.buda.ios
import io.github.szpontium.api.hebe.credentials.RsaCredential
import io.github.szpontium.api.hebe.models.Account
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface SessionVault {
 fun read(): String?
 fun write(value: String): Boolean
 fun clear(): Boolean
}
class MemoryVault : SessionVault {
 private var value:String?=null
 override fun read()=value
 override fun write(value:String):Boolean { this.value=value;return true }
 override fun clear():Boolean { value=null;return true }
}
@Serializable data class StoredCredential(
 val type:String,val restUrl:String?,val certificate:String,val privateKey:String,
 val fingerprint:String,val deviceId:String,val deviceOs:String,val deviceModel:String
) {
 fun restore()=RsaCredential(type,restUrl,certificate,privateKey,fingerprint,null,deviceId,deviceOs,deviceModel)
 companion object { fun from(c:RsaCredential)=StoredCredential(c.type,c.restUrl,c.certificate,c.privateKey,c.fingerprint,c.deviceId,c.deviceOs,c.deviceModel) }
}
@Serializable data class StoredCookie(val name:String,val value:String,val domain:String?,val path:String?,val secure:Boolean,val httpOnly:Boolean,val expires:Long?=null) {
 fun restore()=io.ktor.http.Cookie(name=name,value=value,domain=domain,path=path,secure=secure,httpOnly=httpOnly,expires=expires?.let { io.ktor.util.date.GMTDate(it) })
 companion object { fun from(c:io.ktor.http.Cookie)=StoredCookie(c.name,c.value,c.domain,c.path,c.secure,c.httpOnly,c.expires?.timestamp) }
}
@Serializable data class StoredStudent(val account:Account,val credential:StoredCredential,val tenant:String?=null,val cookies:List<StoredCookie> = emptyList())
@Serializable data class StoredSession(val version:Int=1,val students:List<StoredStudent>,val selected:Int?=null)
internal val sessionJson=Json { ignoreUnknownKeys=true }
internal fun encodeSession(value:StoredSession)=sessionJson.encodeToString(StoredSession.serializer(),value)
internal fun decodeSession(value:String):StoredSession {
 val result=sessionJson.decodeFromString(StoredSession.serializer(),value)
 require(result.version==1 && result.students.isNotEmpty())
 require(result.selected==null || result.selected in result.students.indices)
 return result
}
