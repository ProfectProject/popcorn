package com.popcorn.order.dto.store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 굿즈 가격 조회 응답 DTO
 *
 * Store 서비스에서 굿즈 변형의 가격 정보를 반환할 때 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsPriceResponse {

    /** 굿즈 변형 ID */
    private UUID goodsVariantId;

    /** 상품명 */
    private String productName;

    /** 판매 가격 */
    private Integer price;

    /** 정가 (할인 전 가격) */
    private Integer originalPrice;

    /** 할인율 (%) */
    private Integer discountRate;

    /** 재고 수량 */
    private Integer stockQuantity;

    /** 판매 상태 (AVAILABLE, OUT_OF_STOCK, DISCONTINUED) */
    private String status;

    /** 통화 코드 (KRW, USD 등) */
    private String currency;
}