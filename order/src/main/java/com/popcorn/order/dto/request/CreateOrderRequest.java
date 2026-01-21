package com.popcorn.order.dto.request;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 생성 요청 DTO
 *
 * [초보자 가이드]
 * 사용자가 주문을 생성할 때 필요한 모든 정보를 담는 클래스입니다.
 * 프론트엔드에서 이 형태로 데이터를 보내면, 백엔드에서 주문을 생성합니다.
 *
 * 검증 어노테이션:
 * @Valid: 중첩된 객체도 검증 실행
 * @NotEmpty: 리스트가 비어있지 않아야 함
 * @NotNull: null이 아니어야 함
 * @NotBlank: 공백이 아닌 문자가 있어야 함
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    /** 사용자 ID - 주문하는 사용자 */
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    /** 주문 타입 - "RESERVATION", "GOODS", "MIXED" */
    @NotBlank(message = "주문 타입은 필수입니다.")
    @Schema(description = "주문 타입", allowableValues = {"RESERVATION", "GOODS", "MIXED"}, example = "RESERVATION")
    private String orderType;

    /** 팝업 ID - 어떤 팝업에서 주문하는지 */
    @NotNull(message = "팝업 ID는 필수입니다.")
    private UUID popupId;

    /** 예약 ID - 예약형 주문의 경우 예약 정보 */
    private UUID reservationId;

    /** 결제 방식 - "CARD", "CASH" 등 */
    @Schema(description = "결제 방식", allowableValues = {"CARD", "TRANSFER", "MOBILE_PHONE", "VIRTUAL_ACCOUNT", "CASH"}, example = "CARD")
    private String paymentMethod;

    /** 주문 항목 목록 - 주문할 상품들의 리스트 */
    @NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다.")
    @Valid
    private List<OrderItemRequest> items;

    /** 주소는 user 모듈에서 userId로 조회 */

    // ========================= 검증 메서드 =========================

    /**
     * 예약형 주문인지 확인
     * @return 예약 주문이면 true
     */
    public boolean isReservationType() {
        return "RESERVATION".equals(orderType);
    }

    /**
     * 구매형 주문인지 확인
     * @return 구매 주문이면 true
     */
    public boolean isGoodsType() {
        return "GOODS".equals(orderType);
    }

    /**
     * 혼합형 주문인지 확인
     * @return 혼합 주문이면 true
     */
    public boolean isMixedType() {
        return "MIXED".equals(orderType);
    }

    /**
     * 주문 항목들의 타입이 일관성 있는지 확인
     * @return 타입이 일관성 있으면 true
     *
     * [초보자 가이드]
     * - 예약형 주문: 모든 항목이 "RESERVATION"
     * - 굿즈형 주문: 모든 항목이 "GOODS"
     * - 혼합형 주문: "RESERVATION"과 "GOODS" 항목이 모두 존재
     */
    public boolean hasConsistentItemTypes() {
        if (items == null || items.isEmpty()) {
            return false;
        }

        if (isReservationType()) {
            return items.stream().allMatch(item -> "RESERVATION".equals(item.getOrderItemType()));
        }

        if (isGoodsType()) {
            return items.stream().allMatch(item -> "GOODS".equals(item.getOrderItemType()));
        }

        if (isMixedType()) {
            boolean hasReservation = items.stream().anyMatch(item -> "RESERVATION".equals(item.getOrderItemType()));
            boolean hasGoods = items.stream().anyMatch(item -> "GOODS".equals(item.getOrderItemType()));
            return hasReservation && hasGoods;
        }

        return false;
    }

    /**
     * 유효한 예약형 요청인지 확인
     * @return 예약 주문 조건을 만족하면 true
     *
     * [초보자 가이드]
     * 예약형 주문은 모든 항목이 예약 타입이고 sessionId가 있어야 함
     */
    public boolean isValidReservationRequest() {
        if (!isReservationType()) {
            return false;
        }

        return items.stream()
                .allMatch(item ->
                        "RESERVATION".equals(item.getOrderItemType())
                                && item.getSessionId() != null
                );
    }

    /**
     * 유효한 구매형 요청인지 확인
     * @return 구매 주문 조건을 만족하면 true
     *
     * [초보자 가이드]
     * 구매형 주문은 모든 항목이 굿즈 타입이고 goodsVariantId가 있어야 함
     * 배송 주소는 user 모듈에서 userId로 조회하며, 주소가 없으면 서비스 레이어에서 에러 발생
     */
    public boolean isValidGoodsRequest() {
        if (!isGoodsType()) {
            return false;
        }

        // 굿즈 구매 시 모든 항목이 유효한지 확인
        return items.stream()
                .allMatch(item ->
                        "GOODS".equals(item.getOrderItemType())
                                && item.getGoodsVariantId() != null
                );
    }

    /**
     * 유효한 혼합형 요청인지 확인
     * @return 혼합 주문 조건을 만족하면 true
     *
     * [초보자 가이드]
     * 혼합형 주문은 예약과 굿즈 항목이 모두 있고, 각각 유효한 필드를 가져야 함
     */
    public boolean isValidMixedRequest() {
        if (!isMixedType()) {
            return false;
        }

        // 예약 항목과 굿즈 항목이 모두 있는지 확인
        if (!hasConsistentItemTypes()) {
            return false;
        }

        // 각 항목이 타입에 맞는 유효한 필드를 가지고 있는지 확인
        return items.stream().allMatch(item -> {
            if ("RESERVATION".equals(item.getOrderItemType())) {
                return item.isValidReservationItem();
            } else if ("GOODS".equals(item.getOrderItemType())) {
                return item.isValidGoodsItem();
            }
            return false;
        });
    }

    /**
     * 배송 주소가 필요한 주문인지 확인
     * @return 굿즈 항목이 포함되어 있으면 true (배송 주소 필요)
     *
     * [비즈니스 로직]
     * 굿즈 항목이 포함된 모든 주문은 반드시 배송 주소가 필요합니다.
     * 예약과 굿즈가 같이 있는 주문에서도 굿즈가 있으면 주소가 필요합니다.
     * 서비스 레이어에서 이 메서드를 사용하여 user 모듈에서 주소를 조회하고,
     * 주소가 없으면 에러를 발생시켜야 합니다.
     */
    public boolean requiresShippingAddress() {
        return items != null && items.stream()
                .anyMatch(item -> "GOODS".equals(item.getOrderItemType()));
    }

    /**
     * 전체 주문 수량 계산
     * @return 모든 항목의 수량 합계
     *
     * [초보자 가이드]
     * mapToInt: 각 OrderItemRequest에서 수량(int)을 추출
     * sum: 모든 수량을 합계
     * 메서드 레퍼런스(::) 사용: item -> item.getQty()와 동일
     */
    public int getTotalQuantity() {
        return items.stream()
                .mapToInt(OrderItemRequest::getQty)
                .sum();
    }

}