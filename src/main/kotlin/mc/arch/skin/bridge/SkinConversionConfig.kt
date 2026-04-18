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
    var fallbackHashEnabled: Boolean = true
) {
    companion object {
        fun fromFile(dataDirectory: Path): SkinConversionConfig {
            val configPath = dataDirectory.resolve("config.toml")
            if (Files.notExists(configPath)) {
                writeDefaultConfig(configPath)
            }

            val toml = Toml().read(configPath.toFile())
            val defaults = SkinConversionConfig()

            return SkinConversionConfig(
                mineSkinApiKey = toml.getString("mineskin.api_key", defaults.mineSkinApiKey),
                userAgent = toml.getString("mineskin.user_agent", defaults.userAgent),
                defaultWidth = toml.getLong("mineskin.default_width", defaults.defaultWidth.toLong())?.toInt() ?: defaults.defaultWidth,
                defaultHeight = toml.getLong("mineskin.default_height", defaults.defaultHeight.toLong())?.toInt() ?: defaults.defaultHeight,
                maxConcurrentJobs = toml.getLong("settings.max_concurrent_jobs", defaults.maxConcurrentJobs.toLong())?.toInt() ?: defaults.maxConcurrentJobs,
                cacheEnabled = toml.getBoolean("cache.enabled", defaults.cacheEnabled) ?: defaults.cacheEnabled,
                skinCacheTtlMinutes = toml.getLong("cache.skin_cache_ttl_minutes", defaults.skinCacheTtlMinutes) ?: defaults.skinCacheTtlMinutes,
                pngCacheTtlMinutes = toml.getLong("cache.png_cache_ttl_minutes", defaults.pngCacheTtlMinutes) ?: defaults.pngCacheTtlMinutes,
                fallbackHashEnabled = toml.getBoolean("cache.fallback_hash_enabled", defaults.fallbackHashEnabled) ?: defaults.fallbackHashEnabled
            )
        }

        @Throws(IOException::class)
        private fun writeDefaultConfig(path: Path) {
            path.parent?.let { Files.createDirectories(it) }
            Files.writeString(path, defaultConfigToml(), StandardCharsets.UTF_8)
        }

        private fun defaultConfigToml(): String =
            """
            # SkinBridge configuration.
            # This file is generated automatically on first startup.
            
            [mineskin]
            # API key used for uploads to https://api.mineskin.org/generate/upload. (Optional)
            api_key = ""
            # User-Agent header sent with MineSkin requests.
            user_agent = "EaglercraftSkinBridge/v1.0"
            # Dimensions of incoming Eaglercraft ABGR8 skin payloads.
            default_width = 64
            default_height = 64
            
            [settings]
            # Maximum number of concurrent MineSkin upload jobs processed by this worker.
            max_concurrent_jobs = 5
            
            [cache]
            # ttl_minutes
            # Enables internal PNG and signed skin response caching.
            enabled = true
            # TTL for signed MineSkin responses, in minutes.
            skin_cache_ttl_minutes = 10080
            # TTL for generated PNG payloads, in minutes.
            png_cache_ttl_minutes = 10080
            # Preserve fallback hash behavior from the original service.
            fallback_hash_enabled = true
            """.trimIndent() + "\n"
    }
}
