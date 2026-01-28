package com.popcorn.checkIns.event.standard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 표준 QR 코드 생성 이벤트
 * QR 코드가 생성되었을 때 발행되는 이벤트
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardQrGeneratedEvent extends StandardBaseEvent {

    /**
     * QR 코드 ID
     */
    private UUID qrId;

    /**
     * QR 코드
     */
    private String qrCode;

    /**
     * QR 코드 만료 시간
     */
    private LocalDateTime expiresAt;

    /**
     * QR 코드 TTL (초)
     */
    private Long ttlSeconds;

    /**
     * QR 코드 생성 시간
     */
    private LocalDateTime qrCreatedAt;

    /**
     * QR 코드 생성자
     */
    private Long qrCreatedBy;

    /**
     * 정적 팩토리 메서드
     */
    public static StandardQrGeneratedEvent create(UUID orderId, String orderNo, Long userId, UUID storeId, UUID popupId,
                                                  UUID qrId, String qrCode, LocalDateTime expiresAt, Long ttlSeconds,
                                                  Long qrCreatedBy) {
        StandardQrGeneratedEvent event = StandardQrGeneratedEvent.builder()
                .eventType(StandardEventType.QR_GENERATED)
                .producer("checkins-service")
                .orderId(orderId)
                .orderNo(orderNo)
                .userId(userId)
                .storeId(storeId)
                .popupId(popupId)
                .qrId(qrId)
                .qrCode(qrCode)
                .expiresAt(expiresAt)
                .ttlSeconds(ttlSeconds)
                .qrCreatedAt(LocalDateTime.now())
                .qrCreatedBy(qrCreatedBy)
                .build();

        event.setDefaults();
        return event;
    }

    /**
     * Redis Stream 발행용 Map 변환 (QR 전용 필드 추가)
     */
    @Override
    public java.util.Map<String, String> toStreamMap() {
        java.util.Map<String, String> map = super.toStreamMap();

        // QR 전용 필드 추가
        if (qrId != null) map.put("qrId", qrId.toString());
        if (qrCode != null) map.put("qrCode", qrCode);
        if (expiresAt != null) map.put("expiresAt", expiresAt.toString());
        if (ttlSeconds != null) map.put("ttlSeconds", ttlSeconds.toString());
        if (qrCreatedAt != null) map.put("qrCreatedAt", qrCreatedAt.toString());
        if (qrCreatedBy != null) map.put("qrCreatedBy", qrCreatedBy.toString());

        return map;
    }
}