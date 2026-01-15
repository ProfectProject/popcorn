package com.popcorn.demo.domain.payment.toss;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TossPaymentsClient {

	private final RestTemplate restTemplate;
	private final TossPaymentsProperties properties;

	public TossPaymentsClient(RestTemplateBuilder restTemplateBuilder, TossPaymentsProperties properties) {
		this.restTemplate = restTemplateBuilder.build();
		this.properties = properties;
	}

	public TossPaymentsConfirmResponse confirm(TossPaymentsConfirmRequest request) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader());
		HttpEntity<TossPaymentsConfirmRequest> entity = new HttpEntity<>(request, headers);
		String url = properties.getBaseUrl() + "/v1/payments/confirm";
		return restTemplate.postForObject(url, entity, TossPaymentsConfirmResponse.class);
	}

	public TossPaymentsCancelResponse cancel(String paymentKey, TossPaymentsCancelRequest request) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader());
		HttpEntity<TossPaymentsCancelRequest> entity = new HttpEntity<>(request, headers);
		String url = properties.getBaseUrl() + "/v1/payments/" + paymentKey + "/cancel";
		return restTemplate.postForObject(url, entity, TossPaymentsCancelResponse.class);
	}

	private String buildAuthorizationHeader() {
		String secretKey = properties.getSecretKey();
		String token = secretKey == null ? "" : secretKey;
		String encoded = Base64.getEncoder()
				.encodeToString((token + ":").getBytes(StandardCharsets.UTF_8));
		return "Basic " + encoded;
	}
}
