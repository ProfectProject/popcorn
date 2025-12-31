package com.popcorn.demo.infrastructure.persistence.order;

import java.util.UUID;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import com.popcorn.demo.application.order.port.out.FindOrderItemPricePort;

import lombok.RequiredArgsConstructor;

/**
	* 주문 항목 단가 조회 어댑터 (Infrastructure Layer)
	*
	* 세션 옵션/머치 변형 단가를 네이티브 쿼리로 조회합니다.
	*/
@Component
@RequiredArgsConstructor
public class OrderItemPriceAdapter implements FindOrderItemPricePort {

	private final DatabaseClient databaseClient;

	@Override
	public Mono<Integer> findSessionOptionPrice(UUID sessionOptionId) {
		if (sessionOptionId == null) {
			return Mono.empty();
		}
		return findPrice(
				"SELECT price FROM p_session_options WHERE id = :id AND deleted_at IS NULL",
				sessionOptionId
		);
	}

	@Override
	public Mono<Integer> findMerchVariantPrice(UUID merchVariantId) {
		if (merchVariantId == null) {
			return Mono.empty();
		}
		return findPrice(
				"SELECT price FROM p_merch_variants WHERE id = :id AND deleted_at IS NULL",
				merchVariantId
		);
	}

	private Mono<Integer> findPrice(String sql, UUID id) {
		return databaseClient.sql(sql)
				.bind("id", id)
				.map((row, metadata) -> {
					Number value = row.get("price", Number.class);
					return value == null ? null : value.intValue();
				})
				.one();
	}
}
