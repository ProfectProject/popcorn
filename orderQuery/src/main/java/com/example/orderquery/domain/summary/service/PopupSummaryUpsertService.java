package com.example.orderquery.domain.summary.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.orderquery.domain.summary.entity.OrderSummary;
import com.example.orderquery.domain.summary.repository.OrderSummaryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PopupSummaryUpsertService {

    private final OrderSummaryRepository orderSummaryRepository;

    public void createFromPopup(UUID popupId,
                                UUID storeId,
                                Long ownerId,
                                String title,
                                String status,
                                String addressRoad,
                                String addressDetail,
                                LocalDateTime reservationOpenAt) {
        OrderSummary summary = orderSummaryRepository.findByStoreIdAndPopupId(storeId, popupId)
                .orElse(null);
        if (summary != null) {
            summary.setUpdatedBy(ownerId);
            summary.updatePopupInfo(title, status, addressRoad, addressDetail, reservationOpenAt);
            orderSummaryRepository.save(summary);
            return;
        }

        OrderSummary created = OrderSummary.builder()
                .popupId(popupId)
                .storeId(storeId)
                .popupTitle(title)
                .popupStatus(status)
                .addressRoad(addressRoad)
                .addressDetail(addressDetail)
                .reservationOpenAt(reservationOpenAt)
                .reservationTotalOrders(0)
                .reservationPaidOrders(0)
                .reservationCancelledOrders(0)
                .goodsTotalOrders(0)
                .goodsPaidOrders(0)
                .goodsCancelledOrders(0)
                .checkedInOrders(0)
                .build();
        created.setCreatedBy(ownerId);
        created.setUpdatedBy(ownerId);
        orderSummaryRepository.save(created);
    }

    public void updateFromPopup(UUID popupId,
                                UUID storeId,
                                Long ownerId,
                                String title,
                                String status,
                                String addressRoad,
                                String addressDetail,
                                LocalDateTime reservationOpenAt) {
        OrderSummary summary = orderSummaryRepository.findByStoreIdAndPopupId(storeId, popupId)
                .orElse(null);
        if (summary == null) {
            log.warn("Popup summary update ignored: summary not found. storeId={}, popupId={}", storeId, popupId);
            return;
        }

        summary.setUpdatedBy(ownerId);
        summary.updatePopupInfo(title, status, addressRoad, addressDetail, reservationOpenAt);
        orderSummaryRepository.save(summary);
    }

    public void deleteSummary(UUID storeId, UUID popupId) {
        orderSummaryRepository.findByStoreIdAndPopupId(storeId, popupId)
                .ifPresent(orderSummaryRepository::delete);
    }
}
