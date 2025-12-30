package com.popcorn.demo.domain.order.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 생성 완료 응답 DTO
 * POST /orders API의 성공 응답(201)으로 사용됩니다.
 *
 * API 스펙 기준:
 * - 주문 기본 정보 (id, orderNo, orderType, status, storeId, productId)
 * - 금액 정보 (totalAmount - 서버에서 계산된 총액)
 * - 정책 관련 정보 (cancelableUntil - 취소 가능 시각)
 * - 메타 정보 (createdAt)
 * - 주문 아이템 목록 (items[])
 */
public class OrderCreatedDto {

    /**
     * 주문 ID (Primary Key)
     * 생성된 주문의 고유 식별자
     */
    private Long id;

    /**
     * 주문 번호
     * 형식: O{YYYYMMDD}-{6자리순번} (예: O20251226-000501)
     */
    private String orderNo;

    /**
     * 주문 유형
     * - RESERVATION: 예약형 주문 (체험/이벤트 예약)
     * - PURCHASE: 구매형 주문 (굿즈 구매)
     */
    private String orderType;

    /**
     * 주문 상태
     * 생성 시점에는 항상 "REQUESTED"로 고정
     */
    private String status;

    /**
     * 스토어 ID
     * 주문 대상 매장의 식별자
     */
    private Long storeId;

    /**
     * 상품 ID
     * 주문 기준 상품(행사/굿즈)의 식별자
     */
    private Long productId;

    /**
     * 총 주문 금액
     * 서버에서 계산된 전체 아이템의 합계 금액
     * (클라이언트에서 전송한 금액은 무시하고 서버에서 재계산)
     */
    private Integer totalAmount;

    /**
     * 취소 가능 기한
     * 이 시각 이후로는 주문 취소가 불가능
     * 비즈니스 정책에 따라 계산됨
     */
    private LocalDateTime cancelableUntil;

    /**
     * 주문 생성 시각
     * 주문이 최초 생성된 시간
     */
    private LocalDateTime createdAt;

    /**
     * 주문 아이템 목록
     * 주문에 포함된 개별 아이템들의 상세 정보
     * (최소 1개 이상 보장)
     */
    private List<OrderItemDto> items;

    // 기본 생성자
    public OrderCreatedDto() {}

    // 전체 필드 생성자
    public OrderCreatedDto(Long id, String orderNo, String orderType, String status,
                           Long storeId, Long productId, Integer totalAmount,
                           LocalDateTime cancelableUntil, LocalDateTime createdAt,
                           List<OrderItemDto> items) {
        this.id = id;
        this.orderNo = orderNo;
        this.orderType = orderType;
        this.status = status;
        this.storeId = storeId;
        this.productId = productId;
        this.totalAmount = totalAmount;
        this.cancelableUntil = cancelableUntil;
        this.createdAt = createdAt;
        this.items = items;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getCancelableUntil() { return cancelableUntil; }
    public void setCancelableUntil(LocalDateTime cancelableUntil) { this.cancelableUntil = cancelableUntil; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }

    /**
     * 주문 아이템 정보 DTO
     * 주문에 포함된 개별 아이템의 상세 정보를 담는 내부 클래스
     */
    public static class OrderItemDto {

        /**
         * 주문 아이템 ID
         * 개별 주문 아이템의 고유 식별자
         */
        private Long id;

        /**
         * 주문 아이템 유형
         * - RESERVATION: 예약 아이템 (체험/이벤트)
         * - MERCH: 굿즈 아이템
         * 주문 유형(orderType)과 일치해야 함
         */
        private String orderItemType;

        /**
         * 수량
         * 해당 아이템의 주문 수량 (1 이상)
         */
        private Integer qty;

        /**
         * 단가
         * 서버에서 계산된 개별 아이템의 단위 가격
         * (클라이언트 값은 무시하고 서버에서 재계산)
         */
        private Integer unitPrice;

        /**
         * 라인 금액
         * unitPrice * qty 로 계산된 해당 아이템의 총 금액
         */
        private Integer lineAmount;

        // 기본 생성자
        public OrderItemDto() {}

        // 전체 필드 생성자
        public OrderItemDto(Long id, String orderItemType, Integer qty,
                            Integer unitPrice, Integer lineAmount) {
            this.id = id;
            this.orderItemType = orderItemType;
            this.qty = qty;
            this.unitPrice = unitPrice;
            this.lineAmount = lineAmount;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getOrderItemType() { return orderItemType; }
        public void setOrderItemType(String orderItemType) { this.orderItemType = orderItemType; }

        public Integer getQty() { return qty; }
        public void setQty(Integer qty) { this.qty = qty; }

        public Integer getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Integer unitPrice) { this.unitPrice = unitPrice; }

        public Integer getLineAmount() { return lineAmount; }
        public void setLineAmount(Integer lineAmount) { this.lineAmount = lineAmount; }
    }
}