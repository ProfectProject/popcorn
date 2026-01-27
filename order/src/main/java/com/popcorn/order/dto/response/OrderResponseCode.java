package com.popcorn.order.dto.response;

import com.popcorn.common.dto.ResponseCode;

/**
 * 주문 도메인 응답 코드 정의
 *
 * common-lib의 ResponseCode 인터페이스를 구현하여
 * 표준화된 응답 코드를 제공합니다.
 *
 * 코드 체계:
 * - 1xxx: 일반적인 성공
 * - 2xxx: 주문 관련 성공
 * - 4xxx: 클라이언트 오류
 * - 5xxx: 서버 오류
 */
public enum OrderResponseCode implements ResponseCode {

    // ================ 성공 응답 ================

    /** 주문 생성 성공 */
    ORDER_CREATED(2001, 201, "주문이 성공적으로 생성되었습니다."),

    /** 주문 조회 성공 */
    ORDER_RETRIEVED(2002, 200, "주문 정보를 성공적으로 조회했습니다."),

    /** 주문 목록 조회 성공 */
    ORDER_LIST_RETRIEVED(2003, 200, "주문 목록을 성공적으로 조회했습니다."),

    /** 주문 상태 변경 성공 */
    ORDER_STATUS_UPDATED(2004, 200, "주문 상태가 성공적으로 변경되었습니다."),

    /** 주문 취소 성공 */
    ORDER_CANCELLED(2005, 200, "주문이 성공적으로 취소되었습니다."),

    /** 주문 이벤트 조회 성공 */
    ORDER_EVENTS_RETRIEVED(2006, 200, "주문 이벤트를 성공적으로 조회했습니다."),

    /** 멱등성 키 생성 성공 */
    IDEMPOTENCY_KEY_GENERATED(2007, 200, "멱등성 키가 성공적으로 생성되었습니다."),

    /** 이벤트 재생 시작 */
    EVENT_REPLAY_STARTED(2008, 200, "이벤트 재생이 시작되었습니다."),

    // ================ 클라이언트 오류 ================

    /** 주문을 찾을 수 없음 */
    ORDER_NOT_FOUND(4001, 404, "요청한 주문을 찾을 수 없습니다."),

    /** 잘못된 주문 요청 */
    INVALID_ORDER_REQUEST(4002, 400, "주문 요청 데이터가 올바르지 않습니다."),

    /** 취소 불가능한 주문 */
    ORDER_NOT_CANCELLABLE(4003, 400, "취소할 수 없는 주문입니다."),

    /** 이미 취소된 주문 */
    ORDER_ALREADY_CANCELLED(4004, 400, "이미 취소된 주문입니다."),

    /** 주문 상태 변경 불가 */
    ORDER_STATUS_CHANGE_NOT_ALLOWED(4005, 400, "현재 상태에서는 변경할 수 없습니다."),

    /** 권한 없음 */
    ORDER_ACCESS_DENIED(4006, 403, "주문에 접근할 권한이 없습니다."),

    /** 주문 번호 중복 */
    ORDER_NO_ALREADY_EXISTS(4007, 409, "이미 존재하는 주문 번호입니다."),

    /** 재고 부족 */
    INSUFFICIENT_STOCK(4008, 400, "재고가 부족합니다."),

    /** 팝업 운영 시간 외 */
    POPUP_NOT_OPERATING(4009, 400, "팝업 운영 시간이 아닙니다."),

    /** 잘못된 멱등성 키 */
    INVALID_IDEMPOTENCY_KEY(4010, 400, "잘못된 멱등성 키입니다."),

    /** 비즈니스 규칙 위반 */
    BUSINESS_RULE_VIOLATION(4011, 400, "비즈니스 규칙을 위반했습니다."),

    /** 동일 요청 처리 중 */
    IDEMPOTENCY_REQUEST_IN_PROGRESS(4012, 409, "이미 처리 중인 동일 요청이 있습니다."),

    // ================ 서버 오류 ================

    /** 주문 생성 실패 */
    ORDER_CREATION_FAILED(5001, 500, "주문 생성 중 서버 오류가 발생했습니다."),

    /** 주문 상태 변경 실패 */
    ORDER_STATUS_UPDATE_FAILED(5002, 500, "주문 상태 변경 중 오류가 발생했습니다."),

    /** 결제 서비스 오류 */
    PAYMENT_SERVICE_ERROR(5003, 502, "결제 서비스에 문제가 발생했습니다."),

    /** 재고 서비스 오류 */
    INVENTORY_SERVICE_ERROR(5004, 502, "재고 서비스에 문제가 발생했습니다."),

    /** 알림 서비스 오류 */
    NOTIFICATION_SERVICE_ERROR(5005, 502, "알림 서비스에 문제가 발생했습니다."),

    /** 데이터베이스 오류 */
    DATABASE_ERROR(5006, 500, "데이터베이스 오류가 발생했습니다."),

    /** 결제 처리 실패 */
    PAYMENT_PROCESSING_FAILED(5007, 500, "결제 처리 중 오류가 발생했습니다."),

    /** 외부 서비스 오류 */
    EXTERNAL_SERVICE_ERROR(5008, 502, "외부 서비스 연동 중 오류가 발생했습니다."),

    /** 내부 서버 오류 */
    INTERNAL_SERVER_ERROR(5009, 500, "내부 서버 오류가 발생했습니다."),

    /** 멱등성 키 생성 실패 */
    IDEMPOTENCY_KEY_GENERATION_FAILED(5010, 500, "멱등성 키 생성에 실패했습니다.");

    private final int code;
    private final int httpStatus;
    private final String message;

    OrderResponseCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }

    // ================ 편의 메서드 ================

    /**
     * HTTP 상태 코드가 성공인지 확인
     */
    public boolean isSuccess() {
        return httpStatus >= 200 && httpStatus < 300;
    }

    /**
     * 클라이언트 오류인지 확인
     */
    public boolean isClientError() {
        return httpStatus >= 400 && httpStatus < 500;
    }

    /**
     * 서버 오류인지 확인
     */
    public boolean isServerError() {
        return httpStatus >= 500 && httpStatus < 600;
    }

    /**
     * 응답 코드에 따른 설명 반환
     */
    public String getDescription() {
        return String.format("[%d] %s (HTTP %d)", code, message, httpStatus);
    }
}
