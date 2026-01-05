package com.popcorn.demo.domain.popup.exception;

import com.popcorn.demo.domain.popup.dto.PopupResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class PopupException extends BaseException {

	public PopupException(PopupResponseCode responseCode) {
		super(responseCode);
	}

	public static PopupException popupNotFound() {
		return new PopupException(PopupResponseCode.POPUP_NOT_FOUND);
	}
}
