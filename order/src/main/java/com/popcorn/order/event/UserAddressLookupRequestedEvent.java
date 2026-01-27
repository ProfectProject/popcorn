package com.popcorn.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User 서비스로 주소 조회 요청 이벤트
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressLookupRequestedEvent {
    private String eventId;
    private String correlationId;
    private String requestType;  // "DEFAULT_ADDRESS"
    private Long userId;
    private LocalDateTime requestedAt;
}