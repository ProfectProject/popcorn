package com.popcorn.payment.event

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.stereotype.Component

/**
 * Payment 서비스 범용 이벤트 리스너
 * - Redis 이벤트 구독
 * - Spring Application 이벤트 구독
 * - 모든 이벤트를 로그로 기록
 */
@Component
class PaymentRedisEventListener : MessageListener {
    private val log = LoggerFactory.getLogger(PaymentRedisEventListener::class.java)

    override fun onMessage(message: Message, pattern: ByteArray?) {
        try {
            val channel = String(message.channel)
            val body = String(message.body)

            log.info("🔔 [PAYMENT] Redis 이벤트 수신 - channel: {}, body: {}", channel, body)

            handleRedisEvent(channel, body)
        } catch (e: Exception) {
            log.error(
                "🚨 [PAYMENT] Redis 이벤트 처리 실패 - message: {}, error: {}",
                String(message.body),
                e.message,
                e
            )
        }
    }

    private fun handleRedisEvent(channel: String, body: String) {
        try {
            when (channel) {
                "events:order-created" -> log.info("📦 [PAYMENT] 주문 생성 이벤트 수신 - {}", body)
                "events:order-paid" -> log.info("💳 [PAYMENT] 주문 결제 완료 이벤트 수신 - {}", body)
                "events:order-cancelled" -> log.info("🧾 [PAYMENT] 주문 취소 이벤트 수신 - {}", body)
                "events:payment-created" -> log.info("🧾 [PAYMENT] 결제 생성 이벤트 수신 - {}", body)
                "events:payment-approved" -> log.info("✅ [PAYMENT] 결제 승인 이벤트 수신 - {}", body)
                "events:payment-failed" -> log.warn("❌ [PAYMENT] 결제 실패 이벤트 수신 - {}", body)
                "events:payment-cancelled" -> log.info("↩️ [PAYMENT] 결제 취소 이벤트 수신 - {}", body)
                "events:inventory-confirmation-requested" -> log.info("📦 [PAYMENT] 재고 확정 요청 이벤트 수신 - {}", body)
                "events:inventory-restore-requested" -> log.info("🔄 [PAYMENT] 재고 복구 요청 이벤트 수신 - {}", body)
                else -> log.info("🔔 [PAYMENT] 기타 이벤트 수신 - channel: {}, body: {}", channel, body)
            }
        } catch (e: Exception) {
            log.error("🚨 [PAYMENT] 이벤트 처리 실패 - channel: {}, error: {}", channel, e.message, e)
        }
    }

    @EventListener
    fun handleApplicationEvent(event: Any) {
        try {
            val eventType = event.javaClass.simpleName
            if (eventType.contains("Payment") || eventType.contains("Order")) {
                log.info("🌟 [PAYMENT] Application 이벤트 수신 - type: {}, event: {}", eventType, event.toString())
            } else {
                log.debug("🔔 [PAYMENT] Application 이벤트 수신 - type: {}", eventType)
            }
        } catch (e: Exception) {
            log.error(
                "🚨 [PAYMENT] Application 이벤트 처리 실패 - event: {}, error: {}",
                event.javaClass.simpleName,
                e.message,
                e
            )
        }
    }
}
