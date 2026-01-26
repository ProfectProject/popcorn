package com.popcorn.store.api.event;

import com.popcorn.store.domain.goods.service.GoodsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 재고 관련 이벤트 수신 컨트롤러
 *
 * Order 서비스에서 발송하는 재고 관련 이벤트를 HTTP API로 수신합니다.
 */
@RestController
@RequestMapping("/api/stores/v1/events")
@RequiredArgsConstructor
@Slf4j
public class StockEventController {

    private final GoodsService goodsService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 재고 차감 요청 이벤트 수신
     */
    @PostMapping("/stock-deduction-requested")
    public ResponseEntity<StockDeductionResponse> handleStockDeductionRequested(
            @RequestBody StockDeductionRequestedEvent event) {

        try {
            log.info("재고 차감 요청 이벤트 수신 - orderId: {}, popupId: {}, items: {}",
                    event.getOrderId(), event.getPopupId(), event.getDeductionItems().size());

            // 각 항목별로 재고 차감 처리
            boolean allSuccess = true;
            String failureReason = null;

            for (StockDeductionRequestedEvent.StockDeductionItem item : event.getDeductionItems()) {
                try {
                    log.info("굿즈 재고 차감 처리 - goodsVariantId: {}, quantity: {}",
                            item.getGoodsVariantId(), item.getQuantity());

                    // 실제 재고 차감 (기존 HTTP API 로직 재사용)
                    goodsService.completeReservationGoods(
                            event.getPopupId(),
                            item.getGoodsVariantId(),
                            item.getQuantity()
                    );

                    log.info("굿즈 재고 차감 성공 - goodsVariantId: {}", item.getGoodsVariantId());

                } catch (Exception e) {
                    log.error("굿즈 재고 차감 실패 - goodsVariantId: {}, error: {}",
                            item.getGoodsVariantId(), e.getMessage(), e);
                    allSuccess = false;
                    failureReason = "재고 차감 실패: " + e.getMessage();
                    break; // 하나라도 실패하면 전체 실패
                }
            }

            // 결과에 따라 성공/실패 이벤트를 Order 서비스로 다시 전송해야 함
            if (allSuccess) {
                log.info("재고 차감 요청 처리 성공 - orderId: {}", event.getOrderId());

                // TODO: Order 서비스로 StockDeductionSuccessEvent HTTP 전송
                // sendStockDeductionSuccessEvent(event);

                return ResponseEntity.ok(StockDeductionResponse.success(event.getEventId()));

            } else {
                log.error("재고 차감 요청 처리 실패 - orderId: {}, reason: {}",
                        event.getOrderId(), failureReason);

                // TODO: Order 서비스로 StockDeductionFailedEvent HTTP 전송
                // sendStockDeductionFailedEvent(event, failureReason);

                return ResponseEntity.ok(StockDeductionResponse.failure(event.getEventId(), failureReason));
            }

        } catch (Exception e) {
            log.error("재고 차감 요청 이벤트 처리 중 오류 - orderId: {}", event.getOrderId(), e);

            return ResponseEntity.ok(StockDeductionResponse.failure(
                    event.getEventId(), "시스템 오류: " + e.getMessage()));
        }
    }

    /**
     * 재고 차감 요청 이벤트 DTO
     */
    public static class StockDeductionRequestedEvent {
        private String eventId;
        private UUID orderId;
        private String orderNo;
        private UUID popupId;
        private List<StockDeductionItem> deductionItems;

        // getters and setters
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }

        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }

        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

        public UUID getPopupId() { return popupId; }
        public void setPopupId(UUID popupId) { this.popupId = popupId; }

        public List<StockDeductionItem> getDeductionItems() { return deductionItems; }
        public void setDeductionItems(List<StockDeductionItem> deductionItems) { this.deductionItems = deductionItems; }

        public static class StockDeductionItem {
            private UUID goodsVariantId;
            private Integer quantity;
            private String productName;
            private String variantName;

            // getters and setters
            public UUID getGoodsVariantId() { return goodsVariantId; }
            public void setGoodsVariantId(UUID goodsVariantId) { this.goodsVariantId = goodsVariantId; }

            public Integer getQuantity() { return quantity; }
            public void setQuantity(Integer quantity) { this.quantity = quantity; }

            public String getProductName() { return productName; }
            public void setProductName(String productName) { this.productName = productName; }

            public String getVariantName() { return variantName; }
            public void setVariantName(String variantName) { this.variantName = variantName; }
        }
    }

    /**
     * 재고 차감 응답 DTO
     */
    public static class StockDeductionResponse {
        private String eventId;
        private boolean success;
        private String message;

        public StockDeductionResponse(String eventId, boolean success, String message) {
            this.eventId = eventId;
            this.success = success;
            this.message = message;
        }

        public static StockDeductionResponse success(String eventId) {
            return new StockDeductionResponse(eventId, true, "재고 차감 완료");
        }

        public static StockDeductionResponse failure(String eventId, String message) {
            return new StockDeductionResponse(eventId, false, message);
        }

        // getters
        public String getEventId() { return eventId; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}