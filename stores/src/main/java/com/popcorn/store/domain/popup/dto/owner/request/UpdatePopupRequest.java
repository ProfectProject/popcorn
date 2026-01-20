package com.popcorn.store.domain.popup.dto.owner.request;


import java.util.List;
import java.util.UUID;

import com.popcorn.store.domain.popup.entity.enums.PopupCategory;
import jakarta.validation.Valid;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdatePopupRequest {

    private String title;
    private String description;
    private PopupCategory popupCategory;
    private java.time.LocalDateTime reservationOpenAt;
    private String addressRoad;
    private String addressDetail;
    @Valid
    private List<CreatePopupScheduleRequest> createSchedules;
    @Valid
    private List<UpdatePopupScheduleRequest> updateSchedules;
    private List<UUID> deleteScheduleIds;

}
