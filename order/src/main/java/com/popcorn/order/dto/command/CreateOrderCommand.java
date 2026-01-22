package com.popcorn.order.dto.command;

import java.util.List;
import java.util.UUID;

import com.popcorn.order.dto.request.CreateOrderRequest;
import com.popcorn.order.dto.request.OrderItemRequest;
import com.popcorn.order.entity.OrderItemType;

import lombok.Builder;
import lombok.Getter;

/**
 * 주문 생성 명령 DTO
 * - 서비스 계층으로 전달되는 입력 값
 * - 불변 객체로 사용합니다.
 *
 * Command 패턴: 요청을 객체로 캡슐화하는 패턴
 * DTO vs Command의 차이:
 * - DTO: 단순히 데이터를 전달하는 용도
 * - Command: 특정 작업을 수행하라는 명령을 나타냄
 *
 * 이 클래스는 "주문을 생성하라"는 명령과 그에 필요한 모든 정보를 담고 있습니다.
 */
@Getter
@Builder
public class CreateOrderCommand {

    /** 주문을 생성하는 사용자 ID */
    private final Long userId;

    /** 관련된 팝업 ID */
    private final UUID popupId;

    /** 주문 타입 - "RESERVATION" 또는 "PURCHASE" */
    private final String orderType;

    /** 주문 항목들 - 실제로 주문할 상품 목록 */
    private final List<OrderItemCommand> items;

    /**
     * 주문 항목 명령 (중첩 클래스)
     *
     * [초보자 가이드]
     * 중첩 클래스를 사용하는 이유:
     * - OrderItemCommand는 CreateOrderCommand와 밀접한 관련이 있음
     * - 외부에서 독립적으로 사용될 일이 거의 없음
     * - 네임스페이스를 깔끔하게 정리 가능
     */
    @Getter
    @Builder
    public static class OrderItemCommand {

        /** 주문 항목 타입 - RESERVATION 또는 GOODS */
        private final OrderItemType orderItemType;

        /** 세션 ID - 예약형 상품의 경우 */
        private final UUID sessionId;

        /** 옵션 ID - 세션의 추가 옵션 */
        private final UUID optionId;

        /** 굿즈 변형 ID - 구매형 상품의 경우 */
        private final UUID goodsVariantId;

        /** 수량 */
        private final Integer qty;

        /** 단가 (원) */
        private final Integer unitPrice;

    }

    // ================ Factory Methods ================

    /**
     * CreateOrderRequest로부터 CreateOrderCommand 생성
     *
     * @param request 주문 생성 요청 DTO
     * @param userId JWT에서 추출한 사용자 ID
     * @return 변환된 주문 생성 명령
     */
    public static CreateOrderCommand fromRequest(CreateOrderRequest request, Long userId) {
        List<OrderItemCommand> itemCommands = request.getItems().stream()
                .map(CreateOrderCommand::convertOrderItemRequest)
                .toList();

        return CreateOrderCommand.builder()
                .userId(userId)
                .popupId(request.getPopupId())
                .orderType(request.getOrderType())
                .items(itemCommands)
                .build();
    }

    /**
     * OrderItemRequest를 OrderItemCommand로 변환
     *
     * @param itemRequest 주문 항목 요청
     * @return 변환된 주문 항목 명령
     */
    private static OrderItemCommand convertOrderItemRequest(OrderItemRequest itemRequest) {
        return OrderItemCommand.builder()
                .orderItemType(OrderItemType.valueOf(itemRequest.getOrderItemType()))
                .sessionId(itemRequest.getSessionId())
                .goodsVariantId(itemRequest.getGoodsVariantId())
                .qty(itemRequest.getQty())
                .unitPrice(calculateItemPrice(itemRequest)) // 실제 가격 조회
                .build();
    }

    /**
     * 주문 항목의 실제 가격 조회
     *
     * @param itemRequest 주문 항목 요청
     * @return 계산된 단가
     */
    private static Integer calculateItemPrice(OrderItemRequest itemRequest) {
        try {
            // 주문 항목 타입에 따라 가격 조회
            OrderItemType itemType = OrderItemType.valueOf(itemRequest.getOrderItemType());

            switch (itemType) {
                case RESERVATION:
                    // 예약형: 세션 ID로 가격 조회
                    return getSessionPrice(itemRequest.getSessionId());

                case GOODS:
                    // 굿즈형: 굿즈 변형 ID로 가격 조회
                    return getGoodsVariantPrice(itemRequest.getGoodsVariantId());

                default:
                    // 알 수 없는 타입인 경우 기본값
                    return 10000; // 기본 가격 (1만원)
            }
        } catch (Exception e) {
            // 가격 조회 실패 시 기본값 반환 (실제로는 로그 기록 필요)
            return 10000;
        }
    }

    /**
     * 세션 가격 조회 (예약형)
     * TODO: 실제로는 Store 서비스나 Session 서비스와 연동 필요
     */
    private static Integer getSessionPrice(UUID sessionId) {
        if (sessionId == null) {
            return 10000; // 기본 예약 가격
        }

        // 실제 구현 시에는 다음과 같이 처리:
        // 1. Store 서비스 API 호출
        // 2. 세션 정보 조회
        // 3. 세션의 가격 정보 반환

        // 임시 가격 (세션 ID 기반 간단 계산)
        return 15000; // 예약 기본 가격
    }

    /**
     * 굿즈 변형 가격 조회 (구매형)
     * TODO: 실제로는 Store 서비스나 Goods 서비스와 연동 필요
     */
    private static Integer getGoodsVariantPrice(UUID goodsVariantId) {
        if (goodsVariantId == null) {
            return 5000; // 기본 굿즈 가격
        }

        // 실제 구현 시에는 다음과 같이 처리:
        // 1. Store 서비스 API 호출
        // 2. 굿즈 변형 정보 조회
        // 3. 변형의 가격 정보 반환

        // 임시 가격 (굿즈 변형 ID 기반 간단 계산)
        return 8000; // 굿즈 기본 가격
    }

}