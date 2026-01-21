package com.popcorn.store.domain.popup.dto.owner.request;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePopupScheduleRequest {

	@NotNull(message = "스케줄 ID는 필수입니다.")
	private UUID scheduleId;

	private LocalDateTime startAt;
	private LocalDateTime endAt;

	@Min(value = 0, message = "가격은 0 이상이어야 합니다.")
	private Integer price;

	@Min(value = 1, message = "수용 인원은 1 이상이어야 합니다.")
	private Integer capacity;

	private Boolean active;
}
