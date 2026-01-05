package com.popcorn.demo.domain.popup.dto.query;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopupListQuery {

	private Long regionId;
	private String category;
	private String keyword;
	private UUID storeId;
	private Integer page;
	private Integer size;
	private Boolean withTotal;
}
