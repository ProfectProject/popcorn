package com.popcorn.order.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.popcorn.order.dto.store.PopupInfoResponse;
import com.popcorn.order.event.PopupInfoLookupRequestedEvent;
import com.popcorn.order.event.PopupInfoLookupResponseEvent;
import com.popcorn.order.event.RedisEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Order 서비스의 Popup 정보 조회 서비스 (이벤트 기반)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPopupLookupService {

    private final RedisEventPublisher redisEventPublisher;

    private final ConcurrentHashMap<String, CompletableFuture<PopupInfoLookupResponseEvent>> pendingRequests = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Value("${order.popup-lookup.timeout-ms:300}")
    private long timeoutMs;

    public Optional<PopupInfoResponse> getPopupInfo(UUID popupId) {
        try {
            String correlationId = UUID.randomUUID().toString();

            CompletableFuture<PopupInfoLookupResponseEvent> future = new CompletableFuture<>();
            pendingRequests.put(correlationId, future);

            PopupInfoLookupRequestedEvent requestEvent = PopupInfoLookupRequestedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .correlationId(correlationId)
                    .popupId(popupId)
                    .requestedAt(LocalDateTime.now())
                    .build();

            redisEventPublisher.publishPopupInfoLookupRequestedEvent(
                    requestEvent.getEventId(),
                    requestEvent.getCorrelationId(),
                    requestEvent.getPopupId()
            );
            log.info("팝업 정보 조회 요청 이벤트 발행 - popupId: {}, correlationId: {}", popupId, correlationId);

            PopupInfoLookupResponseEvent response = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (!response.isSuccess()) {
                return Optional.empty();
            }

            PopupInfoResponse.StoreInfo storeInfo = PopupInfoResponse.StoreInfo.builder()
                    .storeId(response.getStoreId())
                    .name(response.getStoreName())
                    .address1(response.getAddress1())
                    .address2(response.getAddress2())
                    .phoneNumber(response.getPhoneNumber())
                    .build();

            PopupInfoResponse popupInfo = PopupInfoResponse.builder()
                    .popupId(response.getPopupId())
                    .title(response.getTitle())
                    .description(response.getDescription())
                    .storeId(response.getStoreId())
                    .storeInfo(storeInfo)
                    .startDate(response.getStartDate())
                    .endDate(response.getEndDate())
                    .status(response.getStatus())
                    .build();

            return Optional.of(popupInfo);

        } catch (Exception e) {
            log.warn("팝업 정보 조회 응답 대기 실패 - popupId: {}, error: {}", popupId, e.getMessage());
            return Optional.empty();
        }
    }

    public void handlePopupInfoLookupResponse(PopupInfoLookupResponseEvent response) {
        String correlationId = response.getCorrelationId();
        CompletableFuture<PopupInfoLookupResponseEvent> future = pendingRequests.remove(correlationId);

        if (future != null) {
            log.info("팝업 정보 조회 응답 처리 완료 - correlationId: {}, success: {}",
                    correlationId, response.isSuccess());
            future.complete(response);
        } else {
            log.warn("해당하는 팝업 정보 조회 요청을 찾을 수 없음 - correlationId: {}", correlationId);
        }
    }
}
