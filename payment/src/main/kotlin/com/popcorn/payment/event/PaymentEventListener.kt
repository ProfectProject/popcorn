package com.popcorn.payment.event

import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * 결제 이벤트 리스너
 *
 * 🎪 이벤트 처리 전략:
 * - 비동기 처리로 메인 트랜잭션과 분리
 * - 실패해도 결제 성공에 영향 없음
 * - 재시도 메커니즘 포함
 * - 코루틴으로 높은 동시성 처리
 */
@Component
class PaymentEventListener {

    private val log = LoggerFactory.getLogger(PaymentEventListener::class.java)
    private val eventScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * 결제 생성 이벤트 처리
     * - 로깅
     * - 메트릭 수집
     * - 알림 발송 (필요시)
     */
    @Async
    @EventListener
    fun handlePaymentCreated(event: PaymentCreatedEvent) {
        eventScope.launch {
            try {
                log.info("📝 결제 생성 이벤트 처리: paymentId={}, orderId={}, amount={}원",
                    event.paymentId, event.orderId, event.amount)

                // 메트릭 수집
                recordPaymentCreatedMetric(event)

                // 사용자별 결제 통계 업데이트 (필요시)
                // updateUserPaymentStats(event.customerId, event.amount)

                log.debug("✅ 결제 생성 이벤트 처리 완료: paymentId={}", event.paymentId)

            } catch (e: Exception) {
                log.error("❌ 결제 생성 이벤트 처리 실패: paymentId={}, error={}",
                    event.paymentId, e.message, e)
            }
        }
    }

    /**
     * 결제 승인 이벤트 처리
     * - QR 코드 생성 (checkIns 서비스 호출)
     * - 재고 차감 확정
     * - 주문 완료 처리
     * - 고객 알림
     */
    @Async
    @EventListener
    fun handlePaymentApproved(event: PaymentApprovedEvent) {
        eventScope.launch {
            try {
                log.info("💳 결제 승인 이벤트 처리: paymentId={}, orderId={}, amount={}원",
                    event.paymentId, event.orderId, event.amount)

                // QR 코드 생성 요청 (checkIns 서비스와 연동)
                generateQrCodeForOrder(event.orderId, event.orderNo)

                // 재고 차감 확정 처리
                confirmInventoryDeduction(event.orderId)

                // 고객 알림 발송
                sendPaymentApprovedNotification(event)

                // 메트릭 수집
                recordPaymentApprovedMetric(event)

                log.info("✅ 결제 승인 이벤트 처리 완료: paymentId={}", event.paymentId)

            } catch (e: Exception) {
                log.error("❌ 결제 승인 이벤트 처리 실패: paymentId={}, error={}",
                    event.paymentId, e.message, e)
                // 실패해도 결제는 성공으로 처리 (보상 트랜잭션은 별도 처리)
            }
        }
    }

    /**
     * 결제 실패 이벤트 처리
     * - 재고 복구
     * - 주문 상태 롤백
     * - 실패 알림
     * - 실패 통계 수집
     */
    @Async
    @EventListener
    fun handlePaymentFailed(event: PaymentFailedEvent) {
        eventScope.launch {
            try {
                log.warn("⚠️ 결제 실패 이벤트 처리: paymentId={}, orderId={}, reason={}",
                    event.paymentId, event.orderId, event.failureReason)

                // 재고 복구 처리
                restoreInventory(event.orderId)

                // 주문 상태를 실패로 변경
                updateOrderStatusToFailed(event.orderId, event.failureReason)

                // 고객 실패 알림
                sendPaymentFailedNotification(event)

                // 실패 메트릭 수집
                recordPaymentFailedMetric(event)

                log.info("✅ 결제 실패 이벤트 처리 완료: paymentId={}", event.paymentId)

            } catch (e: Exception) {
                log.error("❌ 결제 실패 이벤트 처리 실패: paymentId={}, error={}",
                    event.paymentId, e.message, e)
            }
        }
    }

    /**
     * 결제 취소 이벤트 처리
     * - 재고 복구
     * - QR 코드 무효화
     * - 환불 처리
     * - 취소 알림
     */
    @Async
    @EventListener
    fun handlePaymentCancelled(event: PaymentCancelledEvent) {
        eventScope.launch {
            try {
                log.info("🔄 결제 취소 이벤트 처리: paymentId={}, orderId={}, reason={}",
                    event.paymentId, event.orderId, event.cancelReason)

                // 재고 복구
                restoreInventory(event.orderId)

                // QR 코드 무효화
                invalidateQrCode(event.orderId)

                // 고객 취소 알림
                sendPaymentCancelledNotification(event)

                // 취소 메트릭 수집
                recordPaymentCancelledMetric(event)

                log.info("✅ 결제 취소 이벤트 처리 완료: paymentId={}", event.paymentId)

            } catch (e: Exception) {
                log.error("❌ 결제 취소 이벤트 처리 실패: paymentId={}, error={}",
                    event.paymentId, e.message, e)
            }
        }
    }

