package com.popcorn.store.domain.popup.dto.manager;

import java.time.LocalDateTime;
import java.util.UUID;

import com.popcorn.store.domain.popup.entity.enums.PopupStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreWithdrawResponse {

    private UUID popupId;
    private PopupStatus status;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
