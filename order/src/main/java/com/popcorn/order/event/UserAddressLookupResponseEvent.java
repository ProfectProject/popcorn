package com.popcorn.order.event;

import com.popcorn.order.dto.user.UserAddressResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User 서비스로부터 주소 조회 응답 이벤트
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressLookupResponseEvent {
    private String eventId;
    private String correlationId;
    private String requestType;
    private Long userId;
    private List<UserAddressResponse> addresses;
    private boolean success;
    private String message;
    private LocalDateTime respondedAt;
}