package com.popcorn.demo.domain.popup.dto.owner.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.entity.enums.PopupStatus;
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
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
