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

@Tag(name = "Payments", description = "토스 결제 승인 API")
@RestController
@ApiVersion("v1")
@RequestMapping("/api/v1/payments/toss")
@RequiredArgsConstructor
public class TossPaymentController extends BaseController {

	private final TossPaymentService tossPaymentService;

	@Operation(summary = "토스 결제 승인", description = "토스페이먼트 결제 승인(confirm)을 처리합니다.")
	@PostMapping("/confirm")
	public ResponseEntity<BaseResponse<TossPaymentConfirmResponse>> confirm(
			@Valid @RequestBody TossPaymentConfirmRequest request) {

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
	}
}
