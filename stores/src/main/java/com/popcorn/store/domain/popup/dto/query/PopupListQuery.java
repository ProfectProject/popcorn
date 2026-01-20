package com.popcorn.store.domain.popup.dto.query;

import java.util.UUID;

import com.popcorn.store.domain.popup.entity.enums.PopupCategory;
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
	private PopupCategory category;
	private String keyword;
	private UUID storeId;
	private Integer page;
	private Integer size;
	private Boolean withTotal;
}
