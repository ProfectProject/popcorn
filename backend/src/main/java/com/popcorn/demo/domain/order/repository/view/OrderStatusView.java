package com.popcorn.demo.domain.order.repository.view;

import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderStatusView {

	UUID getOrderId();

	String getOrderNo();

	String getStatus();

	String getPaymentStatus();

	LocalDateTime getCancelableUntil();

	LocalDateTime getUpdatedAt();
}
