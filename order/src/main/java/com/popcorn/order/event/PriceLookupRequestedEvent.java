package com.popcorn.order.event;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 가격 조회 요청 이벤트 (Order -> Store)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceLookupRequestedEvent {

    public static final String TYPE_SESSION = "SESSION";
    public static final String TYPE_GOODS = "GOODS";

    private String eventId;
    private String correlationId;
    private String requestType;
    private UUID sessionId;
    private UUID goodsVariantId;
    private LocalDateTime requestedAt;
    private LocalDateTime eventTime;

    public static PriceLookupRequestedEvent forSession(UUID sessionId, String correlationId) {
        LocalDateTime now = LocalDateTime.now();
        return PriceLookupRequestedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(correlationId)
                .requestType(TYPE_SESSION)
                .sessionId(sessionId)
                .requestedAt(now)
                .eventTime(now)
                .build();
    }

    public static PriceLookupRequestedEvent forGoods(UUID goodsVariantId, String correlationId) {
        LocalDateTime now = LocalDateTime.now();
        return PriceLookupRequestedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId(correlationId)
                .requestType(TYPE_GOODS)
                .goodsVariantId(goodsVariantId)
                .requestedAt(now)
                .eventTime(now)
                .build();
    }
}
