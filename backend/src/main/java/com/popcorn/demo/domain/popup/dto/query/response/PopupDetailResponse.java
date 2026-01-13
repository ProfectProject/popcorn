package com.popcorn.demo.domain.popup.dto.query.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopupDetailResponse {

	private UUID id;
	private UUID storeId;
	private String title;
	private String description;
	private PopupCategory category;
	private PopupStatus status;
	private LocalDateTime reservationOpenAt;
	private String addressRoad;
	private String addressDetail;
	private LocalDateTime eventStartAt;
	private LocalDateTime eventEndAt;
}
