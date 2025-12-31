package com.popcorn.demo.infrastructure.persistence.order;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.popcorn.demo.application.order.port.out.FindOrderPort;
import com.popcorn.demo.application.order.port.out.SaveOrderPort;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.repository.OrderSummaryView;

import lombok.RequiredArgsConstructor;

/**

	* 주문 저장소 어댑터 (Infrastructure Layer)

	*

	* Clean Architecture의 Adapter 패턴을 구현합니다.

	* - Application Layer의 FindOrderPort, SaveOrderPort를 구현

	* - Domain Layer의 OrderRepository와 연결하는 브릿지 역할

	* - 데이터베이스 접근을 담당

	*/

@Component

@RequiredArgsConstructor

public class OrderRepositoryAdapter implements FindOrderPort, SaveOrderPort {



	private final OrderRepository orderRepository;



	@Override

	public Optional<Order> findByIdempotencyKey(String idempotencyKey) {

		if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {

			return Optional.empty();

		}

		return orderRepository.findByIdempotencyKey(idempotencyKey);

	}



	@Override

	public Optional<Order> findById(Long orderId) {

		return orderRepository.findById(orderId);

	}



	@Override

	public Optional<OrderSummaryView> findSummaryById(Long orderId) {

		return orderRepository.findSummaryById(orderId);

	}



	@Override

	public Order save(Order order) {

		return orderRepository.save(order);

	}

}

