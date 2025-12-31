package com.popcorn.demo.domain.order.repository;

import java.time.LocalDateTime;

public interface OrderSummaryView {

	Long getId();



	String getOrderNo();



	String getStatus();



	Integer getTotalAmount();



	LocalDateTime getCreatedAt();

}

