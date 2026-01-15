package com.popcorn.demo.domain.payment.toss;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TossPaymentsClient {

	private final RestTemplate restTemplate;
	private final TossPaymentsProperties properties;

	public TossPaymentsClient(RestTemplateBuilder restTemplateBuilder, TossPaymentsProperties properties) {
		// 🔧 TossPayments API 전용 타임아웃 설정
		this.restTemplate = restTemplateBuilder
			.setConnectTimeout(Duration.ofSeconds(3))     // 연결 타임아웃 3초
			.setReadTimeout(Duration.ofSeconds(5))        // 읽기 타임아웃 5초
			.build();
		this.properties = properties;

		log.info("🔧 TossPayments RestTemplate 초기화 완료 - connectTimeout: 3s, readTimeout: 5s");
	}

	@CircuitBreaker(name = "tossPaymentApi", fallbackMethod = "confirmFallback")
	public TossPaymentsConfirmResponse confirm(TossPaymentsConfirmRequest request) {
		log.debug("🎯 Toss Payment API 호출 - 결제 승인: {}", request.getOrderId());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader());
		HttpEntity<TossPaymentsConfirmRequest> entity = new HttpEntity<>(request, headers);
		String url = properties.getBaseUrl() + "/v1/payments/confirm";
		return restTemplate.postForObject(url, entity, TossPaymentsConfirmResponse.class);
	}

	/**
	 * 결제 승인 Circuit Breaker Fallback
	 * 기존 동작 유지: 예외를 그대로 던져서 상위 레이어에서 처리
	 */
	public TossPaymentsConfirmResponse confirmFallback(TossPaymentsConfirmRequest request, Exception ex) {
		log.error("🚨 Toss Payment API Circuit Breaker 열림 - 결제 승인 실패: orderId={}, error={}",
			request.getOrderId(), ex.getMessage());

		// 기존 동작 유지: 원본 예외를 그대로 던짐
		if (ex instanceof RuntimeException) {
			throw (RuntimeException) ex;
		}
		throw new RuntimeException("결제 API 서비스가 불안정합니다. 잠시 후 다시 시도해주세요.", ex);
	}

	@CircuitBreaker(name = "tossPaymentApi", fallbackMethod = "cancelFallback")
	public TossPaymentsCancelResponse cancel(String paymentKey, TossPaymentsCancelRequest request) {
		log.debug("🎯 Toss Payment API 호출 - 결제 취소: {}", paymentKey);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader());
		HttpEntity<TossPaymentsCancelRequest> entity = new HttpEntity<>(request, headers);
		String url = properties.getBaseUrl() + "/v1/payments/" + paymentKey + "/cancel";
		return restTemplate.postForObject(url, entity, TossPaymentsCancelResponse.class);
	}

	/**
	 * 결제 취소 Circuit Breaker Fallback
	 * 기존 동작 유지: 예외를 그대로 던져서 상위 레이어에서 처리
	 */
	public TossPaymentsCancelResponse cancelFallback(String paymentKey, TossPaymentsCancelRequest request, Exception ex) {
		log.error("🚨 Toss Payment API Circuit Breaker 열림 - 결제 취소 실패: paymentKey={}, error={}",
			paymentKey, ex.getMessage());

		// 기존 동작 유지: 원본 예외를 그대로 던짐
		if (ex instanceof RuntimeException) {
			throw (RuntimeException) ex;
		}
		throw new RuntimeException("결제 취소 API 서비스가 불안정합니다. 잠시 후 다시 시도해주세요.", ex);
	}

	private String buildAuthorizationHeader() {
		String secretKey = properties.getSecretKey();
		String token = secretKey == null ? "" : secretKey;
		String encoded = Base64.getEncoder()
				.encodeToString((token + ":").getBytes(StandardCharsets.UTF_8));
		return "Basic " + encoded;
	}
}
