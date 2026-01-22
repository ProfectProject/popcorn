package com.popcorn.order.dto.audit;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비즈니스 이벤트 로그 DTO
 *
 * 주문, 결제, 배송 등 중요한 비즈니스 프로세스의 상태 변화를 기록합니다.
 * 감사 추적, 문제 해결, 비즈니스 분석에 활용됩니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessEventLog {

    /** 이벤트 고유 ID */
    private String eventId;

    /** 이벤트 타입 */
    private String eventType;

    /** 이벤트 카테고리 (ORDER, PAYMENT, SHIPPING 등) */
    private String category;

    /** 이벤트 발생 시간 */
    private LocalDateTime eventTime;

    /** 관련된 주문 ID */
    private UUID orderId;

    /** 주문 번호 (사용자에게 표시되는) */
    private String orderNo;

    /** 이벤트 발생시킨 사용자 ID */
    private Long userId;

    /** 사용자 이름 */
    private String userName;

    /** 이벤트 발생시킨 시스템/서비스 */
    private String sourceSystem;

    /** 이벤트 제목 */
    private String title;

    /** 이벤트 상세 설명 */
    private String description;

    /** 이벤트 이전 상태 */
    private String previousState;

    /** 이벤트 이후 상태 */
    private String newState;

    /** 상태 변경 사유 */
    private String reason;

    /** 이벤트 심각도 (INFO, WARN, ERROR, CRITICAL) */
    private String severity;

    /** 영향받은 데이터 (JSON 형태) */
    private Map<String, Object> affectedData;

    /** 추가 메타데이터 */
    private Map<String, Object> metadata;

    /** 클라이언트 IP 주소 */
    private String clientIp;

    /** 관련 API 요청 ID (추적용) */
    private String requestId;

    /** 처리 결과 (SUCCESS, FAILURE, PARTIAL) */
    private String result;

    /** 오류 메시지 (실패시) */
    private String errorMessage;

    // ========================= 편의 메서드 =========================

    /**
     * 주문 관련 이벤트인지 확인
     * @return 주문 이벤트면 true
     */
    public boolean isOrderEvent() {
        return "ORDER".equals(category);
    }

    /**
     * 결제 관련 이벤트인지 확인
     * @return 결제 이벤트면 true
     */
    public boolean isPaymentEvent() {
        return "PAYMENT".equals(category);
    }

    /**
     * 성공한 이벤트인지 확인
     * @return 성공 이벤트면 true
     */
    public boolean isSuccessful() {
        return "SUCCESS".equals(result);
    }

    /**
     * 실패한 이벤트인지 확인
     * @return 실패 이벤트면 true
     */
    public boolean isFailed() {
        return "FAILURE".equals(result);
    }

    /**
     * 중요한 이벤트인지 확인 (ERROR, CRITICAL 레벨)
     * @return 중요 이벤트면 true
     */
    public boolean isCritical() {
        return "ERROR".equals(severity) || "CRITICAL".equals(severity);
    }

    /**
     * 사용자가 직접 실행한 이벤트인지 확인
     * @return 사용자 실행 이벤트면 true
     */
    public boolean isUserInitiated() {
        return userId != null && !"SYSTEM".equals(sourceSystem);
    }

    /**
     * 시스템에서 자동으로 실행한 이벤트인지 확인
     * @return 시스템 자동 실행 이벤트면 true
     */
    public boolean isSystemInitiated() {
        return "SYSTEM".equals(sourceSystem);
    }

    /**
     * 상태가 변경된 이벤트인지 확인
     * @return 상태 변경 이벤트면 true
     */
    public boolean isStateChangeEvent() {
        return previousState != null && newState != null && !previousState.equals(newState);
    }

    /**
     * 이벤트 요약 정보 반환
     * @return 한 줄로 요약된 이벤트 정보
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("[").append(category).append("] ");
        summary.append(title != null ? title : eventType);

        if (isStateChangeEvent()) {
            summary.append(" (").append(previousState).append(" → ").append(newState).append(")");
        }

        if (userName != null) {
            summary.append(" by ").append(userName);
        } else if (userId != null) {
            summary.append(" by User#").append(userId);
        } else {
            summary.append(" by System");
        }

        return summary.toString();
    }

    /**
     * 이벤트 타입별 한글 설명 반환
     * @return 한글 이벤트 설명
     */
    public String getEventTypeDescription() {
        return switch (eventType) {
            case "ORDER_CREATED" -> "주문 생성";
            case "ORDER_UPDATED" -> "주문 정보 수정";
            case "ORDER_STATUS_CHANGED" -> "주문 상태 변경";
            case "ORDER_CANCELLED" -> "주문 취소";
            case "PAYMENT_STARTED" -> "결제 시작";
            case "PAYMENT_COMPLETED" -> "결제 완료";
            case "PAYMENT_FAILED" -> "결제 실패";
            case "PAYMENT_CANCELLED" -> "결제 취소";
            case "REFUND_REQUESTED" -> "환불 요청";
            case "REFUND_COMPLETED" -> "환불 완료";
            case "SHIPPING_STARTED" -> "배송 시작";
            case "SHIPPING_COMPLETED" -> "배송 완료";
            case "INVENTORY_UPDATED" -> "재고 업데이트";
            case "USER_REGISTRATION" -> "회원 가입";
            case "USER_LOGIN" -> "로그인";
            case "USER_LOGOUT" -> "로그아웃";
            default -> eventType;
        };
    }

    /**
     * 심각도별 색상 클래스 반환 (UI 표시용)
     * @return CSS 색상 클래스명
     */
    public String getSeverityColorClass() {
        return switch (severity) {
            case "INFO" -> "info";
            case "WARN" -> "warning";
            case "ERROR" -> "danger";
            case "CRITICAL" -> "critical";
            default -> "secondary";
        };
    }

    // ========================= 팩토리 메서드 =========================

    /**
     * 주문 생성 이벤트 생성
     */
    public static BusinessEventLog orderCreated(UUID orderId, String orderNo, Long userId, String userName) {
        return BusinessEventLog.builder()
                .eventId("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .eventType("ORDER_CREATED")
                .category("ORDER")
                .eventTime(LocalDateTime.now())
                .orderId(orderId)
                .orderNo(orderNo)
                .userId(userId)
                .userName(userName)
                .title("새 주문이 생성되었습니다")
                .description("주문번호 " + orderNo + "가 생성되었습니다")
                .newState("CREATED")
                .severity("INFO")
                .result("SUCCESS")
                .build();
    }

    /**
     * 주문 상태 변경 이벤트 생성
     */
    public static BusinessEventLog orderStatusChanged(UUID orderId, String orderNo, String fromStatus,
                                                     String toStatus, String reason, Long userId) {
        return BusinessEventLog.builder()
                .eventId("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .eventType("ORDER_STATUS_CHANGED")
                .category("ORDER")
                .eventTime(LocalDateTime.now())
                .orderId(orderId)
                .orderNo(orderNo)
                .userId(userId)
                .title("주문 상태가 변경되었습니다")
                .description("주문 상태가 " + fromStatus + "에서 " + toStatus + "로 변경되었습니다")
                .previousState(fromStatus)
                .newState(toStatus)
                .reason(reason)
                .severity("INFO")
                .result("SUCCESS")
                .build();
    }

    /**
     * 결제 완료 이벤트 생성
     */
    public static BusinessEventLog paymentCompleted(UUID orderId, String orderNo, Long amount,
                                                   String paymentMethod, Long userId) {
        return BusinessEventLog.builder()
                .eventId("EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .eventType("PAYMENT_COMPLETED")
                .category("PAYMENT")
                .eventTime(LocalDateTime.now())
                .orderId(orderId)
                .orderNo(orderNo)
                .userId(userId)
                .title("결제가 완료되었습니다")
                .description(String.format("%s으로 %,d원 결제가 완료되었습니다", paymentMethod, amount))
                .newState("PAID")
                .severity("INFO")
                .result("SUCCESS")
                .affectedData(Map.of("amount", amount, "paymentMethod", paymentMethod))
                .build();
    }

}