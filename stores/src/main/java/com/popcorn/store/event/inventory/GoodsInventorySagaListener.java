package com.popcorn.store.event.inventory;

import com.popcorn.store.domain.goods.entity.GoodsOrderReservation;
import com.popcorn.store.domain.goods.entity.ReservationStatus;
import com.popcorn.store.domain.goods.entity.ReservationType;
import com.popcorn.store.domain.goods.service.GoodsOrderReservationService;
import com.popcorn.store.domain.goods.service.GoodsService;
import com.popcorn.store.event.order.OrderPaidEvent;
import com.popcorn.store.event.order.StockDeductionFailedEvent;
import com.popcorn.store.event.order.StockDeductionSuccessEvent;
import com.popcorn.store.event.order.StockReservedEvent;
import com.popcorn.store.event.order.StockReservationFailedEvent;
import com.popcorn.store.event.payment.InventoryConfirmationRequestedEvent;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoodsInventorySagaListener {

    private final GoodsService goodsService;
    private final GoodsOrderReservationService reservationService;
    private final StoreInventoryEventPublisher eventPublisher;

    @EventListener
    @Transactional
    public void handleOrderPaid(OrderPaidEvent event) {
        List<OrderPaidEvent.OrderItemInfo> goodsItems = event.getGoodsItems();
        if (goodsItems.isEmpty()) {
            log.info("굿즈가 포함되지 않은 주문 - orderId: {}", event.getOrderId());
            return;
        }

        List<GoodsOrderReservation> createdReservations = new ArrayList<>();
        List<StockReservedEvent.ReservedStockItem> reservedItems = new ArrayList<>();

        try {
            for (OrderPaidEvent.OrderItemInfo item : goodsItems) {
                if (item.getGoodsVariantId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                    log.warn("잘못된 굿즈 항목 - orderId: {}, goodsId: {}, quantity: {}",
                            event.getOrderId(), item.getGoodsVariantId(), item.getQuantity());
                    continue;
                }

                goodsService.reservationGoods(event.getPopupId(), item.getGoodsVariantId(), item.getQuantity());

                GoodsOrderReservation reservation = reservationService.createGoodsReservation(
                        event.getOrderId(),
                        event.getOrderNo(),
                        event.getPopupId(),
                        item.getGoodsVariantId(),
                        item.getQuantity()
                );
                createdReservations.add(reservation);

                reservedItems.add(StockReservedEvent.ReservedStockItem.goods(
                        item.getGoodsVariantId(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        resolveProductName(item)
                ));
            }

            if (!reservedItems.isEmpty()) {
                eventPublisher.publishStockReservedEvent(event, reservedItems);
            }

        } catch (Exception e) {
            log.error("재고 예약 실패 - orderId: {}, error: {}", event.getOrderId(), e.getMessage(), e);
            rollbackReservations(createdReservations);

            List<StockReservationFailedEvent.FailedStockItem> failedItems = buildFailedItems(goodsItems, e.getMessage());
            eventPublisher.publishStockReservationFailedEvent(event, failedItems, e.getMessage());

            throw new RuntimeException("재고 예약 처리 중 오류 발생", e);
        }
    }

    @EventListener
    @Transactional
    public void handleInventoryConfirmation(InventoryConfirmationRequestedEvent event) {
        List<GoodsOrderReservation> reservations = reservationService.findByOrderIdAndType(event.getOrderId(), ReservationType.GOODS);
        if (reservations.isEmpty()) {
            log.warn("예약 정보 없음 - inventory event: {}", event.getOrderId());
            return;
        }

        UUID orderId = event.getOrderId();
        String orderNo = reservations.stream()
                .findFirst()
                .map(GoodsOrderReservation::getOrderNo)
                .orElse(null);
        UUID popupId = reservations.stream()
                .findFirst()
                .map(GoodsOrderReservation::getPopupId)
                .orElse(null);

        if (event.isConfirmAction()) {
            List<String> details = new ArrayList<>();
            try {
                for (GoodsOrderReservation reservation : reservations) {
                    if (ReservationStatus.RESERVED != reservation.getStatus()) {
                        continue;
                    }
                    goodsService.completeReservationGoods(reservation.getPopupId(),
                            reservation.getGoodsVariantId(), reservation.getQuantity());
                    reservationService.updateStatus(reservation, ReservationStatus.CONFIRMED, null);
                    details.add(formatDetail(reservation));
                }
                eventPublisher.publishStockDeductionSuccessEvent(orderId, orderNo, popupId, String.join(", ", details));
            } catch (Exception e) {
                log.error("재고 차감 확정 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
                reservations.stream()
                        .filter(res -> ReservationStatus.RESERVED == res.getStatus())
                        .forEach(res -> reservationService.updateStatus(res, ReservationStatus.FAILED, e.getMessage()));
                eventPublisher.publishStockDeductionFailedEvent(orderId, orderNo, popupId,
                        "재고 차감 실패: " + e.getMessage(), "SYSTEM_ERROR", e.getMessage());
                throw new RuntimeException("재고 차감 확정 처리 실패", e);
            }
        } else if (event.isRestoreAction()) {
            reservations.stream()
                    .filter(res -> ReservationStatus.RESERVED == res.getStatus())
                    .forEach(res -> {
                        try {
                            goodsService.cancelReservationGoods(res.getPopupId(),
                                    res.getGoodsVariantId(), res.getQuantity());
                            reservationService.updateStatus(res, ReservationStatus.RESTORED, event.getReason());
                        } catch (Exception e) {
                            log.error("재고 복구 실패 - orderId: {}, goodsId: {}, error: {}",
                                    orderId, res.getGoodsVariantId(), e.getMessage(), e);
                            reservationService.updateStatus(res, ReservationStatus.FAILED, e.getMessage());
                        }
                    });
            eventPublisher.publishStockDeductionFailedEvent(orderId, orderNo, popupId,
                    "재고 복구 - " + event.getReason(), "PAYMENT_RESTORE",
                    "restore requested by payment event");
        }
    }

    private void rollbackReservations(List<GoodsOrderReservation> reservations) {
        for (GoodsOrderReservation reservation : reservations) {
            try {
                goodsService.cancelReservationGoods(reservation.getPopupId(),
                        reservation.getGoodsVariantId(), reservation.getQuantity());
                reservationService.updateStatus(reservation, ReservationStatus.RESTORED,
                        "rollback after failure");
            } catch (Exception ex) {
                log.error("재고 롤백 실패 - goodsId: {}, error: {}", reservation.getGoodsVariantId(),
                        ex.getMessage(), ex);
            }
        }
    }

    private List<StockReservationFailedEvent.FailedStockItem> buildFailedItems(
            List<OrderPaidEvent.OrderItemInfo> goodsItems, String failureReason) {
        List<StockReservationFailedEvent.FailedStockItem> failedItems = new ArrayList<>();
        for (OrderPaidEvent.OrderItemInfo item : goodsItems) {
            if (item.getGoodsVariantId() == null || item.getQuantity() == null) {
                continue;
            }
            failedItems.add(StockReservationFailedEvent.FailedStockItem.create(
                    item.getGoodsVariantId(),
                    item.getQuantity(),
                    0,
                    resolveProductName(item),
                    failureReason));
        }
        return failedItems;
    }

    private String resolveProductName(OrderPaidEvent.OrderItemInfo item) {
        return "굿즈 상품";
    }

    private String formatDetail(GoodsOrderReservation reservation) {
        return String.format("goodsId=%s qty=%d", reservation.getGoodsVariantId(), reservation.getQuantity());
    }
}
