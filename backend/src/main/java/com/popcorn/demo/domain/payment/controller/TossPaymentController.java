package com.popcorn.demo.domain.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.versioning.ApiVersion;
import com.popcorn.demo.domain.payment.dto.request.TossPaymentConfirmRequest;
import com.popcorn.demo.domain.payment.dto.response.TossPaymentConfirmResponse;
import com.popcorn.demo.domain.payment.service.TossPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Payments", description = "결제 관리 API")
@RestController
@ApiVersion("v1")
@RequestMapping("/api/v1/payments/toss")
@RequiredArgsConstructor
public class TossPaymentController extends BaseController {

	private final TossPaymentService tossPaymentService;

	@Operation(
		summary = "토스 결제 승인",
		description = """
			토스페이먼트 결제 승인(confirm)을 처리합니다.

			🛡️ 멱등성 보장:
			- 동일한 주문에 대한 중복 결제 완전 차단
			- 이미 결제된 주문은 기존 결제 정보 반환
			- 결제 중인 요청에 대해서는 적절한 에러 처리
			"""
	)
	@PostMapping("/confirm")
	public ResponseEntity<BaseResponse<TossPaymentConfirmResponse>> confirm(
			@Valid @RequestBody TossPaymentConfirmRequest request) {

		// 🛡️ 멱등성 체크: 이미 결제된 주문인지 확인
		try {
			TossPaymentService.TossPaymentConfirmResult result =
					tossPaymentService.confirmPayment(request.getPaymentKey(), request.getOrderId(), request.getAmount());

			TossPaymentConfirmResponse response = TossPaymentConfirmResponse.builder()
					.paymentId(result.getPaymentId())
					.status(result.getPaymentStatus().name())
					.orderStatus(result.getOrderStatus().name())
					.orderId(result.getOrderId())
					.orderNo(result.getOrderNo())
					.amount(result.getAmount())
					.approvedAt(result.getApprovedAt())
					.build();

			return ok(response);

		} catch (Exception e) {
			// 🔍 중복 결제 시도인지 확인
			if (e.getMessage() != null && e.getMessage().contains("이미") && e.getMessage().contains("결제")) {
				throw e; // 멱등성 에러는 그대로 전달
			}
			throw e; // 기타 에러도 그대로 전달
		}
	}
}
