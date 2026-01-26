package com.popcorn.payment.config

import com.popcorn.payment.event.PaymentRedisEventListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter

/**
 * Payment 서비스 Redis Pub/Sub 이벤트 설정
 *
 * 주문/결제 관련 이벤트를 수신하여 로그 및 후속 처리 기반을 마련
 */
@Configuration
@ConditionalOnProperty(name = ["redis.events.enabled"], havingValue = "true", matchIfMissing = true)
class PaymentRedisEventConfig(
    private val paymentRedisEventListener: PaymentRedisEventListener
) {

    @Bean
    fun paymentRedisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)

        // 주문 관련 이벤트 구독
        container.addMessageListener(
            MessageListenerAdapter(paymentRedisEventListener),
            PatternTopic("events:order-*")
        )

        // 결제 관련 이벤트 구독
        container.addMessageListener(
            MessageListenerAdapter(paymentRedisEventListener),
            PatternTopic("events:payment-*")
        )

        // 재고 관련 이벤트 구독
        container.addMessageListener(
            MessageListenerAdapter(paymentRedisEventListener),
            PatternTopic("events:inventory-*")
        )

        // 모든 이벤트 구독 (모니터링 목적)
        container.addMessageListener(
            MessageListenerAdapter(paymentRedisEventListener),
            PatternTopic("events:*")
        )

        return container
    }
}
