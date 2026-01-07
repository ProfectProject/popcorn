package com.popcorn.demo.domain.popup.service.owner;

import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.popup.dto.PopupResponseCode;
import com.popcorn.demo.domain.popup.dto.owner.request.CreatePopupRequest;
import com.popcorn.demo.domain.popup.exception.PopupException;

@Service
public class OwnerPopupValidationService {

	private static final int MIN_TITLE_LENGTH = 1;
	private static final int MAX_TITLE_LENGTH = 200;
	private static final String INVALID_CHARS = "<>\"'&;";

	public String validateAndTrimTitle(String title) {
		if (title == null || title.trim().isEmpty()) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}

		String trimmed = title.trim();
		if (trimmed.length() < MIN_TITLE_LENGTH || trimmed.length() > MAX_TITLE_LENGTH) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}

		if (trimmed.chars().anyMatch(c -> INVALID_CHARS.indexOf(c) >= 0)) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}

		return trimmed;
	}

	public String validateCreateRequest(CreatePopupRequest request) {
		if (request == null) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
		if (request.getStoreId() == null || request.getCategory() == null) {
			throw new PopupException(PopupResponseCode.INVALID_REQUEST);
		}
		return validateAndTrimTitle(request.getTitle());
	}
}
