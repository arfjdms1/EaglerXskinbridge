package mc.arch.skin.bridge

import com.google.gson.Gson
import com.google.gson.JsonObject
import mc.arch.skin.bridge.cache.CacheKey
import mc.arch.skin.bridge.cache.CacheProvider
import mc.arch.skin.bridge.cache.CachedSkin
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO

class SkinConversionAgent(
    private val config: SkinConversionConfig,
    private val cacheProvider: CacheProvider,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson()
)
{
    fun convertAndUploadToMineSkin(
        base64Data: String,
        variant: String = "classic",
        visibility: String = "public",
        width: Int = config.defaultWidth,
        height: Int = config.defaultHeight
    ): CompletableFuture<SkinUploadResult>
    {
        return CompletableFuture.supplyAsync {
            try
            {
                val hash = generateSkinHash(base64Data)
                getCachedSkinResponse(hash)?.let { cachedSkin ->
                    return@supplyAsync SkinUploadResult(
                        textureValue = cachedSkin.skinValue,
                        textureSignature = cachedSkin.skinSignature,
                        skinUuid = hash,
                        error = null
                    )
                }

                val pngData = getOrCreatePng(hash, base64Data, width, height)
                    ?: throw RuntimeException("Failed to convert ABGR8 to PNG")

                val uploadResult = uploadToMineSkin(hash, pngData, variant, visibility)

                if (config.cacheEnabled && uploadResult.error == null)
                {
                    cacheSkinResponse(
                        skinHash = hash,
                        textureValue = uploadResult.textureValue ?: error("Missing texture value"),
                        textureSignature = uploadResult.textureSignature ?: error("Missing texture signature")
                    )
                }

                uploadResult
            } catch (exception: Exception)
            {
                println("Error during MineSkin upload: ${exception.message}")
                exception.printStackTrace()

                SkinUploadResult(
                    textureValue = null,
                    textureSignature = null,
                    skinUuid = null,
                    error = exception.message ?: "Unknown error"
                )
            }
        }
    }

    fun uploadPresetToMineSkin(
        presetId: Int,
        variant: String = "classic",
        visibility: String = "public"
    ): CompletableFuture<SkinUploadResult> {
        return CompletableFuture.supplyAsync {
            try {
                if (presetId < 0 || presetId > 23) {
                    return@supplyAsync SkinUploadResult(null, null, null, "Preset skin ID $presetId not supported via MineSkin")
                }
                val hash = "preset_$presetId"
                getCachedSkinResponse(hash)?.let { cachedSkin ->
                    return@supplyAsync SkinUploadResult(
                        textureValue = cachedSkin.skinValue,
                        textureSignature = cachedSkin.skinSignature,
                        skinUuid = hash,
                        error = null
                    )
                }

                val resourceStream = javaClass.getResourceAsStream("/presets/$presetId.png")
                if (resourceStream == null) {
                    return@supplyAsync SkinUploadResult(null, null, null, "Preset PNG not found in resources")
                }

                val pngData = resourceStream.readBytes()
                val uploadResult = uploadToMineSkin(hash, pngData, variant, visibility)

                if (config.cacheEnabled && uploadResult.error == null) {
                    cacheSkinResponse(
                        skinHash = hash,
                        textureValue = uploadResult.textureValue ?: error("Missing texture value"),
                        textureSignature = uploadResult.textureSignature ?: error("Missing texture signature")
                    )
                }

                uploadResult
            } catch (exception: Exception) {
                println("Error during MineSkin preset upload: ${exception.message}")
                exception.printStackTrace()

                SkinUploadResult(
                    textureValue = null,
                    textureSignature = null,
                    skinUuid = null,
                    error = exception.message ?: "Unknown error"
                )
            }
        }
    }

    private fun getOrCreatePng(hash: String, base64Data: String, width: Int, height: Int): ByteArray?
    {
        if (config.cacheEnabled)
        {
            cacheProvider.get(CacheKey.PNG, hash)?.let {
                return it
            }
        }

        val pngData = convertABGR8ToPNGBytes(base64Data, width, height)
        if (pngData != null && config.cacheEnabled)
        {
            cacheProvider.put(CacheKey.PNG, hash, pngData, config.pngCacheTtlMinutes)
        }
        return pngData
    }

    private fun uploadToMineSkin(hash: String, pngData: ByteArray, variant: String, visibility: String): SkinUploadResult
    {
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "skin.png",
                pngData.toRequestBody("image/png".toMediaType())
            )
            .addFormDataPart("name", "archmc-${hash.take(7)}")
            .addFormDataPart("visibility", visibility)
            .addFormDataPart("variant", variant)
            .build()

        val requestBuilder = Request.Builder()
            .url("https://api.mineskin.org/generate/upload")
            .post(multipartBody)
            .header("Accept", "application/json")
            .header("User-Agent", config.userAgent)

        if (config.mineSkinApiKey.isNotBlank())
        {
            requestBuilder.header("Authorization", "Bearer ${config.mineSkinApiKey}")
        }

        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            val bodyJson = bodyString
                .takeIf { it.isNotBlank() }
                ?.let { gson.fromJson(it, JsonObject::class.java) }

            if (!response.isSuccessful)
            {
                val errorMessage = parseError(bodyJson)
                    ?: "MineSkin upload failed with HTTP ${response.code}"
                return SkinUploadResult(
                    textureValue = null,
                    textureSignature = null,
                    skinUuid = null,
                    error = errorMessage,
                    responseBody = bodyJson
                )
            }

            return parseSuccessfulResponse(bodyJson)
        }
    }

    private fun parseSuccessfulResponse(bodyJson: JsonObject?): SkinUploadResult
    {
        if (bodyJson == null)
        {
            return SkinUploadResult(
                textureValue = null,
                textureSignature = null,
                skinUuid = null,
                error = "MineSkin returned an empty response body"
            )
        }

        val textureValue = extractString(bodyJson, "data", "texture", "value")
            ?: extractString(bodyJson, "skin", "texture", "data", "value")
        val textureSignature = extractString(bodyJson, "data", "texture", "signature")
            ?: extractString(bodyJson, "skin", "texture", "data", "signature")
        val skinUuid = extractString(bodyJson, "uuid")
            ?: extractString(bodyJson, "data", "uuid")
            ?: extractString(bodyJson, "skin", "uuid")

        val error = if (textureValue.isNullOrBlank() || textureSignature.isNullOrBlank())
        {
            parseError(bodyJson) ?: "MineSkin response did not contain texture data"
        } else
        {
            null
        }

        return SkinUploadResult(
            textureValue = textureValue,
            textureSignature = textureSignature,
            skinUuid = skinUuid,
            error = error,
            responseBody = bodyJson
        )
    }

    private fun parseError(bodyJson: JsonObject?): String?
    {
        if (bodyJson == null)
        {
            return null
        }

        val errorObject = bodyJson.getAsJsonObject("error")
        if (errorObject != null)
        {
            val code = errorObject.get("code")?.takeIf { !it.isJsonNull }?.asString
            val message = errorObject.get("message")?.takeIf { !it.isJsonNull }?.asString
            return listOfNotNull(code, message).joinToString(": ").ifBlank { null }
        }

        return bodyJson.get("error")?.takeIf { !it.isJsonNull }?.asString
            ?: bodyJson.get("message")?.takeIf { !it.isJsonNull }?.asString
    }

    private fun extractString(root: JsonObject, vararg path: String): String?
    {
        var current = root
        for ((index, segment) in path.withIndex())
        {
            val value = current.get(segment) ?: return null
            if (index == path.lastIndex)
            {
                return if (value.isJsonNull) null else value.asString
            }

            if (!value.isJsonObject)
            {
                return null
            }

            current = value.asJsonObject
        }

        return null
    }

    private fun cacheSkinResponse(skinHash: String, textureValue: String, textureSignature: String)
    {
        val jsonData = gson.toJson(
            CachedSkin(
                textureValue,
                textureSignature
            )
        )
        cacheProvider.put(CacheKey.SKIN, skinHash, jsonData.toByteArray(Charsets.UTF_8), config.skinCacheTtlMinutes)
    }

    fun getCachedSkinResponse(skinHash: String): CachedSkin?
    {
        val cachedData = cacheProvider.get(CacheKey.SKIN, skinHash) ?: return null

        val jsonData = String(cachedData, Charsets.UTF_8)
        return gson.fromJson(jsonData, CachedSkin::class.java)
    }

    fun generateSkinHash(base64Data: String): String
    {
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(base64Data.toByteArray(Charsets.UTF_8))
        return  hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun convertABGR8ToPNGBytes(
        base64Data: String,
        width: Int = config.defaultWidth,
        height: Int = config.defaultHeight
    ): ByteArray?
    {
        return generatePNGBytes(base64Data, width, height)
    }

    private fun generatePNGBytes(base64Data: String, width: Int, height: Int): ByteArray?
    {
        val decodedBytes = Base64.getDecoder().decode(base64Data)

        val expectedSize = width * height * 4
        if (decodedBytes.size != expectedSize)
        {
            println("expected $expectedSize bytes, got ${decodedBytes.size} bytes")
        }

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        var byteIndex = 0
        for (y in 0 until height)
        {
            for (x in 0 until width)
            {
                if (byteIndex + 3 < decodedBytes.size)
                {
                    // ABGR8 format: Alpha, Blue, Green, Red (each 8 bits)
                    val a = decodedBytes[byteIndex].toInt() and 0xFF
                    val b = decodedBytes[byteIndex + 1].toInt() and 0xFF
                    val g = decodedBytes[byteIndex + 2].toInt() and 0xFF
                    val r = decodedBytes[byteIndex + 3].toInt() and 0xFF

                    // Convert to ARGB format for BufferedImage
                    val argb = (a shl 24) or (r shl 16) or (g shl 8) or b

                    image.setRGB(x, y, argb)
                    byteIndex += 4
                }
            }
        }

        // Convert to PNG bytes
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", outputStream)
        return outputStream.toByteArray()
    }

    fun close()
    {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
        cacheProvider.close()
    }
}
