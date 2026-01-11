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
import com.popcorn.demo.domain.payment.dto.response.OrderPaymentCreateResponse;
import com.popcorn.demo.domain.payment.dto.response.ReservationPaymentCreateResponse;
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
			summary = "예약 결제 기록 생성",
			description = "예약 주문에 대한 결제 기록을 생성합니다."
	)
	@ApiResponse(
			responseCode = "201",
			description = "예약 결제 기록 생성 성공",
			content = @Content(schema = @Schema(implementation = ReservationPaymentCreateResponse.class))
	)
	@PostMapping("/{orderId}/reservation-payments")
	public ResponseEntity<BaseResponse<ReservationPaymentCreateResponse>> createReservationPayment(
			@Parameter(description = "주문 ID", required = true, example = "00000000-0000-0000-0000-000000001003")
			@PathVariable UUID orderId,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "예약 결제 생성 요청",
				required = true,
				content = @Content(
					schema = @Schema(implementation = PaymentCreateRequest.class),
					examples = {
						@ExampleObject(
							name = "CARD 결제",
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
							name = "EASY_PAY 결제",
							value = """
								{
								  "method": "EASY_PAY",
								  "amount": 4000
								}
								"""
						),
						@ExampleObject(
							name = "TRANSFER 결제",
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
						)
					}
				)
			)
			@Valid @RequestBody PaymentCreateRequest request) {

		String rawPayload = toPayloadJson(request.getRawPayload());
		PaymentCommandService.PaymentCreationResult result =
				paymentCommandService.createReservationPayment(
						orderId,
						request.getMethod(),
						request.getAmount(),
						rawPayload);

		ReservationPaymentCreateResponse response = ReservationPaymentCreateResponse.builder()
				.paymentId(result.getPaymentId())
				.paymentStatus(toApiPaymentStatus(result.getPaymentStatus()))
				.orderStatus(result.getOrderStatus().name())
				.approvedAt(result.getApprovedAt())
				.build();

		return created(response);
	}

	@Operation(
			summary = "주문 결제 기록 생성",
			description = "구매형 주문에 대한 결제 기록을 생성합니다."
	)
	@ApiResponse(
			responseCode = "201",
			description = "주문 결제 기록 생성 성공",
			content = @Content(schema = @Schema(implementation = OrderPaymentCreateResponse.class))
	)
	@PostMapping("/{orderId}/payments")
	public ResponseEntity<BaseResponse<OrderPaymentCreateResponse>> createOrderPayment(
			@Parameter(description = "주문 ID", required = true, example = "00000000-0000-0000-0000-000000001004")
			@PathVariable UUID orderId,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "구매 결제 생성 요청",
				required = true,
				content = @Content(
					schema = @Schema(implementation = PaymentCreateRequest.class),
					examples = @ExampleObject(
						name = "CARD 결제",
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
				)
			)
			@Valid @RequestBody PaymentCreateRequest request) {

		String rawPayload = toPayloadJson(request.getRawPayload());
		PaymentCommandService.PaymentCreationResult result =
				paymentCommandService.createOrderPayment(
						orderId,
						request.getMethod(),
						request.getAmount(),
						rawPayload);

		OrderPaymentCreateResponse response = OrderPaymentCreateResponse.builder()
				.paymentId(result.getPaymentId())
				.status(toApiPaymentStatus(result.getPaymentStatus()))
				.orderStatus(result.getOrderStatus().name())
				.build();

		return created(response);
	}

	@Operation(
			summary = "결제 생성(READY)",
			description = "결제 대기(READY) 상태의 결제 기록을 생성합니다."
	)
	@ApiResponse(
			responseCode = "201",
			description = "결제 기록 생성 성공",
			content = @Content(schema = @Schema(implementation = OrderPaymentCreateResponse.class))
	)
	@PostMapping("/{orderId}/payments/ready")
	public ResponseEntity<BaseResponse<OrderPaymentCreateResponse>> createReadyPayment(
			@Parameter(description = "주문 ID", required = true, example = "00000000-0000-0000-0000-000000001004")
			@PathVariable UUID orderId,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "결제 생성 요청",
				required = true,
				content = @Content(
					schema = @Schema(implementation = PaymentCreateRequest.class),
					examples = @ExampleObject(
						name = "READY 생성",
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
				)
			)
			@Valid @RequestBody PaymentCreateRequest request) {

		String rawPayload = toPayloadJson(request.getRawPayload());
		PaymentCommandService.PaymentCreationResult result =
				paymentCommandService.createReadyPayment(
						orderId,
						request.getMethod(),
						request.getAmount(),
						rawPayload);

		OrderPaymentCreateResponse response = OrderPaymentCreateResponse.builder()
				.paymentId(result.getPaymentId())
				.status(toApiPaymentStatus(result.getPaymentStatus()))
				.orderStatus(result.getOrderStatus().name())
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
