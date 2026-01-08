package com.popcorn.demo.domain.popup.dto.owner.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopupScheduleDetailDto {

	private UUID scheduleId;
	private LocalDateTime startAt;
	private LocalDateTime endAt;
	private Integer price;
	private Integer capacity;
	private Integer remainingCapacity;
	private boolean active;
}
