package com.popcorn.store.domain.popup.exception;

import com.popcorn.store.domain.popup.dto.PopupResponseCode;
import com.popcorn.common.exception.BaseException;

public class PopupException extends BaseException {

	public PopupException(PopupResponseCode responseCode) {
		super(responseCode, "🎁 팝업 굿즈 배송을 위해 배송지를 먼저 등록해주세요!\n\n" + "📍 '내 정보 > 배송지 관리'에서 배송지를 등록한 후 다시 주문해 주세요.\n" + "💡 기본 배송지로 설정하면 다음 주문부터 자동으로 적용됩니다.");
	}

	public static PopupException popupNotFound() {
		return new PopupException(PopupResponseCode.POPUP_NOT_FOUND);
	}

	public static PopupException isNullQuantity(){
		return new PopupException(PopupResponseCode.Quantity_is_Null);
	}

	public static PopupException isNotPositiveQuantity(){
		return new PopupException(PopupResponseCode.Positive_Quantity);
	}

	public static PopupException insufficientReservationCapacity() {
		return new PopupException(PopupResponseCode.INSUFFICIENT_RESERVATION_CAPACITY);
	}
}
