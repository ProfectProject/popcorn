package com.popcorn.store.event.inventory;

import com.popcorn.store.domain.goods.entity.GoodsOrderReservation;
import com.popcorn.store.domain.goods.entity.ReservationStatus;
import com.popcorn.store.domain.goods.entity.ReservationType;
import com.popcorn.store.domain.goods.service.GoodsOrderReservationService;
import com.popcorn.store.domain.popup.dto.query.response.PopupScheduleCapacity;
import com.popcorn.store.domain.popup.service.PopupService;
import com.popcorn.store.event.order.OrderPaidEvent;
import com.popcorn.store.event.order.StockReservedEvent;
import com.popcorn.store.event.order.StockReservationFailedEvent;
import com.popcorn.store.event.payment.InventoryConfirmationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PopupScheduleInventorySagaListener {

    private final PopupService popupService;
    private final GoodsOrderReservationService reservationService;
    private final StoreInventoryEventPublisher eventPublisher;

    @EventListener
    @Transactional
    public void handleOrderPaid(OrderPaidEvent event) {
        List<OrderPaidEvent.OrderItemInfo> scheduleItems = event.getReservationItems();
        if (scheduleItems.isEmpty()) {
            return;
        }

        List<GoodsOrderReservation> createdReservations = new ArrayList<>();
        List<StockReservedEvent.ReservedStockItem> reservedItems = new ArrayList<>();

        try {
            for (OrderPaidEvent.OrderItemInfo item : scheduleItems) {
                if (!item.isReservationItem() || item.getSessionId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                    continue;
                }

                PopupScheduleCapacity capacity = popupService.reservationPopupSchedule(
                        event.getPopupId(),
                        item.getSessionId(),
                        item.getQuantity()
                );

                GoodsOrderReservation reservation = reservationService.createScheduleReservation(
                        event.getOrderId(),
                        event.getOrderNo(),
                        event.getPopupId(),
                        item.getSessionId(),
                        item.getQuantity()
                );
                createdReservations.add(reservation);

                reservedItems.add(buildReservationRecord(item, capacity));
            }

            if (!reservedItems.isEmpty()) {
                eventPublisher.publishStockReservedEvent(event, reservedItems);
            }

        } catch (Exception e) {
            log.error("팝업 스케줄 재고 예약 실패 - orderId: {}, error: {}", event.getOrderId(), e.getMessage(), e);
            rollbackScheduleReservations(createdReservations);
            List<StockReservationFailedEvent.FailedStockItem> failedItems = buildFailedScheduleItems(scheduleItems, e.getMessage());
            eventPublisher.publishStockReservationFailedEvent(event, failedItems, e.getMessage());
            throw new RuntimeException("팝업 스케줄 재고 예약 실패", e);
        }
    }

    @EventListener
    @Transactional
    public void handleInventoryConfirmation(InventoryConfirmationRequestedEvent event) {
        List<GoodsOrderReservation> reservations =
                reservationService.findByOrderIdAndType(event.getOrderId(), ReservationType.SCHEDULE);
        if (reservations.isEmpty()) {
            return;
        }

        UUID orderId = event.getOrderId();
        String orderNo = reservations.stream().findFirst().map(GoodsOrderReservation::getOrderNo).orElse(null);
        UUID popupId = reservations.stream().findFirst().map(GoodsOrderReservation::getPopupId).orElse(null);

        if (event.isConfirmAction()) {
            // 결제 성공이 확인되면 HELD 상태인 예약만 COMMITTED로 상태 전환하고 DB 재고 차감
            List<String> details = new ArrayList<>();
            try {
                for (GoodsOrderReservation reservation : reservations) {
                    if (ReservationStatus.HELD != reservation.getStatus()) {
                        continue;
                    }
                    popupService.completePopupScheduleReservation(
                            reservation.getScheduleId(),
                            reservation.getQuantity()
                    );
                    reservationService.updateStatus(reservation, ReservationStatus.COMMITTED, null);
                    details.add(formatDetail(reservation));
                }
                eventPublisher.publishStockDeductionSuccessEvent(orderId, orderNo, popupId,
                        String.join(", ", details));
            } catch (Exception e) {
                log.error("팝업 스케줄 재고 확정 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
                reservations.stream()
                        .filter(res -> ReservationStatus.HELD == res.getStatus())
                        .forEach(res -> reservationService.updateStatus(res, ReservationStatus.FAILED, e.getMessage()));
                eventPublisher.publishStockDeductionFailedEvent(orderId, orderNo, popupId,
                        "재고 차감 실패: " + e.getMessage(), "SYSTEM_ERROR", e.getMessage());
                throw new RuntimeException("팝업 스케줄 재고 확정 실패", e);
            }
        } else if (event.isRestoreAction()) {
            // 결제 실패/복구 흐름은 REDIS 릴리즈 전략이 담당하므로 예약 상태만 RELEASED로 전환
            reservations.stream()
                    .filter(res -> ReservationStatus.HELD == res.getStatus())
                    .forEach(res -> {
                        try {
                            popupService.cancelPopupScheduleReservation(res.getScheduleId(), res.getQuantity());
                            reservationService.updateStatus(res, ReservationStatus.RELEASED, event.getReason());
                        } catch (Exception e) {
                            log.error("팝업 스케줄 재고 복구 실패 - orderId: {}, scheduleId: {}, error: {}",
                                    orderId, res.getScheduleId(), e.getMessage(), e);
                            reservationService.updateStatus(res, ReservationStatus.FAILED, e.getMessage());
                        }
                    });
            eventPublisher.publishStockDeductionFailedEvent(orderId, orderNo, popupId,
                    "재고 복구 - " + event.getReason(), "PAYMENT_RESTORE",
                    "restore requested by payment event");
        }
    }

    private StockReservedEvent.ReservedStockItem buildReservationRecord(OrderPaidEvent.OrderItemInfo item,
                                                                        PopupScheduleCapacity capacity) {
        return StockReservedEvent.ReservedStockItem.builder()
                .scheduleId(item.getSessionId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .reservationCategory(StockReservedEvent.ReservedStockItem.ReservationCategory.SCHEDULE)
                .reservationName(buildScheduleName(item.getSessionId()))
                .reservationDetails(capacity != null ? capacity.getRemainingCapacity() : null)
                .build();
    }

    private void rollbackScheduleReservations(List<GoodsOrderReservation> reservations) {
        for (GoodsOrderReservation reservation : reservations) {
            try {
                popupService.cancelPopupScheduleReservation(reservation.getScheduleId(), reservation.getQuantity());
                reservationService.updateStatus(reservation, ReservationStatus.FAILED,
                        "rollback after failure");
            } catch (Exception ex) {
                log.error("팝업 스케줄 재고 롤백 실패 - scheduleId: {}, error: {}", reservation.getScheduleId(),
                        ex.getMessage(), ex);
            }
        }
    }

    private List<StockReservationFailedEvent.FailedStockItem> buildFailedScheduleItems(
            List<OrderPaidEvent.OrderItemInfo> scheduleItems, String failureReason) {
        List<StockReservationFailedEvent.FailedStockItem> failedItems = new ArrayList<>();
        for (OrderPaidEvent.OrderItemInfo item : scheduleItems) {
            if (!item.isReservationItem() || item.getSessionId() == null || item.getQuantity() == null) {
                continue;
            }
            failedItems.add(StockReservationFailedEvent.FailedStockItem.create(
                    null,
                    item.getQuantity(),
                    0,
                    buildScheduleName(item.getSessionId()),
                    failureReason
            ));
        }
        return failedItems;
    }

    private String buildScheduleName(UUID scheduleId) {
        return "팝업 스케줄 " + scheduleId;
    }

    private String formatDetail(GoodsOrderReservation reservation) {
        return String.format("scheduleId=%s qty=%d", reservation.getScheduleId(), reservation.getQuantity());
    }
}
