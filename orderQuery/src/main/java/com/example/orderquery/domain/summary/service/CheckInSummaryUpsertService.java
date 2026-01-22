package com.example.orderquery.domain.summary.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.orderquery.domain.summary.repository.OrderSummaryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CheckInSummaryUpsertService {

    private final OrderSummaryRepository orderSummaryRepository;

    public void incrementCheckedIn(UUID storeId, UUID popupId) {
        orderSummaryRepository.findByStoreIdAndPopupId(storeId, popupId)
                .ifPresentOrElse(summary -> {
                    summary.applyDeltas(0, 0, 0, 0, 0, 0, 1, null);
                    orderSummaryRepository.save(summary);
                }, () -> log.warn("CheckIn summary update ignored: summary not found. storeId={}, popupId={}",
                        storeId, popupId));
    }
}
