package com.popcorn.order.event;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 가격 조회 응답 이벤트 (Store -> Order)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceLookupResponseEvent {

    private String eventId;
    private String correlationId;
    private String requestType;
    private UUID sessionId;
    private UUID goodsId;
    private Integer price;
    private Integer stockQuantity;
    private boolean success;
    private String message;
    private LocalDateTime respondedAt;
    private LocalDateTime eventTime;

    public static PriceLookupResponseEvent failure(String correlationId, String requestType, UUID sessionId,
                                                   UUID goodsId, String message) {
        LocalDateTime now = LocalDateTime.now();
        return PriceLookupResponseEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(correlationId)
                .requestType(requestType)
                .sessionId(sessionId)
                .goodsId(goodsId)
                .success(false)
                .message(message)
                .respondedAt(now)
                .eventTime(now)
                .build();
    }
}
