package com.popcorn.demo.domain.popup.dto.owner.response;

import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class PopupCreatedDto {

    private UUID popupId;
    private UUID storeId;
    private String title;
    private String description;
    private PopupCategory popupCategory;
    private PopupStatus status;
    private LocalDateTime createdAt;
    private Long createdBy;

}
