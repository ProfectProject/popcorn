package com.popcorn.checkIns.event.standard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 표준 체크인 생성 이벤트
 * 체크인이 완료되었을 때 발행되는 이벤트
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StandardCheckinCreatedEvent extends StandardBaseEvent {

    /**
     * 체크인 ID
     */
    private UUID checkinId;

    /**
     * QR 코드 ID
     */
    private UUID qrId;

    /**
     * 체크인 시간
     */
    private LocalDateTime checkinAt;

    /**
     * 체크인 생성자
     */
    private Long checkinCreatedBy;

    /**
     * QR 코드
     */
    private String qrCode;

    /**
     * 체크인 위치 (optional)
     */
    private String checkinLocation;

    /**
     * 정적 팩토리 메서드
     */
    public static StandardCheckinCreatedEvent create(UUID orderId, String orderNo, Long userId, UUID storeId, UUID popupId,
                                                     UUID checkinId, UUID qrId, String qrCode, Long checkinCreatedBy,
                                                     String checkinLocation) {
        StandardCheckinCreatedEvent event = StandardCheckinCreatedEvent.builder()
                .eventType(StandardEventType.CHECKIN_CREATED)
                .producer("checkins-service")
                .orderId(orderId)
                .orderNo(orderNo)
                .userId(userId)
                .storeId(storeId)
                .popupId(popupId)
                .checkinId(checkinId)
                .qrId(qrId)
                .qrCode(qrCode)
                .checkinAt(LocalDateTime.now())
                .checkinCreatedBy(checkinCreatedBy)
                .checkinLocation(checkinLocation)
                .build();

        event.setDefaults();
        return event;
    }

    /**
     * Redis Stream 발행용 Map 변환 (체크인 전용 필드 추가)
     */
    @Override
    public java.util.Map<String, String> toStreamMap() {
        java.util.Map<String, String> map = super.toStreamMap();

        // 체크인 전용 필드 추가
        if (checkinId != null) map.put("checkinId", checkinId.toString());
        if (qrId != null) map.put("qrId", qrId.toString());
        if (checkinAt != null) map.put("checkinAt", checkinAt.toString());
        if (checkinCreatedBy != null) map.put("checkinCreatedBy", checkinCreatedBy.toString());
        if (qrCode != null) map.put("qrCode", qrCode);
        if (checkinLocation != null) map.put("checkinLocation", checkinLocation);

        return map;
    }
}