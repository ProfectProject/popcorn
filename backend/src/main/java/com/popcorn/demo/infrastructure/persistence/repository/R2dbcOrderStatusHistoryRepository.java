package com.popcorn.demo.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.popcorn.demo.domain.order.entity.OrderStatusHistory;

public interface R2dbcOrderStatusHistoryRepository extends ReactiveCrudRepository<OrderStatusHistory, UUID> {
}
