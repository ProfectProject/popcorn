package com.popcorn.demo.domain.order.repository.view;

import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderTimelineView {

	UUID getId();

	String getOrderNo();

	String getOrderType();

	String getStatus();

	Integer getTotalAmount();

	LocalDateTime getCancelableUntil();

	LocalDateTime getCreatedAt();

	UUID getProductId();

	UUID getStoreId();

	String getProductTitle();

	LocalDateTime getSessionStartAt();

	String getLocationName();

	String getLocationAddress1();

	String getLocationAddress2();
}
