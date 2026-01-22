package com.popcorn.store.domain.popup.dto.manager;

import com.popcorn.store.domain.popup.entity.enums.PopupStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusUpdateRequest {

    private PopupStatus status;
    private Long managerId;
}
