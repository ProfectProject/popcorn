package com.popcorn.store.domain.popup.dto.owner.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.popcorn.store.domain.popup.entity.enums.PopupCategory;
import com.popcorn.store.domain.popup.entity.enums.PopupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopupDetailDto {

	private UUID popupId;
	private UUID storeId;
	private String title;
	private String description;
	private PopupCategory popupCategory;
	private PopupStatus status;
	private LocalDateTime reservationOpenAt;
	private String addressRoad;
	private String addressDetail;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private List<PopupScheduleDetailDto> schedules;
}
