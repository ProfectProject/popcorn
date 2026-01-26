package com.example.orderquery.domain.event;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.orderquery.domain.itemView.service.CheckInItemUpsertService;
import com.example.orderquery.domain.summary.service.CheckInSummaryUpsertService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class QrEventListener {
    private static final Set<String> SUPPORTED_EVENTS = Set.of("CheckInCreatedEvent");

    private final CheckInSummaryUpsertService upsertSummaryService;
    private final CheckInItemUpsertService upsertItemService;

    @EventListener
    public void handleCheckInEvent(Object event) {
        if (event == null) {
            return;
        }

        String eventName = event.getClass().getSimpleName();

        // 모든 이벤트 수신 로그
        log.info("🔔 [ORDERQUERY] QR 이벤트 수신 - type: {}", eventName);

        if (!SUPPORTED_EVENTS.contains(eventName)) {
            log.debug("🔍 [ORDERQUERY] 지원하지 않는 QR 이벤트 타입 - type: {}", eventName);
            return;
        }

        log.info("📱 [ORDERQUERY] QR 체크인 이벤트 처리 시작 - type: {}", eventName);

        UUID orderGoodsId = (UUID) invokeAny(event, "getOrderGoodsId", "getOrderGoodsID");
        UUID storeId = (UUID) invokeAny(event, "getStoreId");
        UUID popupId = (UUID) invokeAny(event, "getPopupId");
        LocalDateTime checkinAt = toLocalDateTime(invokeAny(event, "getCheckinAt", "getCheckInAt"));

        if (orderGoodsId == null || storeId == null || popupId == null || checkinAt == null) {
            log.warn("CheckIn event ignored: missing fields. storeId={}, popupId={}, orderGoodsId={}, checkinAt={}",
                    storeId, popupId, orderGoodsId, checkinAt);
            return;
        }

        upsertItemService.updateFromCheckIn(storeId, popupId, orderGoodsId, checkinAt);
        upsertSummaryService.incrementCheckedIn(storeId, popupId);
    }

    private Object invokeAny(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            Object value = invoke(target, methodName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Object invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toLocalDateTime();
        }
        if (value instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        }
        return null;
    }
}
