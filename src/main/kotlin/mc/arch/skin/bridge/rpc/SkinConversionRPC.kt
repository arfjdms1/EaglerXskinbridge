package mc.arch.skin.bridge.rpc

import com.google.gson.Gson
import mc.arch.skin.bridge.SkinConversionConfig
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub

object SkinConversionRPC
{
    private val gson = Gson()

    fun publishConvertRequest(jedisPool: JedisPool, config: SkinConversionConfig, request: SkinConversionRequest)
    {
        val message = gson.toJson(request)
        jedisPool.resource.use { jedis ->
            jedis.publish(config.convertChannel, message)
        }
    }

    fun publishAvailable(jedisPool: JedisPool, config: SkinConversionConfig, request: NewSkinAvailableRequest)
    {
        val message = gson.toJson(request)
        jedisPool.resource.use { jedis ->
            jedis.publish(config.availableChannel, message)
        }
    }

    fun startSubscriber(
        jedisPool: JedisPool,
        config: SkinConversionConfig,
        onConvertRequest: (SkinConversionRequest) -> Unit
    ): Thread
    {
        return Thread(
            {
                while (!Thread.currentThread().isInterrupted)
                {
                    try
                    {
                        jedisPool.resource.use { jedis ->
                            jedis.subscribe(object : JedisPubSub()
                            {
                                override fun onMessage(channel: String, message: String)
                                {
                                    if (channel != config.convertChannel)
                                    {
                                        return
                                    }

                                    runCatching {
                                        gson.fromJson(message, SkinConversionRequest::class.java)
                                    }.onSuccess(onConvertRequest).onFailure {
                                        System.err.println("Failed to parse conversion request: ${it.message}")
                                    }
                                }
                            }, config.convertChannel)
                        }
                    } catch (exception: Exception)
                    {
                        System.err.println("Redis subscriber stopped unexpectedly: ${exception.message}")
                        Thread.sleep(1_000L)
                    }
                }
            },
            "skinbridge-redis-subscriber"
        ).apply {
            isDaemon = true
            start()
        }
    }
}
