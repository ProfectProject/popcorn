package com.popcorn.demo.domain.order.repository.view;

import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderDetailView {

	UUID getOrderId();

	String getOrderNo();

	String getOrderType();

	String getStatus();

	Long getCustomerId();

	String getCustomerRole();

	String getCustomerPhone();

	UUID getStoreId();

	Long getStoreOwnerId();

	UUID getPopupId();

	Integer getTotalAmount();

	LocalDateTime getCancelableUntil();

	LocalDateTime getCreatedAt();

	LocalDateTime getUpdatedAt();
}
