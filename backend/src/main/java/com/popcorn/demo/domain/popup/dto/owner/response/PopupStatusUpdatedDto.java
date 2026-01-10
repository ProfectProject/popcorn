package com.popcorn.demo.domain.popup.dto.owner.response;

import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopupStatusUpdatedDto {

    private UUID popupId;
    private String title;
    private PopupStatus status;
    private Long updatedBy;
    private LocalDateTime updatedAt;

}
