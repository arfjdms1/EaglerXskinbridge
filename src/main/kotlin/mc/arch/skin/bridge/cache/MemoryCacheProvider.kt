package mc.arch.skin.bridge.cache

import java.util.concurrent.ConcurrentHashMap

class MemoryCacheProvider : CacheProvider {
    private val store = ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(val value: ByteArray, val expiry: Long?)

    override fun put(key: CacheKey, id: String, value: ByteArray, ttlMinutes: Long?): Boolean {
        val expiry = ttlMinutes?.let { System.currentTimeMillis() + it * 60 * 1000 }
        store["${key.name}:$id"] = CacheEntry(value, expiry)
        return true
    }

    override fun get(key: CacheKey, id: String): ByteArray? {
        val entry = store["${key.name}:$id"] ?: return null
        if (entry.expiry != null && System.currentTimeMillis() > entry.expiry) {
            store.remove("${key.name}:$id")
            return null
        }
        return entry.value
    }

    override fun exists(key: CacheKey, id: String): Boolean {
        return get(key, id) != null
    }

    override fun delete(key: CacheKey, id: String): Boolean {
        return store.remove("${key.name}:$id") != null
    }

    override fun clear(): Boolean {
        store.clear()
        return true
    }

    override fun close() {
        store.clear()
    }
}
