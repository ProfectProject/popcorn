package com.popcorn.payment.dto

import jakarta.validation.constraints.*
import java.util.*

/**
 * 결제 승인 요청 DTO
 */
data class PaymentConfirmRequest(
    @field:NotBlank(message = "결제키는 필수입니다")
    val paymentKey: String,

    @field:NotBlank(message = "주문ID는 필수입니다")
    val orderId: String,

    @field:Min(value = 1, message = "결제 금액은 1원 이상이어야 합니다")
    @field:Max(value = 100_000_000, message = "결제 금액은 1억원을 초과할 수 없습니다")
    val amount: Int
)

/**
 * 결제 취소 요청 DTO
 */
data class PaymentCancelRequest(
    @field:NotBlank(message = "취소 사유는 필수입니다")
    @field:Size(min = 1, max = 200, message = "취소 사유는 1-200자 내로 입력해주세요")
    val cancelReason: String,

    @field:Min(value = 1, message = "취소 금액은 1원 이상이어야 합니다")
    val cancelAmount: Int? = null
)

/**
 * 결제 생성 요청 DTO
 */
data class PaymentCreateRequest(
    @field:NotNull(message = "주문ID는 필수입니다")
    val orderId: UUID,

    @field:NotBlank(message = "결제수단은 필수입니다")
    @field:Pattern(
        regexp = "^(CARD|TRANSFER|VIRTUAL_ACCOUNT|MOBILE_PHONE|GIFT_CERTIFICATE)$",
        message = "유효하지 않은 결제수단입니다"
    )
    val paymentMethod: String,

    @field:Min(value = 1, message = "결제 금액은 1원 이상이어야 합니다")
    val amount: Int
)

/**
 * 결제 상태 업데이트 요청 DTO
 */
data class PaymentStatusUpdateRequest(
    @field:NotBlank(message = "결제 상태는 필수입니다")
    @field:Pattern(
        regexp = "^(READY|PAID|CANCELLED|FAILED)$",
        message = "유효하지 않은 결제 상태입니다"
    )
    val status: String,

    val reason: String? = null
)