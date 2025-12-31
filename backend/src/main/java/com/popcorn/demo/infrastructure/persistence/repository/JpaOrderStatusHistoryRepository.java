package com.popcorn.demo.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.popcorn.demo.domain.order.entity.OrderStatusHistory;

public interface JpaOrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
}
