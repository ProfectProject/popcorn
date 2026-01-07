package com.popcorn.demo.domain.checkin.exception;

import com.popcorn.demo.domain.checkin.dto.CheckinResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class CheckinException extends BaseException {

	private CheckinException(CheckinResponseCode responseCode) {
		super(responseCode);
	}

	public static CheckinException notFound() {
		return new CheckinException(CheckinResponseCode.CHECKIN_NOT_FOUND);
	}
}
