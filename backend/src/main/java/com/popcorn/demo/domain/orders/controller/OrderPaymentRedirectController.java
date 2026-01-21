package com.popcorn.demo.domain.orders.controller;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.demo.domain.manager.handler.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/orders/v1")
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentRedirectController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${microservices.order.base-url:http://localhost:8084}")
    private String orderServiceBaseUrl;

    @Value("${microservices.payment.base-url:http://localhost:8085}")
    private String paymentServiceBaseUrl;

    @GetMapping("/{orderId}/pay")
    public ResponseEntity<Void> redirectToPayment(@PathVariable UUID orderId, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        Map<String, Object> orderData = fetchOrderDetail(orderId, authHeader);

        Number amountNumber = (Number) orderData.get("totalAmount");
        Integer amount = amountNumber != null ? amountNumber.intValue() : null;
        String orderNo = (String) orderData.get("orderNo");
        Object customerId = orderData.get("customerId");

        if (amount == null || orderNo == null) {
            throw new NotFoundException("주문 정보를 찾을 수 없습니다.");
        }

        String paymentUrl = createPaymentUrl(orderId, amount, orderNo, customerId);
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(paymentUrl))
            .build();
    }

    private Map<String, Object> fetchOrderDetail(UUID orderId, String authHeader) {
        String url = String.format("%s/api/orders/v1/%s", orderServiceBaseUrl, orderId);
        try {
            HttpHeaders headers = new HttpHeaders();
            if (authHeader != null && !authHeader.isBlank()) {
                headers.set("Authorization", authHeader);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            BaseResponse response = restTemplate.exchange(
                url, HttpMethod.GET, entity, BaseResponse.class
            ).getBody();
            if (response == null || response.getData() == null) {
                throw new NotFoundException("주문 정보를 찾을 수 없습니다.");
            }
            return objectMapper.convertValue(response.getData(), Map.class);
        } catch (RestClientException e) {
            log.error("주문 조회 실패: orderId={}", orderId, e);
            throw new NotFoundException("주문 정보를 찾을 수 없습니다.");
        }
    }

    private String createPaymentUrl(UUID orderId, Integer amount, String orderNo, Object customerId) {
        String url = paymentServiceBaseUrl + "/api/pay/v1/payments";
        Map<String, Object> request = new HashMap<>();
        request.put("orderId", orderId);
        request.put("paymentMethod", "CARD");
        request.put("amount", amount);
        request.put("orderNo", orderNo);
        request.put("itemName", "팝업스토어 주문 - " + orderNo);
        if (customerId != null) {
            request.put("customerId", customerId);
        }

        try {
            Map response = restTemplate.postForObject(url, request, Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new IllegalStateException("결제 URL 생성 실패");
            }
            Map data = (Map) response.get("data");
            String paymentUrl = data != null ? (String) data.get("paymentUrl") : null;
            if (paymentUrl == null) {
                throw new IllegalStateException("결제 URL이 없습니다");
            }
            return paymentUrl;
        } catch (RestClientException e) {
            log.error("결제 URL 생성 실패: orderId={}", orderId, e);
            throw new IllegalStateException("결제 URL 생성에 실패했습니다.");
        }
    }
}
