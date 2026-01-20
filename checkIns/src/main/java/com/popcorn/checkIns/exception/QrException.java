package com.popcorn.checkIns.exception;

import com.popcorn.checkIns.dto.QrResponseCode;
import com.popcorn.demo.common.exception.BaseException;

public class QrException extends BaseException {

	private QrException(QrResponseCode responseCode) {
		super(responseCode, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");
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
