package com.popcorn.demo.domain.inventory.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 차감/복원을 위한 아이템 DTO
 *
 * 주문 서비스에서 재고 서비스로 재고 차감 요청 시 사용하는 DTO입니다.
 * 마이크로서비스 분리 후 OrderItem 엔티티 의존성을 제거하고 대체하기 위해 생성되었습니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemDto {

    /** 항목 ID (로깅용) */
    private UUID id;

    /** 세션 옵션 ID - 예약 상품의 경우 */
    private UUID sessionOptionId;

    /** 굿즈 변형 ID - 일반 상품의 경우 */
    private UUID goodsVariantId;

    /** 수량 */
    private Integer qty;

    /**
     * OrderItem에서 InventoryItemDto로 변환하는 팩토리 메서드
     * (주문 마이크로서비스에서 사용)
     */
    public static InventoryItemDto of(UUID id, UUID sessionOptionId, UUID goodsVariantId, Integer qty) {
        return InventoryItemDto.builder()
                .id(id)
                .sessionOptionId(sessionOptionId)
                .goodsVariantId(goodsVariantId)
                .qty(qty)
                .build();
    }
}