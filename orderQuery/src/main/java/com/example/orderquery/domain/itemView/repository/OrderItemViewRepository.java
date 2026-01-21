package com.example.orderquery.domain.itemView.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.orderquery.domain.itemView.entity.OrderItemView;
import com.example.orderquery.domain.itemView.entity.OrderItemViewId;

public interface OrderItemViewRepository extends JpaRepository<OrderItemView, OrderItemViewId>,
        JpaSpecificationExecutor<OrderItemView> {
}
