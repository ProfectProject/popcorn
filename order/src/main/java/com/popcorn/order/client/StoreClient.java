package com.popcorn.order.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.popcorn.common.dto.BaseResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Store 마이크로서비스와 통신하는 HTTP Client
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${microservices.store.base-url}")
    private String storeBaseUrl;

    public void completeGoodsReservation(UUID popupId, UUID goodsVariantId, Integer quantity) {
        log.info("재고 차감 확정 요청 - popupId: {}, goodsVariantId: {}, quantity: {}",
                popupId, goodsVariantId, quantity);

        BaseResponse<Object> response = webClientBuilder.build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path(storeBaseUrl + "/api/stores/v1/popups/{popupId}/goods/{goodsId}/reservation/complete")
                        .queryParam("quantity", quantity)
                        .build(popupId, goodsVariantId))
                .headers(headers -> {
                    String authHeader = resolveAuthHeader();
                    if (authHeader != null) {
                        headers.set("Authorization", authHeader);
                    }
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponse<Object>>() {})
                .block();

        if (response == null || response.getCode() != 200) {
            String message = response != null ? response.getMessage() : "스토어 응답이 비어 있습니다.";
            throw new IllegalStateException("재고 차감 확정 실패: " + message);
        }

        log.info("재고 차감 확정 성공 - popupId: {}, goodsVariantId: {}, quantity: {}",
                popupId, goodsVariantId, quantity);
    }

    private String resolveAuthHeader() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null || attrs.getRequest() == null) {
                return null;
            }
            return attrs.getRequest().getHeader("Authorization");
        } catch (Exception e) {
            return null;
        }
    }
}
