package com.popcorn.payment.dto

import java.time.LocalDateTime
import java.util.*

/**
 * 결제 컨텍스트 정보 - 보안 인증용
 */
data class PaymentContext(
    val orderId: UUID,
    val orderNo: String,
    val customerId: Long,
    val customerEmail: String?,
    val amount: Int,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val expiresAt: LocalDateTime = LocalDateTime.now().plusMinutes(30), // 30분 만료
    val ipAddress: String? = null,
    val userAgent: String? = null
)

/**
 * 결제 컨텍스트 생성 요청
 */
data class CreatePaymentContextRequest(
    val orderId: UUID,
    val orderNo: String,
    val customerId: Long,
    val customerEmail: String?,
    val amount: Int,
    val ipAddress: String? = null,
    val userAgent: String? = null
)

/**
 * 결제 컨텍스트 검증 결과
 */
data class PaymentContextValidation(
    val isValid: Boolean,
    val context: PaymentContext? = null,
    val errorMessage: String? = null
) {
    companion object {
        fun valid(context: PaymentContext) = PaymentContextValidation(true, context, null)
        fun invalid(message: String) = PaymentContextValidation(false, null, message)
    }
}