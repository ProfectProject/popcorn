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
    val paymentId: UUID,
    val orderId: UUID,
    val amount: Int,
    val status: String,
    val paymentMethod: String,
    val createdAt: LocalDateTime
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