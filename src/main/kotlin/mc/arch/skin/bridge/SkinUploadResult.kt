package mc.arch.skin.bridge

import com.google.gson.JsonObject

data class SkinUploadResult(
    val textureValue: String?,
    val textureSignature: String?,
    val skinUuid: String?,
    val error: String?,
    val responseBody: JsonObject? = null
)
