package com.popcorn.order.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 팝업 정보 조회 요청 이벤트
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopupInfoLookupRequestedEvent {
    private String eventId;
    private String correlationId;
    private java.util.UUID popupId;
    private LocalDateTime requestedAt;
}
