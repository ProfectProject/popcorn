package com.popcorn.demo.domain.payment.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.versioning.ApiVersion;
import com.popcorn.demo.domain.payment.dto.response.PaymentDetailResponse;
import com.popcorn.demo.domain.payment.dto.response.PaymentListResponse;
import com.popcorn.demo.domain.payment.service.PaymentCommandService;
import com.popcorn.demo.domain.payment.service.PaymentCommandService.PaymentDetailResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Payments", description = "결제 조회/승인/실패/취소 API")
@ApiVersion("v1")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController extends BaseController {

	private final PaymentCommandService paymentCommandService;

	@Operation(summary = "결제 조회(주문 기준)", description = "주문 기준으로 결제 기록을 조회합니다.")
	@GetMapping("/orders/{orderId}/payments")
	public ResponseEntity<BaseResponse<PaymentListResponse>> getPaymentsByOrder(
			@Parameter(description = "주문 ID", required = true)
			@PathVariable UUID orderId) {
		List<PaymentDetailResult> results = paymentCommandService.getPaymentsByOrder(orderId);
		List<PaymentListResponse.Item> items = results.stream()
				.map(this::toListItem)
				.toList();
		PaymentListResponse response = PaymentListResponse.builder()
				.count(items.size())
				.items(items)
				.build();
		return ok(response);
	}

	@Operation(summary = "결제 단건 조회", description = "결제 ID로 결제 기록을 조회합니다.")
	@GetMapping("/payments/{paymentId}")
	public ResponseEntity<BaseResponse<PaymentDetailResponse>> getPayment(
			@Parameter(description = "결제 ID", required = true)
			@PathVariable UUID paymentId) {
		PaymentDetailResult result = paymentCommandService.getPayment(paymentId);
		return ok(toDetailResponse(result));
	}

	@Operation(summary = "결제 승인 처리", description = "결제 승인 확정 처리")
	@PostMapping("/payments/{paymentId}/approve")
	public ResponseEntity<BaseResponse<PaymentDetailResponse>> approvePayment(
			@Parameter(description = "결제 ID", required = true)
			@PathVariable UUID paymentId) {
		PaymentDetailResult result = paymentCommandService.approvePayment(paymentId);
		return ok(toDetailResponse(result));
	}

	@Operation(summary = "결제 실패 처리", description = "결제 실패 처리")
	@PostMapping("/payments/{paymentId}/fail")
	public ResponseEntity<BaseResponse<PaymentDetailResponse>> failPayment(
			@Parameter(description = "결제 ID", required = true)
			@PathVariable UUID paymentId) {
		PaymentDetailResult result = paymentCommandService.failPayment(paymentId);
		return ok(toDetailResponse(result));
	}

	@Operation(summary = "결제 취소/환불", description = "결제 취소/환불 처리")
	@PostMapping("/payments/{paymentId}/cancel")
	public ResponseEntity<BaseResponse<PaymentDetailResponse>> cancelPayment(
			@Parameter(description = "결제 ID", required = true)
			@PathVariable UUID paymentId) {
		PaymentDetailResult result = paymentCommandService.cancelPayment(paymentId);
		return ok(toDetailResponse(result));
	}

	@Operation(summary = "결제 삭제(소프트 삭제)", description = "결제 기록을 소프트 삭제합니다.")
	@DeleteMapping("/payments/{paymentId}")
	public ResponseEntity<Void> deletePayment(
			@Parameter(description = "결제 ID", required = true)
			@PathVariable UUID paymentId) {
		paymentCommandService.deletePayment(paymentId);
		return noContent();
	}

	private PaymentDetailResponse toDetailResponse(PaymentDetailResult result) {
		return PaymentDetailResponse.builder()
				.paymentId(result.getPaymentId())
				.orderId(result.getOrderId())
				.method(result.getMethod() != null ? result.getMethod().name() : null)
				.status(result.getPaymentStatus() != null ? result.getPaymentStatus().name() : null)
				.amount(result.getAmount())
				.approvedAt(result.getApprovedAt())
				.createdAt(result.getCreatedAt())
				.updatedAt(result.getUpdatedAt())
				.orderStatus(result.getOrderStatus() != null ? result.getOrderStatus().name() : null)
				.build();
	}

	private PaymentListResponse.Item toListItem(PaymentDetailResult result) {
		return PaymentListResponse.Item.builder()
				.paymentId(result.getPaymentId())
				.orderId(result.getOrderId())
				.method(result.getMethod() != null ? result.getMethod().name() : null)
				.status(result.getPaymentStatus() != null ? result.getPaymentStatus().name() : null)
				.amount(result.getAmount())
				.approvedAt(result.getApprovedAt())
				.createdAt(result.getCreatedAt())
				.build();
	}
}
