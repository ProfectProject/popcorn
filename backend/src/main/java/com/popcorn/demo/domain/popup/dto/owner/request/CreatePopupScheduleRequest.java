package com.popcorn.demo.domain.popup.dto.owner.request;

import java.time.LocalDateTime;

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
public class CreatePopupScheduleRequest {

	@NotNull(message = "시작 시간은 필수입니다.")
	private LocalDateTime startAt;

	@NotNull(message = "종료 시간은 필수입니다.")
	private LocalDateTime endAt;

	@NotNull(message = "가격은 필수입니다.")
	@Min(value = 0, message = "가격은 0 이상이어야 합니다.")
	private Integer price;

	@NotNull(message = "수용 인원은 필수입니다.")
	@Min(value = 1, message = "수용 인원은 1 이상이어야 합니다.")
	private Integer capacity;
}
