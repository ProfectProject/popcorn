package com.popcorn.payment.dto

import java.time.LocalDateTime
import java.util.*

/**
 * 결제 승인 응답 DTO
 */
data class PaymentConfirmResponse(
    val paymentId: UUID,
    val paymentStatus: String,
    val orderStatus: String,
    val orderId: UUID,
    val orderNo: String,
    val amount: Int,
    val approvedAt: LocalDateTime?
)

/**
 * 결제 취소 응답 DTO
 */
data class PaymentCancelResponse(
    val paymentId: UUID,
    val orderId: UUID,
    val cancelAmount: Int,
    val status: String,
    val cancelReason: String
)

/**
 * 결제 생성 응답 DTO
 */
data class PaymentCreateResponse(
    val paymentId: UUID?,
    val orderId: UUID,
    val amount: Int,
    val status: String,
    val paymentMethod: String,
    val createdAt: LocalDateTime,
    val paymentUrl: String? = null, // 결제 URL (카드결제, 간편결제 등에서 사용)
    val expiresAt: LocalDateTime? = null // 결제 만료 시간
)

/**
 * 결제 토큰 디코드 응답 DTO
 */
data class PaymentTokenDecodeResponse(
    val orderId: UUID,
    val orderNo: String,
    val amount: Int,
    val customerKey: String,
    val successUrl: String,
    val failUrl: String
)

/**
 * 결제 상세 조회 응답 DTO
 */
data class PaymentDetailResponse(
    val paymentId: UUID,
    val orderId: UUID,
    val amount: Int,
    val status: String,
    val paymentMethod: String,
    val createdAt: LocalDateTime,
    val approvedAt: LocalDateTime?,
    val updatedAt: LocalDateTime
)

/**
 * 결제 상태 조회 응답 DTO
 */
data class PaymentStatusResponse(
    val paymentId: UUID,
    val orderId: UUID,
    val status: String,
    val approvedAt: LocalDateTime?
)

/**
 * 결제 목록 조회 응답 DTO
 */
data class PaymentListResponse(
    val payments: List<PaymentDetailResponse>,
    val totalCount: Int,
    val totalAmount: Long
)

/**
 * API 공통 응답 DTO
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val errorCode: String? = null
) {
    companion object {
        fun <T> success(data: T, message: String = "요청이 성공적으로 처리되었습니다."): ApiResponse<T> {
            return ApiResponse(success = true, data = data, message = message)
        }

        fun <T> error(message: String, errorCode: String? = null): ApiResponse<T> {
            return ApiResponse(success = false, message = message, errorCode = errorCode)
        }
    }
}
