package com.example.orderquery.domain.summary.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.orderquery.domain.summary.dto.OrderSummaryDto;
import com.example.orderquery.domain.summary.exception.SummaryException;
import com.example.orderquery.domain.summary.mapper.OrderSummaryMapper;
import com.example.orderquery.domain.summary.repository.OrderSummaryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderSummaryService {

    private final OrderSummaryRepository orderSummaryRepository;

    public OrderSummaryDto getSummary(UUID storeId, UUID popupId) {
        return orderSummaryRepository.findByStoreIdAndPopupId(storeId, popupId)
                .map(OrderSummaryMapper::toDto)
                .orElseThrow(() -> SummaryException.notFound(storeId, popupId));
    }
}
