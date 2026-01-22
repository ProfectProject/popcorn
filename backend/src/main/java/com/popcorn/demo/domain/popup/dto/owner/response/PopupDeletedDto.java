package com.popcorn.demo.domain.popup.dto.owner.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopupDeletedDto {

    private UUID popupId;
    private String title;
    private LocalDateTime reservationOpenAt;
    private String addressRoad;
    private String addressDetail;
    private LocalDateTime deletedAt;
    private Long deletedBy;
}
