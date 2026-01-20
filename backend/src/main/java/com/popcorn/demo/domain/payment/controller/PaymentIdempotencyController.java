package com.popcorn.demo.domain.payment.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.common.annotation.ApiLogging;
import com.popcorn.common.annotation.AuditLog;
import com.popcorn.common.cache.IdempotencyService;
import com.popcorn.common.controller.BaseController;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.common.versioning.ApiVersion;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;

@Hidden
@RestController
@ApiVersion("v1")
@RequestMapping("/api/v1/payments/idempotency")
@RequiredArgsConstructor
public class PaymentIdempotencyController extends BaseController {

	private static final Logger log = LoggerFactory.getLogger(PaymentIdempotencyController.class);

	private final IdempotencyService idempotencyService;

	@DeleteMapping("/cache/prefix/{keyPrefix}")
	@ApiLogging(
		message = "⚠️ 결제 멱등성 접두사 캐시 삭제",
		includeRequest = true,
		includeResponse = true,
		level = ApiLogging.LogLevel.WARN
	)
	@AuditLog(
		action = "PAYMENT_CACHE_PREFIX_DELETE",
		resource = "PAYMENT_IDEMPOTENCY_CACHE",
		description = "⚠️ 결제 멱등성 캐시 접두사를 삭제했습니다",
		userIdExpression = "T(org.springframework.security.core.context.SecurityContextHolder).context.authentication?.principal?.userId ?: 'anonymous'",
		resourceIdExpression = "#keyPrefix",
		level = AuditLog.Level.WARN,
		includeRequestData = true
	)
	public ResponseEntity<BaseResponse<String>> clearByPrefix(@PathVariable String keyPrefix) {
		log.warn("🗑️ 결제 멱등성 캐시 접두사 삭제 요청 - prefix: {}", keyPrefix);

		if (keyPrefix == null || keyPrefix.trim().isEmpty()) {
			throw new IllegalArgumentException("올바른 멱등성 키 접두사를 입력해주세요");
		}

		try {
			idempotencyService.clearByPrefix(keyPrefix);
			String message = String.format("멱등성 접두사 '%s'의 캐시가 성공적으로 삭제되었습니다", keyPrefix);
			return ok(message);
		} catch (Exception e) {
			log.error("❌ 결제 접두사 캐시 삭제 실패 - prefix: {}", keyPrefix, e);
			throw new RuntimeException("접두사 캐시 삭제 중 오류가 발생했습니다", e);
		}
	}
}
