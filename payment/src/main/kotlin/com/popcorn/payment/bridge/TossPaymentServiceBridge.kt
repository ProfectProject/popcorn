package com.popcorn.payment.bridge

import com.popcorn.payment.service.TossPaymentConfirmResult
import com.popcorn.payment.service.TossPaymentCancelResult
import com.popcorn.payment.service.TossPaymentCoroutineService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*


@Service
class TossPaymentServiceBridge(
    private val tossPaymentCoroutineService: TossPaymentCoroutineService
) {

    private val log = LoggerFactory.getLogger(TossPaymentServiceBridge::class.java)

    /**
     * 기존 Java 인터페이스와 호환되는 결제 승인 메서드
     *
     * 💡 사용 예시:
     * ```java
     * // 기존 Java 코드에서 그대로 사용 가능
     * @Autowired
     * private TossPaymentServiceBridge paymentService;
     *
     * public ResponseEntity confirmPayment(String paymentKey, String orderId, Integer amount) {
     *     TossPaymentConfirmResultBridge result = paymentService.confirmPayment(paymentKey, orderId, amount);
     *     return ResponseEntity.ok(result);
     * }
     * ```
     *
     * @param paymentKey 토스 결제 키
     * @param orderId 주문 ID
     * @param amount 결제 금액
     * @return 결제 승인 결과 (Java 호환 형태)
     */
    fun confirmPayment(
        paymentKey: String,
        orderId: String,
        amount: Int
    ): TossPaymentConfirmResultBridge {

        log.info("🌉 Bridge - 토스 결제 승인 요청: orderId={}, paymentKey={}", orderId, paymentKey)

        return try {
            // 코루틴을 동기식으로 실행 (Bridge 역할)
            val result = runBlocking {
                tossPaymentCoroutineService.confirmPayment(paymentKey, orderId, amount)
            }

            // 코루틴 결과를 Java 호환 형태로 변환
            TossPaymentConfirmResultBridge(
                paymentId = result.paymentId,
                paymentStatus = result.paymentStatus,
                orderStatus = result.orderStatus,
                orderId = result.orderId,
                orderNo = result.orderNo,
                amount = result.amount,
                approvedAt = result.approvedAt
            ).also {
                log.info("✅ Bridge - 토스 결제 승인 완료: paymentId={}", it.paymentId)
            }

        } catch (e: Exception) {
            log.error("❌ Bridge - 토스 결제 승인 실패: orderId={}, error={}", orderId, e.message, e)
            throw e  // 예외는 그대로 전파
        }
    }

    /**
     * 기존 Java 인터페이스와 호환되는 결제 취소 메서드
     *
     * @param orderId 주문 ID
     * @param cancelReason 취소 사유
     * @return 결제 취소 결과 (Java 호환 형태)
     */
    fun cancelPayment(
        orderId: UUID,
        cancelReason: String
    ): TossPaymentCancelResultBridge {

        log.info("🌉 Bridge - 토스 결제 취소 요청: orderId={}", orderId)

        return try {
            // 코루틴을 동기식으로 실행 (Bridge 역할)
            val result = runBlocking {
                tossPaymentCoroutineService.cancelPayment(orderId, cancelReason)
            }

            // 코루틴 결과를 Java 호환 형태로 변환
            TossPaymentCancelResultBridge(
                paymentId = result.paymentId,
                orderId = result.orderId,
                cancelAmount = result.cancelAmount,
                status = result.status,
                cancelReason = result.cancelReason
            ).also {
                log.info("✅ Bridge - 토스 결제 취소 완료: paymentId={}", it.paymentId)
            }

        } catch (e: Exception) {
            log.error("❌ Bridge - 토스 결제 취소 실패: orderId={}, error={}", orderId, e.message, e)
            throw e  // 예외는 그대로 전파
        }
    }

    /**
     * 헬스체크용 메서드 - 서비스 상태 확인
     *
     * @return 서비스 정상 여부
     */
    fun isHealthy(): Boolean {
        return try {
            // 간단한 헬스체크 로직
            true
        } catch (e: Exception) {
            log.error("❌ Bridge - 헬스체크 실패: error={}", e.message, e)
            false
        }
    }
}

/**
 * Java 호환성을 위한 결제 승인 결과 DTO
 * Kotlin data class를 Java에서 쉽게 사용할 수 있도록 변환
 */
data class TossPaymentConfirmResultBridge(
    val paymentId: UUID,
    val paymentStatus: String,
    val orderStatus: String,
    val orderId: UUID,
    val orderNo: String,
    val amount: Int,
    val approvedAt: LocalDateTime
)

/**
 * Java 호환성을 위한 결제 취소 결과 DTO
 */
data class TossPaymentCancelResultBridge(
    val paymentId: UUID,
    val orderId: UUID,
    val cancelAmount: Int,
    val status: String,
    val cancelReason: String
)

/**
 * 마이그레이션 진행 상황 모니터링을 위한 유틸리티
 */
object BridgeUsageTracker {

    private var bridgeCallCount = 0L
    private var lastAccessTime = System.currentTimeMillis()

    /**
     * Bridge 사용 횟수 증가
     */
    fun incrementUsage() {
        bridgeCallCount++
        lastAccessTime = System.currentTimeMillis()
    }

    /**
     * Bridge 사용 통계 조회
     */
    fun getUsageStats(): BridgeUsageStats {
        return BridgeUsageStats(
            totalCalls = bridgeCallCount,
            lastAccessTime = lastAccessTime
        )
    }

    /**
     * 통계 초기화
     */
    fun reset() {
        bridgeCallCount = 0
        lastAccessTime = System.currentTimeMillis()
    }
}

/**
 * Bridge 사용 통계 정보
 */
data class BridgeUsageStats(
    val totalCalls: Long,
    val lastAccessTime: Long
)