package mc.arch.skin.bridge

import java.io.IOException

fun main(args: Array<String>)
{
    val config = try
    {
        SkinConversionConfig.fromFile("config.toml")
    } catch (exception: IOException)
    {
        System.err.println("Failed to initialize config.toml: ${exception.message}")
        exception.printStackTrace()
        return
    }

    require(config.mineSkinApiKey.isNotBlank()) {
        "config.toml must define [mineskin].api_key before starting SkinBridge"
    }

    SkinConversionDataSync.start(config)
    Runtime.getRuntime().addShutdownHook(
        Thread({
            SkinConversionDataSync.shutdown()
        }, "skinbridge-shutdown")
    )

    println(
        "SkinBridge started. Redis=${config.redisHost}:${config.redisPort}, " +
            "convertChannel=${config.convertChannel}, availableChannel=${config.availableChannel}"
    )

    SkinConversionDataSync.await()
}
