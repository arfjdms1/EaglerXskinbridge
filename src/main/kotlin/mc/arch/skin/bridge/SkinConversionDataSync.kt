package mc.arch.skin.bridge

import mc.arch.skin.bridge.cache.RedisCacheProvider
import mc.arch.skin.bridge.rpc.SkinConversionRPC
import mc.arch.skin.bridge.rpc.handler.SkinConversionHandler
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisPool
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CompletableFuture

object SkinConversionDataSync
{
    private lateinit var config: SkinConversionConfig
    private lateinit var jedisPool: JedisPool
    private lateinit var agent: SkinConversionAgent
    private lateinit var queue: SkinConversionQueue
    private lateinit var handler: SkinConversionHandler
    private val runningLatch = CountDownLatch(1)

    fun start(config: SkinConversionConfig)
    {
        if (::queue.isInitialized)
        {
            queue.shutdown()
        }

        this.config = config
        this.jedisPool = createPool(config)

        val cacheProvider = RedisCacheProvider(jedisPool)
        this.agent = SkinConversionAgent(config = config, cacheProvider = cacheProvider)
        this.queue = agent.withQueue(maxConcurrentJobs = config.maxConcurrentJobs)
        this.handler = SkinConversionHandler(
            queue = queue,
            publisher = { payload ->
                SkinConversionRPC.publishAvailable(jedisPool, config, payload)
            }
        )

        SkinConversionRPC.startSubscriber(
            jedisPool = jedisPool,
            config = config,
            onConvertRequest = handler::handle
        )
    }

    fun toQueue() = queue
    fun toAgent() = agent
    fun toPool() = jedisPool
    fun config() = config

    fun submitTask(
        playerId: UUID,
        base64Data: String,
        taskId: String = SkinConversionQueue.Companion.generateTaskId()
    ): CompletableFuture<TaskResult> = queue.submitTask(
        playerId,
        base64Data,
        taskId
    )

    fun await()
    {
        runningLatch.await()
    }

    fun shutdown()
    {
        if (::queue.isInitialized)
        {
            queue.shutdown()
        }
        if (::agent.isInitialized)
        {
            agent.close()
        }
        if (::jedisPool.isInitialized)
        {
            jedisPool.close()
        }
        runningLatch.countDown()
    }

    private fun createPool(config: SkinConversionConfig): JedisPool
    {
        val clientConfig = DefaultJedisClientConfig.builder()
            .user(config.redisUsername)
            .password(config.redisPassword)
            .database(config.redisDatabase)
            .timeoutMillis(config.redisTimeoutMillis)
            .build()

        return JedisPool(
            HostAndPort(config.redisHost, config.redisPort),
            clientConfig
        )
    }
}
