package com.popcorn.demo.application.order.event;

import com.popcorn.demo.domain.order.entity.Order;

public record OrderCreatedEvent(Order order) {
}
