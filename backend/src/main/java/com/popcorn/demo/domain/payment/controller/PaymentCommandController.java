package com.popcorn.demo.domain.payment.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.versioning.ApiVersion;
import com.popcorn.demo.domain.payment.dto.request.PaymentCreateRequest;
import com.popcorn.demo.domain.payment.dto.response.PaymentCreateResponse;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;
import com.popcorn.demo.domain.payment.exception.PaymentException;
import com.popcorn.demo.domain.payment.service.PaymentCommandService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Payments", description = "결제 기록 생성 API")
@RestController
@ApiVersion("v1")
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class PaymentCommandController extends BaseController {

	private final PaymentCommandService paymentCommandService;
	private final ObjectMapper objectMapper;

	@Operation(
			summary = "결제 기록 생성",
			description = """
				주문에 대한 결제 기록을 생성합니다.

				**주문 유형 판별:**
				- 주문 아이템에 세션 옵션이 포함되면 예약 주문(RESERVATION)
				- 주문 아이템에 굿즈가 포함되면 구매 주문(PURCHASE)

				**결제 수단 제한:**
				- 예약 주문: CARD, TRANSFER, EASY_PAY
				- 구매 주문: CARD

				**결제 상태:**
				- 생성 시 상태는 READY로 고정됩니다.
				"""
	)
	@ApiResponse(
			responseCode = "201",
			description = "결제 기록 생성 성공",
			content = @Content(schema = @Schema(implementation = PaymentCreateResponse.class))
	)
	@PostMapping("/{orderId}/payments")
	public ResponseEntity<BaseResponse<PaymentCreateResponse>> createPayment(
			@Parameter(description = "주문 ID", required = true, example = "00000000-0000-0000-0000-000000001003")
			@PathVariable UUID orderId,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "결제 생성 요청",
				required = true,
				content = @Content(
					schema = @Schema(implementation = PaymentCreateRequest.class),
					examples = {
						@ExampleObject(
							name = "예약 주문 - 카드 결제",
							value = """
								{
								  "method": "CARD",
								  "amount": 4000,
								  "rawPayload": {
								    "pg": "example",
								    "transactionId": "T-20250101"
								  }
								}
								"""
						),
						@ExampleObject(
							name = "예약 주문 - 간편결제",
							value = """
								{
								  "method": "EASY_PAY",
								  "amount": 4000
								}
								"""
						),
						@ExampleObject(
							name = "예약 주문 - 계좌이체",
							value = """
								{
								  "method": "TRANSFER",
								  "amount": 4000,
								  "rawPayload": {
								    "bank": "K-BANK",
								    "account": "123-456-7890"
								  }
								}
								"""
						),
						@ExampleObject(
							name = "구매 주문 - 카드 결제",
							value = """
								{
								  "method": "CARD",
								  "amount": 3000,
								  "rawPayload": {
								    "pg": "example",
								    "transactionId": "T-20250102"
								  }
								}
								"""
						)
					}
				)
			)
			@Valid @RequestBody PaymentCreateRequest request) {

		String rawPayload = toPayloadJson(request.getRawPayload());
		PaymentCommandService.PaymentCreationResult result =
				paymentCommandService.createPayment(
						orderId,
						request.getMethod(),
						request.getAmount(),
						rawPayload);

		PaymentCreateResponse response = PaymentCreateResponse.builder()
				.paymentId(result.getPaymentId())
				.status(toApiPaymentStatus(result.getPaymentStatus()))
				.orderStatus(result.getOrderStatus().name())
				.approvedAt(result.getApprovedAt())
				.build();

		return created(response);
	}

	private String toPayloadJson(Object payload) {
		if (payload == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException ex) {
			throw PaymentException.invalidRequest();
		}
	}

	private String toApiPaymentStatus(PaymentStatus status) {
		if (status == null) {
			throw PaymentException.invalidRequest();
		}
		return status.name();
	}
}
