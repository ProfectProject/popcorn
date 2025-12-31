package com.popcorn.demo.application.order.event;

import com.popcorn.demo.domain.order.entity.Order;

public class OrderCreatedEvent {

	private final Order order;



	public OrderCreatedEvent(Order order) {

		this.order = order;

	}



	public Order getOrder() {

		return order;

	}

}

