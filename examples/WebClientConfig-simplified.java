// Istio 도입 후 단순화된 WebClient 설정 예시
// order/src/main/java/com/popcorn/order/config/WebClientConfig.java

package com.popcorn.order.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    /**
     * Payment Service WebClient
     * Istio가 타임아웃, 재시도, Circuit Breaker를 처리하므로 단순화
     */
    @Bean
    @Qualifier("paymentWebClient")
    public WebClient paymentWebClient(@Value("${services.payment.url}") String paymentServiceUrl) {
        return WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // Istio가 처리하므로 타임아웃 설정 제거
                // .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
    
    /**
     * User Service WebClient
     */
    @Bean
    @Qualifier("userWebClient")
    public WebClient userWebClient(@Value("${services.user.url}") String userServiceUrl) {
        return WebClient.builder()
                .baseUrl(userServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    /**
     * Store Service WebClient
     */
    @Bean
    @Qualifier("storeWebClient")
    public WebClient storeWebClient(@Value("${services.store.url}") String storeServiceUrl) {
        return WebClient.builder()
                .baseUrl(storeServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    // 기존 복잡한 HttpClient 설정 제거
    /*
    private HttpClient createHttpClient(Duration timeout) {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(timeout)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(30))
                        .addHandlerLast(new WriteTimeoutHandler(10)));
    }
    */
}