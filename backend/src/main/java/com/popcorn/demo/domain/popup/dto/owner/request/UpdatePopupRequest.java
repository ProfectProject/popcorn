package com.popcorn.demo.domain.popup.dto.owner.request;


import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
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

}
