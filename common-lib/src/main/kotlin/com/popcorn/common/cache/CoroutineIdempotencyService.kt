package com.popcorn.common.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration

/**
 * 코루틴 기반 멱등성 서비스 (Kotlin 버전)
 */
interface CoroutineIdempotencyService {
    suspend fun <T> execute(key: String, operation: suspend () -> T): T
    suspend fun checkExists(key: String): Boolean
    suspend fun invalidate(key: String): Boolean
}

/**
 * Redis 기반 코루틴 멱등성 서비스 구현체
 */
@Service
class RedisCoroutineIdempotencyService(
    private val redisTemplate: RedisTemplate<String, Any>
) : CoroutineIdempotencyService {

    private val defaultTtl = Duration.ofMinutes(10)
    private val lockTtl = Duration.ofSeconds(30)

    /**
     * 멱등성 보장 실행
     */
    override suspend fun <T> execute(key: String, operation: suspend () -> T): T = withContext(Dispatchers.IO) {
        val cached = getCachedResult(key)
        if (cached != null) {
            @Suppress("UNCHECKED_CAST")
            return@withContext cached as T
        }

        val lockKey = lockKey(key)
        val lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", lockTtl) == true
        if (!lockAcquired) {
            repeat(10) {
                delay(200)
                val retryCached = getCachedResult(key)
                if (retryCached != null) {
                    @Suppress("UNCHECKED_CAST")
                    return@withContext retryCached as T
                }
            }
        }

        try {
            val result = operation()
            storeCachedResult(key, result)
            result
        } finally {
            redisTemplate.delete(lockKey)
        }
    }

    /**
     * 키 존재 여부 확인
     */
    override suspend fun checkExists(key: String): Boolean = withContext(Dispatchers.IO) {
        redisTemplate.hasKey(resultKey(key)) == true
    }

    /**
     * 캐시 무효화
     */
    override suspend fun invalidate(key: String): Boolean = withContext(Dispatchers.IO) {
        redisTemplate.delete(resultKey(key))
        true
    }

    private suspend fun getCachedResult(key: String): Any? {
        return redisTemplate.opsForValue().get(resultKey(key))
    }

    private suspend fun storeCachedResult(key: String, result: Any?) {
        redisTemplate.opsForValue().set(resultKey(key), result ?: "NULL", defaultTtl)
    }

    private fun resultKey(key: String): String = "idempotency:result:$key"
    private fun lockKey(key: String): String = "idempotency:lock:$key"
}
