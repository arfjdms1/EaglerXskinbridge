package mc.arch.skin.bridge.cache

enum class CacheKey { SKIN, PNG }

interface CacheProvider {
    fun put(key: CacheKey, id: String, value: ByteArray, ttlMinutes: Long?): Boolean
    fun get(key: CacheKey, id: String): ByteArray?
    fun exists(key: CacheKey, id: String): Boolean
    fun delete(key: CacheKey, id: String): Boolean
    fun clear(): Boolean
    fun close()
}

data class CachedSkin(val skinValue: String, val skinSignature: String)
