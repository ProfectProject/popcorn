package com.popcorn.demo.domain.popup.service;

import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.popup.dto.PopupResponseCode;
import com.popcorn.demo.domain.popup.dto.query.PopupDetailQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupListQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupScheduleListQuery;
import com.popcorn.demo.domain.popup.entity.enums.PopupCategory;
import com.popcorn.demo.domain.popup.exception.PopupException;

@Service
public class PopupValidationService {

	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;
	public PopupListQuery normalizeListQuery(PopupListQuery query) {
		if (query == null) {
			return PopupListQuery.builder()
					.page(DEFAULT_PAGE)
					.size(DEFAULT_SIZE)
					.withTotal(true)
					.build();
		}

		Integer page = query.getPage();
		Integer size = query.getSize();
		Long regionId = query.getRegionId();
		PopupCategory category = query.getCategory();
		Boolean withTotal = query.getWithTotal();

		int normalizedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
		int normalizedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
		boolean normalizedWithTotal = withTotal == null || withTotal;

		if (regionId != null && regionId <= 0) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
		validateCategory(category);

		return PopupListQuery.builder()
				.regionId(regionId)
				.category(category)
				.keyword(query.getKeyword())
				.storeId(query.getStoreId())
				.page(normalizedPage)
				.size(normalizedSize)
				.withTotal(normalizedWithTotal)
				.build();
	}

	public void validateDetailQuery(PopupDetailQuery query) {
		if (query == null || query.getPopupId() == null) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
	}

	public PopupScheduleListQuery normalizeSessionQuery(PopupScheduleListQuery query) {
		if (query == null || query.getPopupId() == null) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
		if (query.getFrom() != null && query.getTo() != null
				&& query.getFrom().isAfter(query.getTo())) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
		return query;
	}

	private void validateCategory(PopupCategory category) {
		if (category == null) {
			return;
		}
		try {
			PopupCategory.valueOf(category.name());
		} catch (IllegalArgumentException ex) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
	}
}
