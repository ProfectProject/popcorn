package com.popcorn.payment.event.standard

import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

/**
 * 표준 결제 이벤트 발행자 (Kotlin 버전)
 * 새로운 표준 이벤트 구조로 Redis Stream에 이벤트를 발행
 */
@Component
class StandardPaymentEventPublisher(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    private val log = LoggerFactory.getLogger(StandardPaymentEventPublisher::class.java)

    companion object {
        // 새로운 표준 Stream 이름
        private const val STANDARD_PAYMENT_EVENTS_STREAM = "standard-payment-events"
    }

    /**
     * 표준 결제 생성 이벤트 발행
     */
    fun publishPaymentCreatedEvent(event: StandardPaymentCreatedEvent) {
        publishStandardEvent(event, event.toStreamMap())
    }

    /**
     * 표준 결제 승인 이벤트 발행
     */
    fun publishPaymentApprovedEvent(event: StandardPaymentApprovedEvent) {
        publishStandardEvent(event, event.toStreamMap())
    }

    /**
     * 표준 이벤트 Redis Stream 발행
     */
    private fun publishStandardEvent(event: StandardBaseEvent, eventData: Map<String, String>) {
        try {
            log.info("🚀 [STANDARD-PAYMENT] 표준 이벤트 발행 시작 - eventType: {}, eventId: {}",
                event.eventType, event.eventId)

            // StringRecord로 Redis Stream에 발행
            val record = StreamRecords.string(eventData)
                .withStreamKey(STANDARD_PAYMENT_EVENTS_STREAM)

            val recordId = redisTemplate.opsForStream<String, Any>().add(record)?.value

            log.info("✅ [STANDARD-PAYMENT] 표준 이벤트 발행 완료 - eventType: {}, eventId: {}, recordId: {}",
                event.eventType, event.eventId, recordId)

        } catch (e: Exception) {
            log.error("❌ [STANDARD-PAYMENT] 표준 이벤트 발행 실패 - eventType: {}, eventId: {}, error: {}",
                event.eventType, event.eventId, e.message, e)
        }
    }
}