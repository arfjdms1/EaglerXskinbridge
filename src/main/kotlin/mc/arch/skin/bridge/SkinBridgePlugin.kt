package mc.arch.skin.bridge

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.Player
import net.lax1dude.eaglercraft.backend.server.api.velocity.event.EaglercraftRegisterSkinEvent
import net.skinsrestorer.api.SkinsRestorerProvider
import net.skinsrestorer.api.property.SkinProperty
import net.skinsrestorer.api.property.SkinIdentifier
import org.slf4j.Logger
import java.nio.file.Path
import java.util.Base64
import mc.arch.skin.bridge.cache.MemoryCacheProvider

@Plugin(id = "eaglerxskinbridge", dependencies = [Dependency(id = "eaglerxserver"), Dependency(id = "skinsrestorer")])
class SkinBridgePlugin @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dataDirectory: Path
) {
    private val config: SkinConversionConfig = SkinConversionConfig.fromFile(dataDirectory)
    private val agent: SkinConversionAgent = SkinConversionAgent(config, MemoryCacheProvider())

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        logger.info("[EaglerXskinbridge] Plugin successfully initialized and listening for Eaglercraft skins!")
    }

    @Subscribe
    fun onSkinUpload(event: EaglercraftRegisterSkinEvent) {
        val playerId = event.loginConnection.uniqueId
        val username = event.loginConnection.username
        
        logger.info("[EaglerXskinbridge] EaglercraftRegisterSkinEvent fired for player: {}", username)

        val skin = event.eaglerSkin
        
        logger.info("[EaglerXskinbridge] Player {} skin details -> isCustom: {}, isPreset: {}", username, skin.isSkinCustom, skin.isSkinPreset)

        if (skin.isSkinPreset) {
            val presetId = skin.presetSkinId
            val enumSkin = skin.presetSkin
            val presetName = enumSkin?.name ?: "UNKNOWN"
            val variant = if (presetName.contains("ALEX")) "slim" else "classic"
            
            logger.info("[EaglerXskinbridge] Player {} is using preset skin {} (ID: {}). Uploading to MineSkin...", username, presetName, presetId)

            server.scheduler.buildTask(this, Runnable {
                try {
                    val result = agent.uploadPresetToMineSkin(presetId, variant).join()
                    
                    if (result.error == null && result.textureValue != null && result.textureSignature != null) {
                        val skinsRestorer = SkinsRestorerProvider.get()
                        val property = SkinProperty.of(result.textureValue, result.textureSignature)
                        
                        val skinId = "preset_$presetId"
                        val skinIdentifier = SkinIdentifier.ofCustom(skinId)
                        
                        skinsRestorer.skinStorage.setCustomSkinData(skinId, property)
                        skinsRestorer.playerStorage.setSkinIdOfPlayer(playerId, skinIdentifier)
                        
                        val player = server.getPlayer(playerId).orElse(null)
                        if (player != null) {
                            skinsRestorer.getSkinApplier(Player::class.java).applySkin(player)
                        }
                        
                        logger.info("[EaglerXskinbridge] Successfully mapped preset skin for player {}", username)
                    } else {
                        logger.error("[EaglerXskinbridge] Preset skin upload failed for {}: {}", username, result.error)
                    }
                } catch (e: Exception) {
                    logger.error("[EaglerXskinbridge] Error processing preset skin for {}", username, e)
                }
            }).schedule()
            return
        }

        if (!skin.isSkinCustom) {
            logger.info("[EaglerXskinbridge] Player {} is using a default skin that is neither preset nor custom. Skipping MineSkin upload.", username)
            return
        }

        val rawBytes = skin.customSkinPixels_ABGR8_64x64
        if (rawBytes == null) {
            logger.warn("[EaglerXskinbridge] Player {} has a custom skin but the raw bytes are null!", username)
            return
        }
        
        val isSlim = skin.customSkinModelId?.name == "ALEX"
        val variant = if (isSlim) "slim" else "classic"
        
        logger.info("[EaglerXskinbridge] Player {} has a custom {} skin! Uploading to MineSkin...", username, variant)

        server.scheduler.buildTask(this, Runnable {
            try {
                val base64Data = Base64.getEncoder().encodeToString(rawBytes)
                val result = agent.convertAndUploadToMineSkin(base64Data, variant).join()
                
                if (result.error == null && result.textureValue != null && result.textureSignature != null) {
                    val skinsRestorer = SkinsRestorerProvider.get()
                    val property = SkinProperty.of(result.textureValue, result.textureSignature)
                    
                    val skinId = result.skinUuid ?: username
                    val skinIdentifier = SkinIdentifier.ofCustom(skinId)
                    
                    skinsRestorer.skinStorage.setCustomSkinData(skinId, property)
                    skinsRestorer.playerStorage.setSkinIdOfPlayer(playerId, skinIdentifier)
                    
                    val player = server.getPlayer(playerId).orElse(null)
                    if (player != null) {
                        skinsRestorer.getSkinApplier(Player::class.java).applySkin(player)
                    }
                    
                    logger.info("[EaglerXskinbridge] Successfully mapped custom skin for player {}", username)
                } else {
                    logger.error("[EaglerXskinbridge] Skin upload failed for {}: {}", username, result.error)
                }
            } catch (e: Exception) {
                logger.error("[EaglerXskinbridge] Error processing skin for {}", username, e)
            }
        }).schedule()
    }
}
