package com.popcorn.demo.domain.popup.dto.owner.response;

import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopupListDto {

    private UUID popupId;
    private String title;
    private PopupCategory popupCategory;
    private PopupStatus status;
    private LocalDateTime reservationOpenAt;
    private String addressRoad;
    private String addressDetail;
    private LocalDateTime createdAt;

}
