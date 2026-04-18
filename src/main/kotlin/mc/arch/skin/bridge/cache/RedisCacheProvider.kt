package mc.arch.skin.bridge.cache

import redis.clients.jedis.JedisPool

class RedisCacheProvider(
    private val jedisPool: JedisPool
) : CacheProvider
{
    override fun put(key: CacheKey, id: String, value: ByteArray, ttlMinutes: Long?): Boolean
    {
        val redisKey = redisKey(key, id)
        jedisPool.resource.use { jedis ->
            if (ttlMinutes == null)
            {
                jedis.set(redisKey.toByteArray(Charsets.UTF_8), value)
            } else
            {
                jedis.setex(redisKey.toByteArray(Charsets.UTF_8), ttlMinutes * 60, value)
            }
        }
        return true
    }

    override fun get(key: CacheKey, id: String): ByteArray? =
        jedisPool.resource.use { jedis ->
            jedis.get(redisKey(key, id).toByteArray(Charsets.UTF_8))
        }

    override fun exists(key: CacheKey, id: String): Boolean =
        jedisPool.resource.use { jedis ->
            jedis.exists(redisKey(key, id))
        }

    override fun delete(key: CacheKey, id: String): Boolean
    {
        jedisPool.resource.use { jedis ->
            jedis.del(redisKey(key, id))
        }
        return true
    }

    override fun clear(): Boolean
    {
        jedisPool.resource.use { jedis ->
            val keys = jedis.keys("skinbridge:cache:*")
            if (keys.isNotEmpty())
            {
                jedis.del(*keys.toTypedArray())
            }
        }
        return true
    }

    override fun close()
    {
    }

    private fun redisKey(key: CacheKey, id: String): String = "skinbridge:cache:${key.name.lowercase()}:$id"
}
