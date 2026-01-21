package com.popcorn.order.dto.query;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 목록 조회 쿼리 DTO (Store 서비스 방식)
 *
 * [사용 가이드]
 * Store 서비스의 PopupListQuery와 동일한 구조로 설계:
 * - Builder 패턴으로 객체 생성
 * - 다양한 검색 조건 지원
 * - 페이징 파라미터 포함
 * - withTotal로 전체 개수 계산 여부 제어
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListQuery {

    /** 팝업 ID 필터 */
    private UUID popupId;

    /** 주문 상태 필터 (REQUESTED, PAID, COMPLETED, CANCELLED 등) */
    private String status;

    /** 주문 타입 필터 (RESERVATION, GOODS, MIXED) */
    private String orderType;

    /** 사용자 ID 필터 */
    private Long userId;

    /** 가게 ID 필터 */
    private UUID storeId;

    /** 검색 시작 날짜 */
    private LocalDateTime from;

    /** 검색 종료 날짜 */
    private LocalDateTime to;

    /** 페이지 번호 (1부터 시작) */
    private Integer page;

    /** 페이지 크기 (기본 20) */
    private Integer size;

    /** 전체 개수 포함 여부 (기본 true) */
    private Boolean withTotal;

    /**
     * 페이지 번호 검증 및 기본값 설정
     * @return 검증된 페이지 번호 (최소 1)
     */
    public Integer getValidatedPage() {
        return page != null && page > 0 ? page : 1;
    }

    /**
     * 페이지 크기 검증 및 기본값 설정
     * @return 검증된 페이지 크기 (기본 20, 최대 100)
     */
    public Integer getValidatedSize() {
        if (size == null || size <= 0) {
            return 20; // 기본값
        }
        return Math.min(size, 100); // 최대 100개 제한
    }

    /**
     * 전체 개수 포함 여부 검증
     * @return 전체 개수 포함 여부 (기본 true)
     */
    public Boolean getValidatedWithTotal() {
        return withTotal != null ? withTotal : true;
    }

    /**
     * offset 계산 (JPA나 MyBatis에서 사용)
     * @return 조회 시작 위치
     */
    public Integer getOffset() {
        return (getValidatedPage() - 1) * getValidatedSize();
    }
}