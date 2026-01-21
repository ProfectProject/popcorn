package com.popcorn.payment.service

import com.popcorn.payment.dto.PaymentConfirmRequest
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class PaymentApprovalAsyncService(
    private val tossPaymentService: TossPaymentCoroutineService
) {
    private val log = LoggerFactory.getLogger(PaymentApprovalAsyncService::class.java)

    @Async
    fun confirmAsync(request: PaymentConfirmRequest) {
        try {
            runBlocking {
                tossPaymentService.confirmPayment(
                    paymentKey = request.paymentKey,
                    orderId = request.orderId,
                    amount = request.amount
                )
            }
        } catch (e: Exception) {
            log.error("❌ 결제 승인(비동기) 실패: paymentKey={}, orderId={}, error={}",
                request.paymentKey, request.orderId, e.message, e)
        }
    }
}
