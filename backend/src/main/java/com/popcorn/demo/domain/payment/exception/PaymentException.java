package com.popcorn.demo.domain.payment.exception;

import com.popcorn.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.common.exception.BaseException;

public class PaymentException extends BaseException {

	private PaymentException(CommonResponseCode responseCode, String customMessage) {
		super(responseCode, customMessage);
	}

	private PaymentException(OrderResponseCode responseCode, String customMessage) {
		super(responseCode, customMessage);
	}

	// 기본 생성자들 (기본 메시지 사용)
	private PaymentException(CommonResponseCode responseCode) {
		super(responseCode, responseCode.getMessage());
	}

	private PaymentException(OrderResponseCode responseCode) {
		super(responseCode, responseCode.getMessage());
	}

	public static PaymentException invalidRequest() {
		return new PaymentException(CommonResponseCode.INVALID_REQUEST, "💳 결제 요청이 유효하지 않습니다.");
	}

	public static PaymentException paymentAlreadyExists() {
		return new PaymentException(OrderResponseCode.PAYMENT_ALREADY_EXISTS,
			"""
			🛡️ 이미 결제가 완료된 주문입니다.

			✅ 중복 결제가 방지되었습니다.
			💡 결제 내역은 '내 주문'에서 확인하실 수 있습니다.
			""");
	}

	public static PaymentException paymentNotFound() {
		return new PaymentException(CommonResponseCode.NOT_FOUND, "💳 결제 정보를 찾을 수 없습니다.");
	}

	public static PaymentException cancellationTimeExpired() {
		return new PaymentException(OrderResponseCode.PAYMENT_CANCELLATION_TIME_EXPIRED);
	}

	// 🛡️ 새로운 멱등성 관련 예외
	public static PaymentException duplicatePaymentAttempt() {
		return new PaymentException(OrderResponseCode.PAYMENT_ALREADY_EXISTS,
			"""
			🛡️ 동일한 주문에 대한 중복 결제 시도가 차단되었습니다.

			✅ 시스템이 자동으로 중복 결제를 방지했습니다.
			🔍 이미 결제가 진행 중이거나 완료되었습니다.
			💡 잠시 후 새로고침하여 결제 상태를 확인해주세요.
			""");
	}
}
