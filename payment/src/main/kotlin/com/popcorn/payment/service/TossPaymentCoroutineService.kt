package com.popcorn.payment.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.popcorn.payment.client.TossPaymentsCoroutineClient
import com.popcorn.payment.config.CoroutineTransactionManager
import com.popcorn.payment.dto.TossPaymentCancelRequest
import com.popcorn.payment.dto.TossPaymentConfirmRequest
import com.popcorn.payment.event.PaymentEventPublisherImpl
import com.popcorn.payment.exception.PaymentException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*


/**
 * 토스페이먼츠 결제 처리 서비스
 */
@Service
class TossPaymentCoroutineService(
    private val tossClient: TossPaymentsCoroutineClient,
    private val transactionManager: CoroutineTransactionManager,
    private val paymentCommandService: PaymentCommandCoroutineService,
    private val orderQueryService: OrderQueryCoroutineService,
    private val objectMapper: ObjectMapper,
    private val paymentEventPublisher: PaymentEventPublisherImpl
) {

    private val log = LoggerFactory.getLogger(TossPaymentCoroutineService::class.java)

    /**
     * 토스페이먼츠 결제 생성 (결제 URL 발급)
     */
    suspend fun createPaymentRequest(
        orderId: String,
        amount: Int,
        orderName: String,
        customerKey: String
    ): PaymentCreateResult {
        log.info("토스페이먼츠 결제 생성 요청: orderId={}, amount={}, orderName={}", orderId, amount, orderName)

        return try {
            // 토스페이먼츠 결제 위젯 URL 생성
            // 실제로는 토스페이먼츠 결제 생성 API를 호출해야 함
            val paymentUrl = generateTossPaymentWidgetUrl(orderId, amount, orderName, customerKey)

            PaymentCreateResult(
                paymentUrl = paymentUrl,
                orderId = orderId,
                amount = amount,
                expiresAt = java.time.LocalDateTime.now().plusMinutes(30)
            )
        } catch (e: Exception) {
            log.error("토스페이먼츠 결제 생성 실패: orderId={}, error={}", orderId, e.message, e)
            throw e
        }
    }

    /**
     * 토스페이먼츠 결제 위젯 URL 생성
     */
    private fun generateTossPaymentWidgetUrl(
        orderId: String,
        amount: Int,
        orderName: String,
        customerKey: String
    ): String {
        // 토스페이먼츠 결제 위젯 연동 방식
        // 실제로는 토스페이먼츠 SDK 또는 API를 통해 결제 URL을 받아야 함

        // 현재는 토스페이먼츠 결제 위젯 URL 형식으로 생성
        val baseUrl = "https://js.tosspayments.com/v1/payment"
        val params = "orderId=$orderId&amount=$amount&orderName=${java.net.URLEncoder.encode(orderName, "UTF-8")}&customerKey=$customerKey"

        return "$baseUrl?$params"
    }

    /**
     * 토스 결제 승인 처리
     */
    suspend fun confirmPayment(
        paymentKey: String,
        orderId: String,
        amount: Int
    ): TossPaymentConfirmResult = coroutineScope {

            log.info("토스 결제 승인 요청 시작: orderId={}, paymentKey={}, amount={}", orderId, paymentKey, amount)


            try {
                val idempotencyCheckDeferred = async {
                    checkIdempotency(paymentKey)
                }
                val orderDeferred = async {
                    orderQueryService.getOrder(UUID.fromString(orderId))
                }

                val existingPayment = idempotencyCheckDeferred.await()
                if (existingPayment != null) {
                    log.info("이미 처리된 결제: paymentId={}", existingPayment.paymentId)
                    return@coroutineScope existingPayment
                }

                var order = orderDeferred.await()
                if (order.status == "REQUESTED") {
                    log.info("결제 승인 전 주문 상태를 PAYMENT_PENDING으로 전환: orderId={}", order.id)
                    order = orderQueryService.updateOrderStatus(
                        orderId = order.id,
                        status = "PAYMENT_PENDING",
                        reason = "결제 승인 준비"
                    )
                }

                log.info("토스 결제 승인 API 호출: paymentKey={}", paymentKey)
                val tossResponse = tossClient.confirm(
                    TossPaymentConfirmRequest(
                        paymentKey = paymentKey,
                        orderId = orderId,
                        amount = amount
                    )
                )

                validateAmount(tossResponse.totalAmount, amount)
                val approvedAt = parseApprovedAt(tossResponse.approvedAt)
                val rawPayload = serializeResponse(tossResponse)

                log.info("결제 기록 생성 및 주문 상태 업데이트")

                val paymentResult = transactionManager.executeInTransactionSuspend {
                    val createdPayment = paymentCommandService.createPaymentBlocking(
                        orderId = order.id,
                        paymentMethod = "CARD",
                        amount = amount,
                        paymentKey = paymentKey,
                        rawPayload = rawPayload
                    )

                    paymentCommandService.updatePaymentStatusBlocking(
                        paymentId = createdPayment.paymentId,
                        status = "PAID",
                        approvedAt = approvedAt,
                        rawPayload = rawPayload
                    )

                    createdPayment
                }

                val updatedOrder = orderQueryService.updateOrderStatus(
                    orderId = order.id,
                    status = "PAID",
                    reason = "결제 승인"
                )

                val result = TossPaymentConfirmResult(
                    paymentId = paymentResult.paymentId,
                    paymentStatus = "PAID",
                    orderStatus = updatedOrder.status,
                    orderId = order.id,
                    orderNo = order.orderNo,
                    amount = amount,
                    approvedAt = approvedAt
                )

                try {
                    paymentEventPublisher.publishPaymentApproved(
                        paymentId = paymentResult.paymentId,
                        orderId = order.id,
                        orderNo = order.orderNo,
                        amount = amount,
                        paymentMethod = "CARD",
                        paymentKey = paymentKey,
                        approvedAt = approvedAt,
                        customerId = order.customerId
                    )
                    log.info("결제 성공 이벤트 발행 완료")
                } catch (e: Exception) {
                    log.error("결제 성공 이벤트 발행 실패 - 결제는 성공 처리: error={}", e.message, e)
                }

                log.info("토스 결제 승인 완료: paymentId={}, orderNo={}, amount={}원",
                    result.paymentId, result.orderNo, result.amount)


                result

            } catch (e: Exception) {
                log.error("결제 승인 실패: paymentKey={}, orderId={}, error={}", paymentKey, orderId, e.message, e)
                throw e
            }
    }

    /**
     * 토스 결제 취소 처리 (코루틴 버전)
     *
     * @param orderId 주문 ID
     * @param cancelReason 취소 사유
     * @return 결제 취소 결과
     */
    suspend fun cancelPayment(
        orderId: UUID,
        cancelReason: String
    ): TossPaymentCancelResult {

        log.info("🔄 토스 결제 취소 요청: orderId={}, cancelReason={}", orderId, cancelReason)

        // Step 1: 주문 및 결제 정보 조회
        val order = orderQueryService.getOrder(orderId)
        log.debug("주문 확인 완료: orderId={}, orderNo={}, status={}", order.id, order.orderNo, order.status)
        val payment = paymentCommandService.getLatestPaymentByOrderId(orderId)

        if (payment.status != "PAID") {
            throw PaymentException.invalidStatusTransition("결제 완료 상태가 아닙니다: ${payment.status}")
        }

        val paymentKey = payment.paymentKey
            ?: extractPaymentKeyFromRawPayload(payment.rawPayload)
            ?: throw PaymentException.invalidRequest("결제 키를 찾을 수 없습니다")

        // Step 2: 토스 결제 취소 API 호출
        val cancelResponse = tossClient.cancel(
            paymentKey = paymentKey,
            request = TossPaymentCancelRequest(
                cancelReason = cancelReason
            )
        )

        // Step 3: 결제 상태 업데이트
        paymentCommandService.updatePaymentStatus(
            paymentId = payment.paymentId,
            status = "CANCELLED",
            rawPayload = serializeResponse(cancelResponse)
        )

        val result = TossPaymentCancelResult(
            paymentId = payment.paymentId,
            orderId = orderId,
            cancelAmount = cancelResponse.totalAmount,
            status = cancelResponse.status,
            cancelReason = cancelReason
        )

        log.info("✅ 토스 결제 취소 완료: paymentId={}, cancelAmount={}원",
            result.paymentId, result.cancelAmount)

        return result
    }

    /**
     * 멱등성 체크 - paymentKey 기반 중복 결제 확인
     *
     * @param paymentKey 토스 결제 키
     * @param orderId 주문 ID
     * @return 기존 결제 정보 또는 null
     */
    private suspend fun checkIdempotency(
        paymentKey: String
    ): TossPaymentConfirmResult? {

        // PaymentKey로 기존 결제 조회
        val existingPayments = paymentCommandService.findByPaymentKey(paymentKey)

        return if (existingPayments.isNotEmpty()) {
            val existingPayment = existingPayments.first()
            val order = orderQueryService.getOrder(existingPayment.orderId ?: return null)

            TossPaymentConfirmResult(
                paymentId = existingPayment.paymentId,
                paymentStatus = existingPayment.status,
                orderStatus = order.status,
                orderId = order.id,
                orderNo = order.orderNo,
                amount = existingPayment.amount,
                approvedAt = existingPayment.approvedAt ?: LocalDateTime.now()
            )
        } else {
            null
        }
    }

    private fun validateAmount(tossAmount: Int, requestAmount: Int) {
        if (tossAmount != requestAmount) {
            throw PaymentException.amountMismatch("결제 금액 불일치: 요청=$requestAmount, 토스=$tossAmount")
        }
    }

    private fun parseApprovedAt(approvedAt: String?): LocalDateTime {
        return if (approvedAt.isNullOrBlank()) {
            LocalDateTime.now()
        } else {
            try {
                OffsetDateTime.parse(approvedAt).toLocalDateTime()
            } catch (e: Exception) {
                log.warn("승인 시간 파싱 실패, 현재 시간 사용: approvedAt={}", approvedAt)
                LocalDateTime.now()
            }
        }
    }

    private fun serializeResponse(response: Any): String {
        return try {
            objectMapper.writeValueAsString(response)
        } catch (e: Exception) {
            log.error("응답 직렬화 실패", e)
            throw PaymentException.invalidRequest("응답 직렬화 실패")
        }
    }

    private fun extractPaymentKeyFromRawPayload(rawPayload: String?): String? {
        if (rawPayload.isNullOrBlank()) return null

        return try {
            val node = objectMapper.readTree(rawPayload)
            node["paymentKey"]?.asText()
        } catch (e: Exception) {
            log.warn("결제 키 추출 실패: rawPayload={}", rawPayload, e)
            null
        }
    }

}

/**
 * 토스 결제 승인 결과 DTO
 */
data class TossPaymentConfirmResult(
    val paymentId: UUID,
    val paymentStatus: String,
    val orderStatus: String,
    val orderId: UUID,
    val orderNo: String,
    val amount: Int,
    val approvedAt: LocalDateTime
)

/**
 * 토스 결제 취소 결과 DTO
 */
data class TossPaymentCancelResult(
    val paymentId: UUID,
    val orderId: UUID,
    val cancelAmount: Int,
    val status: String,
    val cancelReason: String
)

/**
 * 토스 결제 생성 결과 DTO
 */
data class PaymentCreateResult(
    val paymentUrl: String,
    val orderId: String,
    val amount: Int,
    val expiresAt: java.time.LocalDateTime
)
