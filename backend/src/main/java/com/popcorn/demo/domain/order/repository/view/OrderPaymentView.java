package com.popcorn.demo.domain.order.repository.view;

import java.time.LocalDateTime;
import java.util.UUID;

public interface OrderPaymentView {

	UUID getPaymentId();

	String getMethod();

	String getStatus();

	Integer getAmount();

	LocalDateTime getApprovedAt();
}