    /**
     * 결제 취소 실패 이벤트 처리
     * - 재시도 큐에 추가
     * - 관리자 알림
     */
    @Async
    @EventListener
    fun handlePaymentCancelFailed(event: PaymentCancelFailedEvent) {
        eventScope.launch {
            try {
                log.error("🚨 결제 취소 실패 이벤트 처리: paymentId={}, reason={}",
                    event.paymentId, event.failureReason)

                // 재시도 큐에 추가
                addToRetryQueue(event)

                // 관리자 알림 (심각한 오류)
                sendCancelFailureAlert(event)

                log.warn("⚠️ 결제 취소 실패 이벤트 처리 완료: paymentId={}, retryCount={}",
                    event.paymentId, event.retryCount)

            } catch (e: Exception) {
                log.error("❌ 결제 취소 실패 이벤트 처리 실패: paymentId={}, error={}",
                    event.paymentId, e.message, e)
            }
        }
    }

    /**
     * 결제 만료 이벤트 처리
     * - 재고 복구
     * - 주문 취소
     */
    @Async
    @EventListener
    fun handlePaymentExpired(event: PaymentExpiredEvent) {
        eventScope.launch {
            try {
                log.info("⏰ 결제 만료 이벤트 처리: paymentId={}, orderId={}",
                    event.paymentId, event.orderId)

                // 재고 복구
                restoreInventory(event.orderId)

                // 주문 상태를 만료로 변경
                updateOrderStatusToExpired(event.orderId)

                log.info("✅ 결제 만료 이벤트 처리 완료: paymentId={}", event.paymentId)

            } catch (e: Exception) {
                log.error("❌ 결제 만료 이벤트 처리 실패: paymentId={}, error={}",
                    event.paymentId, e.message, e)
            }
        }
    }

    // ========================================
    // 헬퍼 메서드들 (실제 구현은 각 서비스와 연동)
    // ========================================

    private suspend fun generateQrCodeForOrder(orderId: java.util.UUID, orderNo: String) {
        // checkIns 서비스와 연동하여 QR 코드 생성
        log.info("🔗 QR 코드 생성 요청: orderId={}, orderNo={}", orderId, orderNo)
        // TODO: checkIns 서비스 API 호출
    }

    private suspend fun confirmInventoryDeduction(orderId: java.util.UUID) {
        // 재고 서비스와 연동하여 재고 차감 확정
        log.info("📦 재고 차감 확정: orderId={}", orderId)
        // TODO: 재고 서비스 API 호출
    }

    private suspend fun restoreInventory(orderId: java.util.UUID) {
        // 재고 서비스와 연동하여 재고 복구
        log.info("🔄 재고 복구: orderId={}", orderId)
        // TODO: 재고 서비스 API 호출
    }

    private suspend fun invalidateQrCode(orderId: java.util.UUID) {
        // checkIns 서비스와 연동하여 QR 코드 무효화
        log.info("❌ QR 코드 무효화: orderId={}", orderId)
        // TODO: checkIns 서비스 API 호출
    }

    private suspend fun updateOrderStatusToFailed(orderId: java.util.UUID, reason: String) {
        log.info("📝 주문 상태 실패 변경: orderId={}, reason={}", orderId, reason)
        // TODO: Order 서비스와 연동
    }

    private suspend fun updateOrderStatusToExpired(orderId: java.util.UUID) {
        log.info("📝 주문 상태 만료 변경: orderId={}", orderId)
        // TODO: Order 서비스와 연동
    }

    private suspend fun sendPaymentApprovedNotification(event: PaymentApprovedEvent) {
        log.info("📧 결제 승인 알림 발송: customerId={}, amount={}원", event.customerId, event.amount)
        // TODO: 알림 서비스와 연동
    }

    private suspend fun sendPaymentFailedNotification(event: PaymentFailedEvent) {
        log.info("📧 결제 실패 알림 발송: customerId={}, reason={}", event.customerId, event.failureReason)
        // TODO: 알림 서비스와 연동
    }

    private suspend fun sendPaymentCancelledNotification(event: PaymentCancelledEvent) {
        log.info("📧 결제 취소 알림 발송: customerId={}, amount={}원", event.customerId, event.cancelAmount)
        // TODO: 알림 서비스와 연동
    }

    private suspend fun sendCancelFailureAlert(event: PaymentCancelFailedEvent) {
        log.error("🚨 결제 취소 실패 관리자 알림: paymentId={}", event.paymentId)
        // TODO: 관리자 알림 시스템과 연동
    }

    private suspend fun addToRetryQueue(event: PaymentCancelFailedEvent) {
        log.info("🔄 재시도 큐 추가: paymentId={}, retryCount={}", event.paymentId, event.retryCount)
        // TODO: 재시도 큐 시스템과 연동
    }

    // 메트릭 수집 메서드들
    private fun recordPaymentCreatedMetric(event: PaymentCreatedEvent) {
        log.debug("📊 결제 생성 메트릭 수집: method={}, amount={}", event.paymentMethod, event.amount)
    }

    private fun recordPaymentApprovedMetric(event: PaymentApprovedEvent) {
        log.debug("📊 결제 승인 메트릭 수집: method={}, amount={}", event.paymentMethod, event.amount)
    }

    private fun recordPaymentFailedMetric(event: PaymentFailedEvent) {
        log.debug("📊 결제 실패 메트릭 수집: method={}, reason={}", event.paymentMethod, event.failureReason)
    }

    private fun recordPaymentCancelledMetric(event: PaymentCancelledEvent) {
        log.debug("📊 결제 취소 메트릭 수집: amount={}, reason={}", event.cancelAmount, event.cancelReason)
    }
}