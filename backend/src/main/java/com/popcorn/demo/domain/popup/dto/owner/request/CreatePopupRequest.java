package com.popcorn.demo.domain.popup.dto.owner.request;

import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePopupRequest {

    @NotNull(message = "스토어 ID는 필수입니다.")
    private UUID storeId;

    @NotBlank(message = "팝업 제목은 필수입니다.")
    @Size(min = 1, max = 200, message = "팝업 제목은 1-200자 사이여야 합니다.")
    private String title;

    private String description;

    @NotNull(message = "팝업 카테고리는 필수입니다.")
    private PopupCategory category;

    @NotEmpty(message = "팝업 스케줄은 최소 1개 이상 필요합니다.")
    @Valid
    private List<CreatePopupScheduleRequest> schedules;

}
