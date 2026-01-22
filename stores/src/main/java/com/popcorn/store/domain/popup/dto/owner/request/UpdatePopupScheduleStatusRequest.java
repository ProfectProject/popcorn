package com.popcorn.store.domain.popup.dto.owner.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePopupScheduleStatusRequest {

	@NotNull(message = "스케줄 ID는 필수입니다.")
	private UUID scheduleId;

	@NotNull(message = "활성 여부는 필수입니다.")
	private Boolean active;
}
