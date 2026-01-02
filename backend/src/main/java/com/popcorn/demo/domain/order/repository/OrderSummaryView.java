package com.popcorn.demo.domain.order.repository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderSummaryView {

	UUID getId();



	String getOrderNo();



	String getStatus();



	Integer getTotalAmount();



	LocalDateTime getCreatedAt();

}
