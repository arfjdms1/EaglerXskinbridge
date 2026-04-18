package mc.arch.skin.bridge

import com.moandjiezana.toml.Toml
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

data class SkinConversionConfig(
    var mineSkinApiKey: String = "",
    var userAgent: String = "EaglercraftSkinBridge/v1.0",
    var defaultWidth: Int = 64,
    var defaultHeight: Int = 64,
    var maxConcurrentJobs: Int = 5,
    var cacheEnabled: Boolean = true,
    var skinCacheTtlMinutes: Long = 1440 * 7,
    var pngCacheTtlMinutes: Long = 1440 * 7,
    var fallbackHashEnabled: Boolean = true,
    var redisHost: String = "127.0.0.1",
    var redisPort: Int = 6379,
    var redisUsername: String? = null,
    var redisPassword: String? = null,
    var redisDatabase: Int = 0,
    var redisTimeoutMillis: Int = 2_000,
    var convertChannel: String = "skinbridge:convert",
    var availableChannel: String = "skinbridge:available"
)
{
    companion object
    {
        fun fromFile(path: String): SkinConversionConfig
        {
            val configPath = Path.of(path)
            if (Files.notExists(configPath))
            {
                writeDefaultConfig(configPath)
            }

            val toml = Toml().read(configPath.toFile())
            val defaults = SkinConversionConfig()

            return SkinConversionConfig(
                mineSkinApiKey = toml.getString("mineskin.api_key", defaults.mineSkinApiKey),
                userAgent = toml.getString("mineskin.user_agent", defaults.userAgent),
                defaultWidth = toml.getLong("mineskin.default_width", defaults.defaultWidth.toLong()).toInt(),
                defaultHeight = toml.getLong("mineskin.default_height", defaults.defaultHeight.toLong()).toInt(),
                maxConcurrentJobs = toml.getLong("settings.max_concurrent_jobs", defaults.maxConcurrentJobs.toLong()).toInt(),
                cacheEnabled = toml.getBoolean("cache.enabled", defaults.cacheEnabled),
                skinCacheTtlMinutes = toml.getLong("cache.skin_cache_ttl_minutes", defaults.skinCacheTtlMinutes),
                pngCacheTtlMinutes = toml.getLong("cache.png_cache_ttl_minutes", defaults.pngCacheTtlMinutes),
                fallbackHashEnabled = toml.getBoolean("cache.fallback_hash_enabled", defaults.fallbackHashEnabled),
                redisHost = toml.getString("redis.host", defaults.redisHost),
                redisPort = toml.getLong("redis.port", defaults.redisPort.toLong()).toInt(),
                redisUsername = toml.getString("redis.username", defaults.redisUsername).nullIfBlank(),
                redisPassword = toml.getString("redis.password", defaults.redisPassword).nullIfBlank(),
                redisDatabase = toml.getLong("redis.database", defaults.redisDatabase.toLong()).toInt(),
                redisTimeoutMillis = toml.getLong("redis.timeout_millis", defaults.redisTimeoutMillis.toLong()).toInt(),
                convertChannel = toml.getString("redis.convert_channel", defaults.convertChannel),
                availableChannel = toml.getString("redis.available_channel", defaults.availableChannel)
            )
        }

        @Throws(IOException::class)
        private fun writeDefaultConfig(path: Path)
        {
            path.parent?.let { Files.createDirectories(it) }
            Files.writeString(path, defaultConfigToml(), StandardCharsets.UTF_8)
        }

        private fun defaultConfigToml(): String =
            """
            # SkinBridge configuration.
            # This file is generated automatically on first startup.
            # Update the values below to match your Redis and MineSkin environment.
            
            [mineskin]
            # API key used for uploads to https://api.mineskin.org/generate/upload.
            api_key = ""
            # User-Agent header sent with MineSkin requests.
            user_agent = "EaglercraftSkinBridge/v1.0"
            # Dimensions of incoming Eaglercraft ABGR8 skin payloads.
            default_width = 64
            default_height = 64
            
            [redis]
            # Redis connection target for cache storage and Pub/Sub job distribution.
            host = "127.0.0.1"
            port = 6379
            # Optional ACL username. Leave blank if your Redis deployment does not use one.
            username = ""
            # Optional password. Leave blank for unauthenticated local Redis.
            password = ""
            # Logical Redis database index.
            database = 0
            # Socket timeout for Redis operations, in milliseconds.
            timeout_millis = 2000
            # Pub/Sub channel that receives skin conversion jobs.
            convert_channel = "skinbridge:convert"
            # Pub/Sub channel that broadcasts completed skin signatures.
            available_channel = "skinbridge:available"
            
            [settings]
            # Maximum number of concurrent MineSkin upload jobs processed by this worker.
            max_concurrent_jobs = 5
            
            [cache]
            # Enables Redis-backed PNG and signed skin response caching.
            enabled = true
            # TTL for signed MineSkin responses, in minutes.
            skin_cache_ttl_minutes = 10080
            # TTL for generated PNG payloads, in minutes.
            png_cache_ttl_minutes = 10080
            # Preserve fallback hash behavior from the original service.
            fallback_hash_enabled = true
            """.trimIndent() + "\n"

        private fun String?.nullIfBlank(): String? = this?.takeIf { it.isNotBlank() }
    }
}
