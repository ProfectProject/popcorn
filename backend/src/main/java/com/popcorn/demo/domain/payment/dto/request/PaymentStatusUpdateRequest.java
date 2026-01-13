package com.popcorn.demo.domain.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "결제 상태 변경 요청")
public class PaymentStatusUpdateRequest {

    @NotNull(message = "결제 상태는 필수입니다")
    @Schema(description = "결제 상태", example = "PAID", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentStatus status;

}