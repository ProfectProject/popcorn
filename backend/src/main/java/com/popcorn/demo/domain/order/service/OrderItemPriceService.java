package com.popcorn.demo.domain.order.service;

import java.util.Optional;
import java.util.UUID;

/**
 * 주문 아이템 가격 조회 서비스
 *
 * 간단한 Service 인터페이스로, 주문 아이템의 가격을 조회하는 기능을 제공합니다.
 * 구현체는 Infrastructure Layer에서 제공됩니다.
 */
public interface OrderItemPriceService {

    /**
     * 세션 옵션 가격 조회
     * @param sessionOptionId 세션 옵션 ID
     * @return 가격 (원 단위)
     */
    Optional<Integer> findSessionOptionPrice(UUID sessionOptionId);

    /**
     * 머치 변형 가격 조회
     * @param merchVariantId 머치 변형 ID
     * @return 가격 (원 단위)
     */
    Optional<Integer> findMerchVariantPrice(UUID merchVariantId);
}