package io.github.szpontium.api.prometheus
import kotlinx.serialization.json.*
internal fun validateSendResponse(status:Int,body:String,requestId:String) {
 if(status !in 200..299) throw MessagesAccessException("SEND_HTTP_$status")
 if(body.isBlank()) return
 val data=try { Json.parseToJsonElement(body) } catch(e:Exception) { throw MessagesAccessException("SEND_UNKNOWN_RESPONSE") }
 if(data is JsonNull || (data as? JsonPrimitive)?.booleanOrNull==true) return
 val obj=data as? JsonObject ?: throw MessagesAccessException("SEND_UNKNOWN_RESPONSE")
 if(obj.keys.any { it.lowercase() in setOf("error","errors","exception") && obj[it]!=JsonNull }) throw MessagesAccessException("SEND_REPORTED_ERROR")
 if((obj["success"] as? JsonPrimitive)?.booleanOrNull==false) throw MessagesAccessException("SEND_REPORTED_ERROR")
 if((obj["success"] as? JsonPrimitive)?.booleanOrNull==true) return
 if((obj["apiGlobalKey"] as? JsonPrimitive)?.content==requestId) return
 throw MessagesAccessException("SEND_UNKNOWN_RESPONSE")
}
