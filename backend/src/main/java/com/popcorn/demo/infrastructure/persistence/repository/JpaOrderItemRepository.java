package com.popcorn.demo.infrastructure.persistence.repository;

import com.popcorn.demo.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaOrderItemRepository extends JpaRepository<OrderItem, Long> {
}
