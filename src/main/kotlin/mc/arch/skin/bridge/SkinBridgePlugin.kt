package mc.arch.skin.bridge

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
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
    fun onSkinUpload(event: EaglercraftRegisterSkinEvent) {
        val skin = event.eaglerSkin
        if (!skin.isSkinCustom) {
            return
        }

        val rawBytes = skin.customSkinPixels_ABGR8_64x64
        if (rawBytes == null) {
            return
        }
        
        val playerId = event.loginConnection.uniqueId
        val username = event.loginConnection.username

        server.scheduler.buildTask(this, Runnable {
            try {
                val base64Data = Base64.getEncoder().encodeToString(rawBytes)
                val result = agent.convertAndUploadToMineSkin(base64Data).join()
                
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
