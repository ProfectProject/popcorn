package com.popcorn.demo.domain.order.repository.view;

import java.time.LocalDateTime;
import java.util.UUID;

public interface StoreOrderReservationView {

	UUID getId();

	String getOrderNo();

	String getStatus();

	Integer getTotalAmount();

	LocalDateTime getCancelableUntil();

	LocalDateTime getCreatedAt();
}
