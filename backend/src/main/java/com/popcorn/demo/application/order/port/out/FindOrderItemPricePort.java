package com.popcorn.demo.application.order.port.out;

import java.util.UUID;

import reactor.core.publisher.Mono;

/**
	* 주문 항목 단가 조회 포트
	*
	* 주문 생성 시 세션 옵션/머치 변형의 가격을 조회합니다.
	*/
public interface FindOrderItemPricePort {

	Mono<Integer> findSessionOptionPrice(UUID sessionOptionId);

	Mono<Integer> findMerchVariantPrice(UUID merchVariantId);
}
