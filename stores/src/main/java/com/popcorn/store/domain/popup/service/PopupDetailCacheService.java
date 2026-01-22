package com.popcorn.store.domain.popup.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.popcorn.common.annotation.RedisCacheResult;
import com.popcorn.store.domain.popup.dto.query.PopupDetailQuery;
import com.popcorn.store.domain.popup.dto.query.response.PopupDetailResponse;
import com.popcorn.store.domain.popup.dto.query.response.PopupScheduleListResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopupDetailCacheService {

    private final PopupQueryService popupQueryService;

    @RedisCacheResult(
            cacheName = "popup",
            keyExpression = "#query.popupId + ':detail'",
            ttlSeconds = 600
    )
    public PopupDetailResponse getPopupDetailCached(PopupDetailQuery query) {
        // 캐시 미스일 때만 DB 조회로 내려오며,
        // 반환값은 RedisCacheResultAspect가 Redis에 저장한다.
        PopupDetailResponse response = popupQueryService.getPopupDetail(query);
        // remainingCapacity는 실시간성이 필요한 값이라 캐시에 넣지 않는다.
        return stripRemainingCapacity(response);
    }

    private PopupDetailResponse stripRemainingCapacity(PopupDetailResponse response) {
        if (response == null || response.getSchedules() == null) {
            return response;
        }

        // schedules의 remainingCapacity를 null로 비워 캐시에 저장될 값에서 제외한다.
        List<PopupScheduleListResponse.ItemDto> schedules = response.getSchedules().stream()
                .map(item -> PopupScheduleListResponse.ItemDto.builder()
                        .id(item.getId())
                        .startAt(item.getStartAt())
                        .endAt(item.getEndAt())
                        .price(item.getPrice())
                        .capacity(item.getCapacity())
                        .remainingCapacity(null)
                        .isActive(item.getIsActive())
                        .build())
                .toList();

        return PopupDetailResponse.builder()
                .id(response.getId())
                .storeId(response.getStoreId())
                .title(response.getTitle())
                .description(response.getDescription())
                .category(response.getCategory())
                .status(response.getStatus())
                .reservationOpenAt(response.getReservationOpenAt())
                .addressRoad(response.getAddressRoad())
                .addressDetail(response.getAddressDetail())
                .eventStartAt(response.getEventStartAt())
                .eventEndAt(response.getEventEndAt())
                .schedules(schedules)
                .build();
    }
}
