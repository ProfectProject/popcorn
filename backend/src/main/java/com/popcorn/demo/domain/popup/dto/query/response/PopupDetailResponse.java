package com.popcorn.demo.domain.popup.dto.query.response;

import java.time.LocalDateTime;
import java.util.UUID;

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
	private String category;
	private String status;
	private LocalDateTime eventStartAt;
	private LocalDateTime eventEndAt;
}
