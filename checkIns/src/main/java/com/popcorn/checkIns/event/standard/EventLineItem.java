package com.popcorn.checkIns.event.standard;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 표준 이벤트의 주문 라인 아이템
 * 주문 내 개별 항목들을 표현
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventLineItem {

    /**
     * 아이템 타입 (RESERVATION, GOODS)
     */
    private String itemType;

    /**
     * 스케줄 ID (예약 아이템인 경우)
     */
    private UUID scheduleId;

    /**
     * 굿즈 변형 ID (굿즈 아이템인 경우)
     */
    private UUID goodsId;

    /**
     * 수량
     */
    private Integer qty;

    /**
     * 단가
     */
    private BigDecimal unitPrice;

    /**
     * 라인 총액 (단가 * 수량)
     */
    private BigDecimal linePrice;

    /**
     * 아이템명 (optional)
     */
    private String itemName;

    /**
     * 아이템 설명 (optional)
     */
    private String itemDescription;
}