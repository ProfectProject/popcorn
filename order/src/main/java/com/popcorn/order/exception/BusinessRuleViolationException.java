package com.popcorn.order.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 발생하는 예외
 *
 * 애플리케이션의 비즈니스 로직 규칙을 위반했을 때 발생합니다.
 * 기술적 오류가 아닌 업무 규칙 위반에 대한 명확한 안내를 제공합니다.
 */
@Getter
public class BusinessRuleViolationException extends RuntimeException {

    private final String ruleName;
    private final String ruleCategory;
    private final Object violatedValue;

    public BusinessRuleViolationException(String ruleName, String message) {
        super(message);
        this.ruleName = ruleName;
        this.ruleCategory = "GENERAL";
        this.violatedValue = null;
    }

    public BusinessRuleViolationException(String ruleName, String ruleCategory, String message) {
        super(message);
        this.ruleName = ruleName;
        this.ruleCategory = ruleCategory;
        this.violatedValue = null;
    }

    public BusinessRuleViolationException(String ruleName, String ruleCategory, Object violatedValue, String message) {
        super(message);
        this.ruleName = ruleName;
        this.ruleCategory = ruleCategory;
        this.violatedValue = violatedValue;
    }

    /**
     * 주문 수량 제한 규칙 위반시 사용하는 팩토리 메서드
     */
    public static BusinessRuleViolationException quantityLimitExceeded(int requestedQty, int maxAllowed) {
        return new BusinessRuleViolationException(
                "QUANTITY_LIMIT",
                "ORDER",
                requestedQty,
                String.format("주문 수량이 제한을 초과했습니다. 요청: %d개, 최대: %d개", requestedQty, maxAllowed)
        );
    }

    /**
     * 주문 금액 제한 규칙 위반시 사용하는 팩토리 메서드
     */
    public static BusinessRuleViolationException amountLimitExceeded(long requestedAmount, long maxAllowed) {
        return new BusinessRuleViolationException(
                "AMOUNT_LIMIT",
                "ORDER",
                requestedAmount,
                String.format("주문 금액이 제한을 초과했습니다. 요청: %,d원, 최대: %,d원", requestedAmount, maxAllowed)
        );
    }

    /**
     * 예약 시간 규칙 위반시 사용하는 팩토리 메서드
     */
    public static BusinessRuleViolationException invalidReservationTime(String timeSlot) {
        return new BusinessRuleViolationException(
                "RESERVATION_TIME",
                "RESERVATION",
                timeSlot,
                "예약 가능한 시간이 아닙니다. 운영 시간을 확인해주세요."
        );
    }

    /**
     * 사용자 등급 제한 규칙 위반시 사용하는 팩토리 메서드
     */
    public static BusinessRuleViolationException userGradeRestriction(String userGrade, String requiredGrade) {
        return new BusinessRuleViolationException(
                "USER_GRADE_RESTRICTION",
                "ACCESS",
                userGrade,
                String.format("접근 권한이 부족합니다. 현재등급: %s, 필요등급: %s", userGrade, requiredGrade)
        );
    }

    /**
     * 중복 주문 방지 규칙 위반시 사용하는 팩토리 메서드
     */
    public static BusinessRuleViolationException duplicateOrderPrevention(String reason) {
        return new BusinessRuleViolationException(
                "DUPLICATE_ORDER_PREVENTION",
                "ORDER",
                null,
                "중복 주문이 감지되었습니다. " + reason
        );
    }

}