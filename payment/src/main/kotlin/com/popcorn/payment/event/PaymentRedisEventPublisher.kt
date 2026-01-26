package com.popcorn.payment.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class PaymentRedisEventPublisher(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(PaymentRedisEventPublisher::class.java)

    fun publish(event: Any) {
        val topic = resolveTopic(event) ?: return
        try {
            val payload = objectMapper.writeValueAsString(event)
            val subscribers = redisTemplate.convertAndSend(topic, payload)
            log.info(
                "Redis 이벤트 발행: topic={} subscribers={} payload={}",
                topic,
                subscribers,
                summarize(payload)
            )
        } catch (e: Exception) {
            log.error("Redis 이벤트 발행 실패: topic={} error={}", topic, e.message, e)
        }
    }

    private fun resolveTopic(event: Any): String? {
        return when (event) {
            is PaymentCreatedEvent -> "events:payment-created"
            is PaymentApprovedEvent -> "events:payment-approved"
            is PaymentFailedEvent -> "events:payment-failed"
            is PaymentCancelledEvent -> "events:payment-cancelled"
            is PaymentCancelFailedEvent -> "events:payment-cancel-failed"
            is PaymentExpiredEvent -> "events:payment-expired"
            is PaymentCompletedEvent -> "events:payment-completed"
            is PaymentSuccessEvent -> "events:payment-success"
            is QrCodeGenerationRequestedEvent -> "events:qr-generation-requested"
            is QrCodeInvalidationRequestedEvent -> "events:qr-invalidation-requested"
            is InventoryConfirmationRequestedEvent -> "events:inventory-confirmation-requested"
            is OrderStatusUpdateRequestedEvent -> "events:order-status-update-requested"
            is PaymentCancelRetryEvent -> "events:payment-cancel-retry"
            is PaymentCancelFinalFailureEvent -> "events:payment-cancel-final-failure"
            else -> null
        }
    }

    private fun summarize(value: String): String {
        val limit = 1000
        return if (value.length <= limit) value else value.substring(0, limit) + "...(truncated)"
    }
}
