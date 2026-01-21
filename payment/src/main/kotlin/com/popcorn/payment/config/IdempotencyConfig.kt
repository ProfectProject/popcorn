package com.popcorn.payment.config

import com.popcorn.common.cache.CoroutineIdempotencyService
import com.popcorn.common.cache.RedisCoroutineIdempotencyService
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisTemplate

@Configuration
class IdempotencyConfig {

    @Bean
    @ConditionalOnMissingBean(CoroutineIdempotencyService::class)
    fun coroutineIdempotencyService(
        redisTemplate: RedisTemplate<String, Any>
    ): CoroutineIdempotencyService {
        return RedisCoroutineIdempotencyService(redisTemplate)
    }
}
