package com.popcorn.demo.domain.order.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 아이템 가격 조회 서비스 구현체
 *
 * 간단하게 Domain Layer에 직접 구현된 서비스입니다.
 * 복잡한 Architecture 패턴 없이 직관적으로 이해할 수 있습니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderItemPriceServiceImpl implements OrderItemPriceService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<Integer> findSessionOptionPrice(UUID sessionOptionId) {
        if (sessionOptionId == null) {
        log.debug("스케줄 ID가 null입니다.");
            return Optional.empty();
        }

        log.debug("스케줄 가격 조회 - ID: {}", sessionOptionId);
        return findPrice(
                "SELECT price FROM p_popup_schedules WHERE schedule_id = ? AND deleted_at IS NULL",
                sessionOptionId,
                "price",
                "스케줄"
        );
    }

    @Override
    public Optional<Integer> findMerchVariantPrice(UUID merchVariantId) {
        if (merchVariantId == null) {
            log.debug("머치 변형 ID가 null입니다.");
            return Optional.empty();
        }

        log.debug("굿즈 변형 가격 조회 - ID: {}", merchVariantId);
        return findPrice(
                "SELECT goods_price FROM p_goods_variants WHERE goods_id = ? AND deleted_at IS NULL",
                merchVariantId,
                "goods_price",
                "굿즈 변형"
        );
    }

    /**
     * 공통 가격 조회 메서드
     */
    private Optional<Integer> findPrice(String sql, UUID id, String priceColumn, String itemType) {
        try {
            Integer price = jdbcTemplate.query(sql, rs -> {
                if (rs.next()) {
                    int priceValue = rs.getInt(priceColumn);
                    return rs.wasNull() ? null : priceValue;
                }
                return null;
            }, id);

            if (price != null) {
                log.debug("{} 가격 조회 성공 - ID: {}, 가격: {}원", itemType, id, price);
            } else {
                log.warn("{} 정보를 찾을 수 없습니다 - ID: {}", itemType, id);
            }

            return Optional.ofNullable(price);
        } catch (Exception e) {
            log.error("{} 가격 조회 중 오류 발생 - ID: {}", itemType, id, e);
            return Optional.empty();
        }
    }
}
