package com.popcorn.demo.infrastructure.persistence.order;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.popcorn.demo.application.order.port.out.FindOrderItemPricePort;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;

/**
	* 주문 항목 단가 조회 어댑터 (Infrastructure Layer)
	*
	* 세션 옵션/머치 변형 단가를 네이티브 쿼리로 조회합니다.
	*/
@Component
@RequiredArgsConstructor
public class OrderItemPriceAdapter implements FindOrderItemPricePort {

	private final EntityManager entityManager;

	@Override
	public Optional<Integer> findSessionOptionPrice(Long sessionOptionId) {
		if (sessionOptionId == null || sessionOptionId <= 0) {
			return Optional.empty();
		}
		return findPrice(
				"SELECT price FROM p_session_options WHERE id = :id AND deleted_at IS NULL",
				sessionOptionId
		);
	}

	@Override
	public Optional<Integer> findMerchVariantPrice(Long merchVariantId) {
		if (merchVariantId == null || merchVariantId <= 0) {
			return Optional.empty();
		}
		return findPrice(
				"SELECT price FROM p_merch_variants WHERE id = :id AND deleted_at IS NULL",
				merchVariantId
		);
	}

	private Optional<Integer> findPrice(String sql, Long id) {
		try {
			Object result = entityManager.createNativeQuery(sql)
					.setParameter("id", id)
					.getSingleResult();
			if (result instanceof Number number) {
				return Optional.of(number.intValue());
			}
			return Optional.empty();
		} catch (NoResultException ex) {
			return Optional.empty();
		}
	}
}
