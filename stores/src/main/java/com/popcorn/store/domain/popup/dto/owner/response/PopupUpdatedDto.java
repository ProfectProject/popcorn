package com.popcorn.store.domain.popup.dto.owner.response;

import com.popcorn.store.domain.popup.entity.enums.PopupCategory;
import com.popcorn.store.domain.popup.entity.enums.PopupStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopupUpdatedDto {

    private UUID popupId;
    private String title;
    private String description;
    private PopupStatus status;
    private PopupCategory popupCategory;
    private LocalDateTime reservationOpenAt;
    private String addressRoad;
    private String addressDetail;
    private Long updatedBy;
    private LocalDateTime updatedAt;

}
