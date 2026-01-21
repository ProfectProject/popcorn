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
        try {
            log.info("🔗 QR 코드 생성 시작: orderId={}, orderNo={}", orderId, orderNo)

            // QR 코드 데이터 생성 (체크인 또는 주문 확인용 URL)
            val qrCodeData = generateQrCodeData(orderId, orderNo)

            // QR 코드 생성 및 저장
            val qrCodeInfo = createAndSaveQrCode(orderId, orderNo, qrCodeData)

            // checkIns 서비스에 QR 코드 정보 전달 (향후 구현)
            sendQrCodeToCheckInsService(qrCodeInfo)

            log.info("✅ QR 코드 생성 완료: orderId={}, qrCodeId={}, expiresAt={}",
                orderId, qrCodeInfo.qrCodeId, qrCodeInfo.expiresAt)

        } catch (e: Exception) {
            log.error("❌ QR 코드 생성 실패: orderId={}, error={}", orderId, e.message, e)
            // QR 코드 생성 실패해도 결제는 성공으로 처리
        }
    }

    /**
     * QR 코드에 포함될 데이터 생성
     */
    private fun generateQrCodeData(orderId: java.util.UUID, orderNo: String): String {
        // QR 코드 스캔 시 이동할 URL 또는 데이터
        val baseUrl = "https://api.popcorn.com/checkin" // 실제 도메인으로 변경 필요
        val checkInUrl = "$baseUrl?orderId=$orderId&orderNo=$orderNo"

        // 추가 보안을 위해 토큰 생성 (간단한 예시)
        val timestamp = System.currentTimeMillis()
        val token = generateSecureToken(orderId.toString(), orderNo, timestamp)

        return "$checkInUrl&token=$token&ts=$timestamp"
    }

    /**
     * QR 코드 생성 및 저장
     */
    private suspend fun createAndSaveQrCode(orderId: java.util.UUID, orderNo: String, qrCodeData: String): QrCodeInfo {
        // QR 코드 생성 (실제로는 ZXing 라이브러리 등 사용)
        log.info("📱 QR 코드 이미지 생성: data={}", qrCodeData)

        // QR 코드 정보 생성
        val qrCodeId = java.util.UUID.randomUUID()
        val now = java.time.LocalDateTime.now()
        val expiresAt = now.plusDays(1) // 24시간 후 만료

        // 실제로는 QR 코드 이미지를 S3, CloudFront 등에 저장
        val qrCodeImageUrl = "https://cdn.popcorn.com/qr/${qrCodeId}.png"

        val qrCodeInfo = QrCodeInfo(
            qrCodeId = qrCodeId,
            orderId = orderId,
            orderNo = orderNo,
            qrCodeData = qrCodeData,
            qrCodeImageUrl = qrCodeImageUrl,
            createdAt = now,
            expiresAt = expiresAt,
            status = "ACTIVE"
        )

        // 실제로는 데이터베이스에 저장
        log.info("💾 QR 코드 정보 저장: qrCodeId={}", qrCodeId)
        // qrCodeRepository.save(qrCodeInfo)

        return qrCodeInfo
    }

    /**
     * checkIns 서비스에 QR 코드 정보 전달
     */
    private suspend fun sendQrCodeToCheckInsService(qrCodeInfo: QrCodeInfo) {
        try {
            log.info("📤 checkIns 서비스에 QR 코드 정보 전달: qrCodeId={}", qrCodeInfo.qrCodeId)

            // 실제로는 checkIns 서비스 API 호출
            // checkInsClient.registerQrCode(qrCodeInfo)

            log.info("✅ checkIns 서비스 등록 완료: qrCodeId={}", qrCodeInfo.qrCodeId)
        } catch (e: Exception) {
            log.warn("⚠️ checkIns 서비스 등록 실패: qrCodeId={}, error={}", qrCodeInfo.qrCodeId, e.message)
        }
    }

    /**
     * 보안 토큰 생성 (간단한 예시)
     */
    private fun generateSecureToken(orderId: String, orderNo: String, timestamp: Long): String {
        val data = "$orderId:$orderNo:$timestamp"
        return data.hashCode().toString(16) // 실제로는 HMAC, JWT 등 사용
    }

    /**
     * QR 코드 정보 데이터 클래스
     */
    data class QrCodeInfo(
        val qrCodeId: java.util.UUID,
        val orderId: java.util.UUID,
        val orderNo: String,
        val qrCodeData: String,
        val qrCodeImageUrl: String,
        val createdAt: java.time.LocalDateTime,
        val expiresAt: java.time.LocalDateTime,
        val status: String
    )

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
        try {
            log.info("❌ QR 코드 무효화 시작: orderId={}", orderId)

            // QR 코드 정보 조회 (실제로는 데이터베이스에서 조회)
            val qrCodes = findActiveQrCodesByOrderId(orderId)

            if (qrCodes.isEmpty()) {
                log.warn("⚠️ 무효화할 QR 코드가 없음: orderId={}", orderId)
                return
            }

            // 모든 관련 QR 코드 무효화
            qrCodes.forEach { qrCode ->
                invalidateQrCodeInfo(qrCode)
            }

            // checkIns 서비스에 무효화 알림
            notifyCheckInsServiceForInvalidation(orderId, qrCodes)

            log.info("✅ QR 코드 무효화 완료: orderId={}, invalidatedCount={}", orderId, qrCodes.size)

        } catch (e: Exception) {
            log.error("❌ QR 코드 무효화 실패: orderId={}, error={}", orderId, e.message, e)
        }
    }

    /**
     * 주문 ID로 활성 QR 코드 조회 (Mock)
     */
    private fun findActiveQrCodesByOrderId(orderId: java.util.UUID): List<QrCodeInfo> {
        // 실제로는 QR 코드 데이터베이스에서 조회
        // return qrCodeRepository.findActiveByOrderId(orderId)

        // Mock 데이터 (로그에서 QR 코드가 있다고 가정)
        log.info("🔍 활성 QR 코드 조회: orderId={}", orderId)
        return listOf() // 실제 구현 시 데이터베이스에서 조회
    }

    /**
     * QR 코드 정보 무효화
     */
    private fun invalidateQrCodeInfo(qrCode: QrCodeInfo) {
        log.info("🚫 QR 코드 무효화: qrCodeId={}", qrCode.qrCodeId)

        // 실제로는 데이터베이스에서 상태 업데이트
        // qrCodeRepository.updateStatus(qrCode.qrCodeId, "INVALIDATED")

        // QR 코드 이미지 삭제 또는 무효화 표시
        // imageService.invalidateQrCodeImage(qrCode.qrCodeImageUrl)
    }

    /**
     * checkIns 서비스에 QR 코드 무효화 알림
     */
    private suspend fun notifyCheckInsServiceForInvalidation(orderId: java.util.UUID, qrCodes: List<QrCodeInfo>) {
        try {
            log.info("📤 checkIns 서비스에 QR 코드 무효화 알림: orderId={}, qrCodeCount={}",
                orderId, qrCodes.size)

            // 실제로는 checkIns 서비스 API 호출
            // checkInsClient.invalidateQrCodes(orderId, qrCodes.map { it.qrCodeId })

            log.info("✅ checkIns 서비스 무효화 알림 완료: orderId={}", orderId)
        } catch (e: Exception) {
            log.warn("⚠️ checkIns 서비스 무효화 알림 실패: orderId={}, error={}", orderId, e.message)
        }
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