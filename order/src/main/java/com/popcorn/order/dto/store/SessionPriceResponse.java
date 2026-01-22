package com.popcorn.order.dto.store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 세션 가격 조회 응답 DTO
 *
 * Store 서비스에서 팝업 세션의 가격 정보를 반환할 때 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionPriceResponse {

    /** 세션 ID */
    private UUID sessionId;

    /** 팝업 ID */
    private UUID popupId;

    /** 세션명 */
    private String sessionName;

    /** 참여 가격 */
    private Integer price;

    /** 정가 (할인 전 가격) */
    private Integer originalPrice;

    /** 할인율 (%) */
    private Integer discountRate;

    /** 남은 좌석 수 */
    private Integer availableSeats;

    /** 총 좌석 수 */
    private Integer totalSeats;

    /** 세션 상태 (AVAILABLE, FULL, CANCELLED, ENDED) */
    private String status;

    /** 세션 시작 시간 */
    private LocalDateTime sessionStartTime;

    /** 세션 종료 시간 */
    private LocalDateTime sessionEndTime;

    /** 통화 코드 (KRW, USD 등) */
    private String currency;
}