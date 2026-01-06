package com.popcorn.demo.domain.popup.dto.query;

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
public class PopupSessionListQuery {

	private UUID popupId;
	private LocalDateTime from;
	private LocalDateTime to;
}
