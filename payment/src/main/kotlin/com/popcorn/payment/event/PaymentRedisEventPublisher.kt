package com.popcorn.payment.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.connection.stream.StringRecord
import java.time.LocalDateTime
import org.springframework.stereotype.Component

@Component
class PaymentRedisEventPublisher(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(PaymentRedisEventPublisher::class.java)

    // Stream 이름 상수
    companion object {
        private const val PAYMENT_EVENTS_STREAM = "payment-events"
        private const val QR_EVENTS_STREAM = "qr-events"
        private const val ORDER_EVENTS_STREAM = "order-events"
        private const val INVENTORY_EVENTS_STREAM = "inventory-events"
    }

    fun publish(event: Any) {
        val (streamName, eventType) = resolveStreamAndType(event) ?: return
        try {
            val eventData = mutableMapOf<String, Any>(
                "eventType" to eventType,
                "eventId" to java.util.UUID.randomUUID().toString(),
                "eventTime" to java.time.LocalDateTime.now().toString()
            )

            // 이벤트 객체를 Map으로 변환하여 추가
            val eventJson = objectMapper.writeValueAsString(event)
            val eventMap = objectMapper.readValue(eventJson, Map::class.java) as Map<String, Any>
            eventData.putAll(eventMap)

            // eventTime을 현재 시간으로 설정
            eventData["eventTime"] = LocalDateTime.now().toString()

            // Map<String, Any>를 Map<String, String>으로 변환
            val stringEventData = eventData.mapValues { it.value?.toString() ?: "" }

            val streamOps = redisTemplate.opsForStream<String, Any>()
            val record: StringRecord = StreamRecords.string(stringEventData).withStreamKey(streamName)
            val recordId = streamOps.add(record)

            log.info(
                "Redis Stream 이벤트 발행 완료: stream={} eventType={} recordId={}",
                streamName,
                eventType,
                recordId
            )
        } catch (e: Exception) {
            log.error("Redis Stream 이벤트 발행 실패: eventType={} error={}", eventType, e.message, e)
        }
    }

    private fun resolveStreamAndType(event: Any): Pair<String, String>? {
        return when (event) {
            is PaymentCreatedEvent -> PAYMENT_EVENTS_STREAM to "payment-created"
            is PaymentApprovedEvent -> PAYMENT_EVENTS_STREAM to "payment-approved"
            is PaymentFailedEvent -> PAYMENT_EVENTS_STREAM to "payment-failed"
            is PaymentCancelledEvent -> PAYMENT_EVENTS_STREAM to "payment-cancelled"
            is PaymentCancelFailedEvent -> PAYMENT_EVENTS_STREAM to "payment-cancel-failed"
            is PaymentExpiredEvent -> PAYMENT_EVENTS_STREAM to "payment-expired"
            is PaymentCompletedEvent -> PAYMENT_EVENTS_STREAM to "payment-completed"
            is PaymentSuccessEvent -> PAYMENT_EVENTS_STREAM to "payment-success"
            is QrCodeGenerationRequestedEvent -> QR_EVENTS_STREAM to "qr-generation-requested"
            is QrCodeInvalidationRequestedEvent -> QR_EVENTS_STREAM to "qr-invalidation-requested"
            is InventoryConfirmationRequestedEvent -> INVENTORY_EVENTS_STREAM to "inventory-confirmation-requested"
            is OrderStatusUpdateRequestedEvent -> ORDER_EVENTS_STREAM to "order-status-update-requested"
            is PaymentCancelRetryEvent -> PAYMENT_EVENTS_STREAM to "payment-cancel-retry"
            is PaymentCancelFinalFailureEvent -> PAYMENT_EVENTS_STREAM to "payment-cancel-final-failure"
            else -> null
        }
    }
}
