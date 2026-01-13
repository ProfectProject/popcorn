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
import com.popcorn.demo.domain.payment.service.PaymentQueryService;
import com.popcorn.demo.domain.payment.service.PaymentQueryService.PaymentDetailResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Payments", description = "결제 관리 API")
@ApiVersion("v1")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController extends BaseController {

	private final PaymentCommandService paymentCommandService;
	private final PaymentQueryService paymentQueryService;

	@Operation(summary = "결제 조회(주문 기준)", description = "주문 기준으로 결제 기록을 조회합니다.")
	@ApiResponse(
			responseCode = "200",
			description = "결제 목록 조회 성공",
			content = @Content(schema = @Schema(implementation = PaymentListResponse.class))
	)
	@GetMapping("/orders/{orderId}/payments")
	public ResponseEntity<BaseResponse<PaymentListResponse>> getPaymentsByOrder(
			@Parameter(description = "주문 ID", required = true, example = "40000000-0000-0000-0000-000000000004")
			@PathVariable UUID orderId) {
		List<PaymentDetailResult> results = paymentQueryService.getPaymentsByOrder(orderId);
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
	@ApiResponse(
			responseCode = "200",
			description = "결제 단건 조회 성공",
			content = @Content(schema = @Schema(implementation = PaymentDetailResponse.class))
	)
	@GetMapping("/payments/{paymentId}")
	public ResponseEntity<BaseResponse<PaymentDetailResponse>> getPayment(
			@Parameter(description = "결제 ID", required = true, example = "70000000-0000-0000-0000-000000000001")
			@PathVariable UUID paymentId) {
		PaymentDetailResult result = paymentQueryService.getPayment(paymentId);
		return ok(toDetailResponse(result));
	}

	@Operation(summary = "결제 승인 처리", description = "결제 승인 확정 처리")
	@ApiResponse(
			responseCode = "200",
			description = "결제 승인 처리 성공",
			content = @Content(schema = @Schema(implementation = PaymentDetailResponse.class))
	)
	@PostMapping("/payments/{paymentId}/approve")
	public ResponseEntity<BaseResponse<PaymentDetailResponse>> approvePayment(
			@Parameter(description = "결제 ID", required = true, example = "70000000-0000-0000-0000-000000000003")
			@PathVariable UUID paymentId,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "결제 상태 변경 요청",
				required = true,
				content = @Content(
					schema = @Schema(implementation = PaymentStatusUpdateRequest.class),
					examples = {
						@ExampleObject(
							name = "결제 승인",
							value = """
								{
								  "status": "PAID"
								}
								"""
						),
						@ExampleObject(
							name = "결제 실패",
							value = """
								{
								  "status": "FAILED"
								}
								"""
						),
						@ExampleObject(
							name = "결제 취소",
							value = """
								{
								  "status": "CANCELLED"
								}
								"""
						)
					}
				)
			)
			@Valid @RequestBody PaymentStatusUpdateRequest request) {
		com.popcorn.demo.domain.payment.service.PaymentCommandService.PaymentDetailResult result =
				paymentCommandService.updatePaymentStatus(paymentId, request.getStatus());
		return ok(toCommandDetailResponse(result));
	}

	@Operation(summary = "결제 삭제(소프트 삭제)", description = "결제 기록을 소프트 삭제합니다.")
	@ApiResponse(responseCode = "204", description = "결제 삭제 성공")
	@DeleteMapping("/payments/{paymentId}")
	public ResponseEntity<Void> deletePayment(
			@Parameter(description = "결제 ID", required = true, example = "70000000-0000-0000-0000-000000000003")
			@PathVariable UUID paymentId) {
		paymentCommandService.deletePayment(paymentId);
		return noContent();
	}

	/**
	 * Query Service 결과를 응답 DTO로 변환 (QR 정보 포함)
	 */
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
				.qrAvailable(result.getQrAvailable())
				.qrCode(result.getQrCode())
				.qrExpiresAt(result.getQrExpiresAt())
				.build();
	}

	/**
	 * Command Service 결과를 응답 DTO로 변환 (QR 정보 없음)
	 */
	private PaymentDetailResponse toCommandDetailResponse(
			com.popcorn.demo.domain.payment.service.PaymentCommandService.PaymentDetailResult result) {
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
				.qrAvailable(false) // Command 작업에서는 QR 정보 없음
				.qrCode(null)
				.qrExpiresAt(null)
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
