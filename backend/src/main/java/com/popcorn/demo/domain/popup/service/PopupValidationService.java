package com.popcorn.demo.domain.popup.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.popup.dto.PopupResponseCode;
import com.popcorn.demo.domain.popup.dto.query.PopupDetailQuery;
import com.popcorn.demo.domain.popup.dto.query.PopupListQuery;
import com.popcorn.demo.domain.popup.exception.PopupException;

@Service
public class PopupValidationService {

	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;
	private static final Set<String> ALLOWED_CATEGORIES = Set.of("POPUP", "MERCH", "EVENT");

	public PopupListQuery normalizeListQuery(PopupListQuery query) {
		if (query == null) {
			return PopupListQuery.builder()
					.page(DEFAULT_PAGE)
					.size(DEFAULT_SIZE)
					.build();
		}

		Integer page = query.getPage();
		Integer size = query.getSize();
		Long regionId = query.getRegionId();
		String category = normalizeCategory(query.getCategory());

		int normalizedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
		int normalizedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

		if (regionId != null && regionId <= 0) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}

		return PopupListQuery.builder()
				.regionId(regionId)
				.category(category)
				.keyword(query.getKeyword())
				.storeId(query.getStoreId())
				.page(normalizedPage)
				.size(normalizedSize)
				.build();
	}

	public void validateDetailQuery(PopupDetailQuery query) {
		if (query == null || query.getProductId() == null) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
	}

	private String normalizeCategory(String category) {
		if (category == null || category.isBlank()) {
			return null;
		}
		String normalized = category.trim().toUpperCase(Locale.ROOT);
		if (!ALLOWED_CATEGORIES.contains(normalized)) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
		return normalized;
	}
}
