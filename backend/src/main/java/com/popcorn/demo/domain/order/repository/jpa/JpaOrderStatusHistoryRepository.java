package com.popcorn.demo.domain.order.repository.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.popcorn.demo.domain.order.entity.OrderStatusHistory;

public interface JpaOrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {
}
