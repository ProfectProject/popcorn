package com.popcorn.demo.domain.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.common.controller.BaseController;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.common.versioning.ApiVersion;
import com.popcorn.demo.domain.payment.dto.request.TossPaymentConfirmRequest;
import com.popcorn.demo.domain.payment.dto.response.TossPaymentConfirmResponse;
import com.popcorn.demo.domain.payment.service.TossPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.popcorn.common.annotation.ApiLogging;
import com.popcorn.common.annotation.AuditLog;
import com.popcorn.common.annotation.Idempotent;
import com.popcorn.common.annotation.RateLimit;
import com.popcorn.common.annotation.RetryOnFailure;
import com.popcorn.common.annotation.ValidateRequest;

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

			⚠️ 보안 주의사항:
			- 분당 3회 결제 승인 제한
			- 결제 키 검증 및 민감정보 마스킹
			- 전체 결제 과정 감사 로그 기록
			"""
	)
	@ApiResponse(
		responseCode = "200",
		description = "결제 승인 성공",
		content = @Content(
			schema = @Schema(implementation = TossPaymentConfirmResponse.class),
			examples = @ExampleObject(
				name = "결제 승인 성공",
				value = """
					{
					  "success": true,
					  "data": {
					    "paymentId": "70000000-0000-0000-0000-000000000001",
					    "status": "PAID",
					    "orderStatus": "PAID",
					    "orderId": "40000000-0000-0000-0000-000000000004",
					    "orderNo": "ORD202601150001",
					    "amount": 25000,
					    "approvedAt": "2026-01-15T10:30:00"
					  }
					}
					"""
			)
		)
	)
	@PostMapping("/confirm")
	@RateLimit(
		requests = 3,
		window = 60,
		keyExpression = "T(org.springframework.web.context.request.RequestContextHolder).currentRequestAttributes().getRequest().getRemoteAddr()",
		errorMessage = "🚨 결제 승인은 분당 3회만 허용됩니다.",
		algorithm = RateLimit.Algorithm.SLIDING_WINDOW
	)
	@ApiLogging(
		message = "🔒 토스 결제 승인 처리",
		includeRequest = true,
		includeResponse = true,
		includeExecutionTime = true,
		maskSensitiveData = true,
		level = ApiLogging.LogLevel.WARN
	)
	@ValidateRequest(
		validateNulls = true,
		validateEmpty = true,
		errorMessage = "결제 승인 요청 데이터가 올바르지 않습니다.",
		exceptionType = SecurityException.class
	)
	@AuditLog(
		action = "PAYMENT_CONFIRM",
		resource = "PAYMENT",
		description = "🚨 중요: 토스 결제 승인이 처리되었습니다",
		userIdExpression = "T(org.springframework.security.core.context.SecurityContextHolder).context.authentication?.principal?.userId ?: 'system'",
		resourceIdExpression = "#request.orderId",
		includeRequestData = true,
		includeResponseData = true,
		level = AuditLog.Level.ERROR
	)
	@Idempotent(
		keyExpression = "#request.orderId + ':toss_confirm:' + @idempotencyKeyGenerator.hash(#request)",
		keyPrefix = "payment_confirm",
		responseType = TossPaymentConfirmResponse.class
	)
	@RetryOnFailure(
		maxAttempts = 2,
		backoffMillis = 500,
		exponentialBackoff = false,
		retryOn = {RuntimeException.class},
		noRetryOn = {SecurityException.class, IllegalArgumentException.class},
		logRetryAttempts = true
	)
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
