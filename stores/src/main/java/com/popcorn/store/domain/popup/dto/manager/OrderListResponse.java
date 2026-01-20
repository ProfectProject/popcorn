package com.popcorn.store.domain.popup.dto.manager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.popcorn.store.domain.popup.entity.enums.PopupCategory;
import com.popcorn.store.domain.popup.entity.enums.PopupStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderListResponse {

    private List<ItemDto> items;
    private int page;
    private int size;
    private long total;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDto {

        private UUID popupId;
        private UUID storeId;
        private String title;
        private PopupCategory popupCategory;
        private PopupStatus status;
        private LocalDateTime eventStartAt;
        private LocalDateTime eventEndAt;
    }
}
