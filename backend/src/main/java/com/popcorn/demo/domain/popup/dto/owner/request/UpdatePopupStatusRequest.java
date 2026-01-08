package com.popcorn.demo.domain.popup.dto.owner.request;

import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdatePopupStatusRequest {

    @NotNull(message = "팝업 상태는 필수입니다.")
    private PopupStatus status;

}
