package com.popcorn.demo.domain.payment.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.popcorn.common.controller.BaseController;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.common.versioning.ApiVersion;
import com.popcorn.demo.domain.payment.service.PaymentTokenService;
import com.popcorn.demo.domain.payment.toss.TossPaymentsProperties;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;

import com.popcorn.common.annotation.ApiLogging;
import com.popcorn.common.annotation.AuditLog;
import com.popcorn.common.annotation.RedisCacheResult;
import com.popcorn.common.annotation.RateLimit;
import com.popcorn.common.annotation.RetryOnFailure;
import com.popcorn.common.annotation.ValidateRequest;

@RestController
@ApiVersion("v1")
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentPageController extends BaseController {

	private final PaymentTokenService paymentTokenService;
	private final TossPaymentsProperties tossPaymentsProperties;

	@Operation(
			summary = "토큰 기반 결제 페이지 리다이렉트",
			description = """
				JWT 토큰으로 암호화된 결제 정보를 이용해 결제 페이지로 리다이렉트합니다.

				🛡️ 보안 조치:
				- 토큰 유효성 검증 필수
				- 분당 10회 리다이렉트 제한
				- 모든 토큰 사용 기록을 감사 로그에 저장
				""",
			hidden = true
	)
	@ApiResponse(
		responseCode = "302",
		description = "결제 페이지로 리다이렉트"
	)
	@ApiResponse(
		responseCode = "400",
		description = "토큰이 유효하지 않음"
	)
	@GetMapping("/checkout")
	@RateLimit(
		requests = 10,
		window = 60,
		keyExpression = "T(org.springframework.web.context.request.RequestContextHolder).currentRequestAttributes().getRequest().getRemoteAddr()",
		errorMessage = "결제 페이지 리다이렉트 요청이 너무 많습니다.",
		algorithm = RateLimit.Algorithm.FIXED_WINDOW
	)
	@ApiLogging(
		message = "🔐 결제 페이지 토큰 리다이렉트",
		includeRequest = true,
		includeResponse = false,
		includeExecutionTime = true,
		maskSensitiveData = true,
		level = ApiLogging.LogLevel.INFO
	)
	@ValidateRequest(
		validateNulls = true,
		validateEmpty = true,
		errorMessage = "결제 토큰이 제공되지 않았습니다.",
		exceptionType = IllegalArgumentException.class
	)
	@AuditLog(
		action = "PAYMENT_TOKEN_REDIRECT",
		resource = "PAYMENT_TOKEN",
		description = "결제 토큰 기반 페이지 리다이렉트",
		userIdExpression = "T(org.springframework.security.core.context.SecurityContextHolder).context.authentication?.principal?.userId ?: 'anonymous'",
		resourceIdExpression = "'token_' + T(java.util.UUID).randomUUID().toString()",
		includeRequestData = true,
		level = AuditLog.Level.INFO
	)
	@RetryOnFailure(
		maxAttempts = 2,
		backoffMillis = 200,
		retryOn = {RuntimeException.class},
		noRetryOn = {SecurityException.class, IllegalArgumentException.class},
		logRetryAttempts = false
	)
	public ResponseEntity<Void> redirectToPaymentPage(
			@Parameter(description = "JWT 암호화된 결제 토큰", required = true)
			@RequestParam String token) {

		// 토큰 유효성 검증
		if (!paymentTokenService.isValidToken(token)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

		// 토큰을 파라미터로 포함한 결제 페이지 URL 생성
		String redirectUrl = UriComponentsBuilder.fromHttpUrl(tossPaymentsProperties.getCheckoutUrl())
				.queryParam("token", token) // 암호화된 토큰만 전달
				.build(true)
				.toUriString();

		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(redirectUrl))
				.build();
	}

	@Operation(
			summary = "토큰에서 결제 정보 추출",
			description = """
				JWT 토큰을 디코딩하여 결제에 필요한 정보를 반환합니다.

				🛡️ 보안 조치:
				- 토큰 변조 및 만료 검증
				- 분당 15회 디코딩 제한
				- 토큰 사용 패턴 감사
				- 유효한 토큰만 짧은 시간 캐싱
				""",
			hidden = true
	)
	@ApiResponse(
		responseCode = "200",
		description = "토큰 디코딩 성공",
		content = @Content(schema = @Schema(implementation = PaymentTokenService.PaymentTokenInfo.class))
	)
	@ApiResponse(
		responseCode = "400",
		description = "토큰 디코딩 실패"
	)
	@GetMapping("/decode")
	@RateLimit(
		requests = 15,
		window = 60,
		keyExpression = "T(org.springframework.web.context.request.RequestContextHolder).currentRequestAttributes().getRequest().getRemoteAddr()",
		errorMessage = "토큰 디코딩 요청이 너무 많습니다.",
		algorithm = RateLimit.Algorithm.SLIDING_WINDOW
	)
	@ApiLogging(
		message = "🔍 결제 토큰 디코딩",
		includeRequest = true,
		includeResponse = true,
		includeExecutionTime = true,
		maskSensitiveData = true,
		level = ApiLogging.LogLevel.INFO
	)
	@ValidateRequest(
		validateNulls = true,
		validateEmpty = true,
		errorMessage = "디코딩할 토큰이 제공되지 않았습니다.",
		exceptionType = IllegalArgumentException.class
	)
	@AuditLog(
		action = "PAYMENT_TOKEN_DECODE",
		resource = "PAYMENT_TOKEN",
		description = "결제 토큰 디코딩 요청",
		userIdExpression = "T(org.springframework.security.core.context.SecurityContextHolder).context.authentication?.principal?.userId ?: 'anonymous'",
		resourceIdExpression = "'token_decode_' + T(java.time.Instant).now().epochSecond",
		includeRequestData = false,
		includeResponseData = true,
		level = AuditLog.Level.INFO
	)
	@RedisCacheResult(
		cacheName = "paymentTokenDecodeCache",
		keyExpression = "T(java.util.Objects).hash(#token)",
		ttlSeconds = 30,
		condition = "#token != null && #token.length() > 10"
	)
	@RetryOnFailure(
		maxAttempts = 2,
		backoffMillis = 150,
		retryOn = {RuntimeException.class},
		noRetryOn = {IllegalArgumentException.class, SecurityException.class},
		logRetryAttempts = true
	)
	public ResponseEntity<BaseResponse<PaymentTokenService.PaymentTokenInfo>> decodePaymentToken(
			@Parameter(description = "JWT 암호화된 결제 토큰", required = true)
			@RequestParam String token) {

		try {
			PaymentTokenService.PaymentTokenInfo paymentInfo = paymentTokenService.parsePaymentToken(token);
			return ok(paymentInfo);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}
}