package com.popcorn.order.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 항목 요청 DTO
 *
 * [초보자 가이드]
 * 주문에 포함될 각각의 상품 정보를 담는 클래스입니다.
 * 예: 팝콘 3개, 티셔츠 2개 → 각각이 하나의 OrderItemRequest
 *
 * 검증 어노테이션:
 * @NotBlank: 공백이 아닌 값이 필수
 * @NotNull: null이 아닌 값이 필수
 * @Min: 최솟값 지정
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {

    /** 주문 항목 타입 - "RESERVATION" 또는 "GOODS" */
    @NotBlank(message = "주문 항목 타입은 필수입니다.")
    @Schema(description = "주문 항목 타입", allowableValues = {"RESERVATION", "GOODS"}, example = "RESERVATION")
    private String orderItemType;

    /** 수량 - 주문하는 개수 */
    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private Integer qty;

    /** 단가 - 개당 가격 (원) */
    private Integer unitPrice;

    /** 세션 ID - 예약형 상품의 경우 시간 슬롯 */
    private UUID sessionId;

    /** 옵션 ID - 세션의 추가 옵션 */
    private UUID optionId;

    /** 굿즈 변형 ID - 구매형 상품의 경우 (색상, 사이즈 등) */
    private UUID goodsVariantId;

    // ========================= 검증 메서드 =========================

    /**
     * 예약형 항목인지 확인
     * @return 예약 항목이면 true
     */
    public boolean isReservationType() {
        return "RESERVATION".equals(orderItemType);
    }

    /**
     * 굿즈형 항목인지 확인
     * @return 굿즈 항목이면 true
     */
    public boolean isGoodsType() {
        return "GOODS".equals(orderItemType);
    }

    /**
     * 유효한 예약 항목인지 검증
     * @return 예약 항목 조건을 만족하면 true
     *
     * [초보자 가이드]
     * 예약 항목은 sessionId가 반드시 필요하고 수량이 1 이상이어야 함
     */
    public boolean isValidReservationItem() {
        return isReservationType()
                && sessionId != null
                && qty != null
                && qty > 0;
    }

    /**
     * 유효한 굿즈 항목인지 검증
     * @return 굿즈 항목 조건을 만족하면 true
     *
     * [초보자 가이드]
     * 굿즈 항목은 goodsVariantId가 반드시 필요하고 수량이 1 이상이어야 함
     */
    public boolean isValidGoodsItem() {
        return isGoodsType()
                && goodsVariantId != null
                && qty != null
                && qty > 0;
    }

    /**
     * 항목 타입에 따른 필수 필드 보유 여부 확인
     * @return 필수 필드가 모두 있으면 true
     */
    public boolean hasRequiredFields() {
        if (isReservationType()) {
            return isValidReservationItem();
        }
        if (isGoodsType()) {
            return isValidGoodsItem();
        }
        return false;
    }

    /**
     * 항목 타입에 맞지 않는 불필요한 필드가 있는지 확인
     * @return 불필요한 필드가 있으면 true (잘못된 요청)
     *
     * [초보자 가이드]
     * 예약 항목인데 goodsVariantId가 있거나,
     * 굿즈 항목인데 sessionId가 있으면 잘못된 요청
     */
    public boolean hasUnnecessaryFields() {
        if (isReservationType()) {
            return goodsVariantId != null;
        }
        if (isGoodsType()) {
            return sessionId != null || optionId != null;
        }
        return false;
    }

    // ========================= 편의 메서드 =========================

    /**
     * 세션 옵션 키 반환
     * @return 예약형인 경우 sessionId 문자열, 아니면 null
     */
    public String getSessionOptionKey() {
        if (isReservationType() && sessionId != null) {
            return sessionId.toString();
        }
        return null;
    }

    /**
     * 재고 식별자 반환
     * @return 재고 관리를 위한 고유 식별자
     *
     * [초보자 가이드]
     * 예약형: 세션 ID로 재고 관리
     * 굿즈형: 상품 변형 ID로 재고 관리
     */
    public String getStockIdentifier() {
        if (isReservationType()) {
            return getSessionOptionKey();
        }
        if (isGoodsType()) {
            return "VARIANT_" + goodsVariantId;
        }
        return null;
    }

    /**
     * 화면 표시용 설명 문자열 생성
     * @return 사용자에게 보여질 항목 설명
     *
     * [초보자 가이드]
     * String.format(): 문자열 템플릿을 사용한 포맷팅
     * %s: 문자열 치환, %d: 숫자 치환
     */
    public String getDisplayDescription() {
        if (isReservationType()) {
            return String.format("예약 (스케줄: %s) x %d개", sessionId, qty);
        }
        if (isGoodsType()) {
            return String.format("굿즈 (변형: %s) x %d개", goodsVariantId, qty);
        }
        return String.format("항목 (%s) x %d개", orderItemType, qty);
    }

}