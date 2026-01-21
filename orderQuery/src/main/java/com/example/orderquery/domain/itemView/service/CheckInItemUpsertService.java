package com.example.orderquery.domain.itemView.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.orderquery.domain.itemView.repository.OrderItemViewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CheckInItemUpsertService {

    private final OrderItemViewRepository orderItemViewRepository;

    public void updateFromCheckIn(UUID storeId, UUID popupId, UUID orderGoodsId, LocalDateTime checkinAt) {
        int updated = orderItemViewRepository.markCheckedIn(storeId, popupId, orderGoodsId, checkinAt);
        if (updated == 0) {
            log.warn("CheckIn item update ignored: item not found. storeId={}, popupId={}, orderGoodsId={}",
                    storeId, popupId, orderGoodsId);
        }
    }
}
