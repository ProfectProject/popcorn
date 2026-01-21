package com.example.orderquery.domain.summary.repository;

import com.example.orderquery.domain.summary.entity.OrderSummary;

import java.util.Optional;
import java.util.UUID;

public interface OrderSummaryRepository {

    Optional<OrderSummary> findByPopupId(UUID popupId);

}
