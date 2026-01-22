package com.example.orderquery.domain.summary.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.orderquery.domain.summary.entity.OrderSummary;

public interface OrderSummaryRepository extends JpaRepository<OrderSummary, UUID> {

    Optional<OrderSummary> findByStoreIdAndPopupId(UUID storeId, UUID popupId);
}
