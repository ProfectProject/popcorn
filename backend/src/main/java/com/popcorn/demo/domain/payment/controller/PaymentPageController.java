package com.popcorn.demo.domain.payment.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.versioning.ApiVersion;
import com.popcorn.demo.domain.payment.service.PaymentTokenService;
import com.popcorn.demo.domain.payment.toss.TossPaymentsProperties;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

@RestController
@ApiVersion("v1")
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentPageController extends BaseController {

	private final PaymentTokenService paymentTokenService;
	private final TossPaymentsProperties tossPaymentsProperties;

	@Operation(
			summary = "토큰 기반 결제 페이지 리다이렉트",
			description = "JWT 토큰으로 암호화된 결제 정보를 이용해 결제 페이지로 리다이렉트합니다.",
			hidden = true
	)
	@GetMapping("/checkout")
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
			description = "JWT 토큰을 디코딩하여 결제에 필요한 정보를 반환합니다.",
			hidden = true
	)
	@GetMapping("/decode")
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