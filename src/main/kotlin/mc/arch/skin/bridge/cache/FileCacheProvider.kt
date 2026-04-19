package mc.arch.skin.bridge.cache

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

class FileCacheProvider(private val cacheDir: Path) : CacheProvider {
    private val memoryStore = ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(val value: ByteArray, val expiry: Long?)

    init {
        if (!cacheDir.exists()) {
            cacheDir.createDirectories()
        }
        loadDiskCache()
    }

    private fun loadDiskCache() {
        if (!cacheDir.exists()) return
        val currentTime = System.currentTimeMillis()
        Files.walk(cacheDir).filter { Files.isRegularFile(it) }.forEach { filePath ->
            try {
                val fileName = filePath.fileName.toString()
                val parts = fileName.split("_", limit = 2)
                if (parts.size == 2) {
                    val expiryStr = parts[0]
                    val internalKey = parts[1]

                    if (expiryStr != "PERM") {
                        filePath.deleteIfExists()
                    } else {
                        val bytes = filePath.readBytes()
                        val rawKeyStr = String(java.util.Base64.getDecoder().decode(internalKey))
                        memoryStore[rawKeyStr] = CacheEntry(bytes, null)
                    }
                } else {
                    filePath.deleteIfExists()
                }
            } catch (e: Exception) {
                filePath.deleteIfExists()
            }
        }
    }

    private fun saveToDisk(keyStr: String, entry: CacheEntry) {
        val safeFileNameKey = java.util.Base64.getEncoder().encodeToString(keyStr.toByteArray())
        val expiryPrefix = entry.expiry?.toString() ?: "PERM"
        
        // Delete old versions of this key
        Files.walk(cacheDir).filter { Files.isRegularFile(it) }.forEach { filePath ->
            val fName = filePath.fileName.toString()
            if (fName.endsWith("_$safeFileNameKey")) {
                filePath.deleteIfExists()
            }
        }

        val file = cacheDir.resolve("${expiryPrefix}_${safeFileNameKey}")
        file.writeBytes(entry.value)
    }

    override fun put(key: CacheKey, id: String, value: ByteArray, ttlMinutes: Long?): Boolean {
        val expiry = ttlMinutes?.let { System.currentTimeMillis() + it * 60 * 1000 }
        val keyStr = "${key.name}:$id"
        val entry = CacheEntry(value, expiry)
        memoryStore[keyStr] = entry
        if (ttlMinutes == null) {
            saveToDisk(keyStr, entry)
        }
        return true
    }

    override fun get(key: CacheKey, id: String): ByteArray? {
        val keyStr = "${key.name}:$id"
        val entry = memoryStore[keyStr] ?: return null
        if (entry.expiry != null && System.currentTimeMillis() > entry.expiry) {
            memoryStore.remove(keyStr)
            return null
        }
        return entry.value
    }

    override fun exists(key: CacheKey, id: String): Boolean {
        return get(key, id) != null
    }

    override fun delete(key: CacheKey, id: String): Boolean {
        val keyStr = "${key.name}:$id"
        val removed = memoryStore.remove(keyStr) != null
        val safeFileNameKey = java.util.Base64.getEncoder().encodeToString(keyStr.toByteArray())
        Files.walk(cacheDir).filter { Files.isRegularFile(it) }.forEach { filePath ->
            if (filePath.fileName.toString().endsWith("_$safeFileNameKey")) {
                filePath.deleteIfExists()
            }
        }
        return removed
    }

    override fun clear(): Boolean {
        memoryStore.clear()
        Files.walk(cacheDir).filter { Files.isRegularFile(it) }.forEach { it.deleteIfExists() }
        return true
    }

    override fun close() {
        memoryStore.clear()
    }
}
