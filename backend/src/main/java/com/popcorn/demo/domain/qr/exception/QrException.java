package com.popcorn.demo.domain.qr.exception;

import com.popcorn.demo.domain.qr.dto.QrResponseCode;
import com.popcorn.demo.global.exception.BaseException;

public class QrException extends BaseException {

	private QrException(QrResponseCode responseCode) {
		super(responseCode);
	}

	public static QrException qrNotFound() {
		return new QrException(QrResponseCode.QR_NOT_FOUND);
	}

	public static QrException qrExpired() {
		return new QrException(QrResponseCode.QR_EXPIRED);
	}

	public static QrException orderNotFound() {
		return new QrException(QrResponseCode.ORDER_NOT_FOUND);
	}

	public static QrException orderNotReserved() {
		return new QrException(QrResponseCode.ORDER_NOT_RESERVED);
	}
}
