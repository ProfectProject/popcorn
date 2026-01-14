package com.popcorn.demo.domain.payment.toss;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "toss.payments")
public class TossPaymentsProperties {

	private String baseUrl;
	private String secretKey;
	private String clientKey;  // 🎯 프론트엔드용 클라이언트 키 추가
	private String checkoutUrl;
	private String successUrl;
	private String failUrl;
}
