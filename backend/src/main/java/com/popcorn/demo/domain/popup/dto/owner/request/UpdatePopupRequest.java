package com.popcorn.demo.domain.popup.dto.owner.request;


import java.util.List;
import java.util.UUID;

import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
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
    @Valid
    private List<CreatePopupScheduleRequest> createSchedules;
    @Valid
    private List<UpdatePopupScheduleRequest> updateSchedules;
    private List<UUID> deleteScheduleIds;

}
