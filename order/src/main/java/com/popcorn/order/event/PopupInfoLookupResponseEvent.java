package com.popcorn.order.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 팝업 정보 조회 응답 이벤트
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopupInfoLookupResponseEvent {
    private String eventId;
    private String correlationId;
    private UUID popupId;
    private boolean success;
    private String message;

    private String title;
    private String description;
    private UUID storeId;
    private String storeName;
    private String address1;
    private String address2;
    private String phoneNumber;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private LocalDateTime respondedAt;
    private LocalDateTime eventTime;
}
