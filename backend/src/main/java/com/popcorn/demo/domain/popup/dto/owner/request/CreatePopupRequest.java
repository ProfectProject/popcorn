package com.popcorn.demo.domain.popup.dto.owner.request;

import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePopupRequest {

    private UUID storeId;

    private String title;

    private String description;

    private PopupCategory popupCategory;

}
