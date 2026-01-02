package com.popcorn.demo.application.order.usecase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.popcorn.demo.application.order.event.OrderCreatedEvent;
import com.popcorn.demo.application.order.port.in.CreateOrderCommand;
import com.popcorn.demo.application.order.port.in.CreateOrderResponse;
import com.popcorn.demo.application.order.port.out.FindOrderItemPricePort;
import com.popcorn.demo.application.order.port.out.FindOrderPort;
import com.popcorn.demo.application.order.port.out.ProcessOrderPort;
import com.popcorn.demo.application.order.port.out.SaveOrderPort;
import com.popcorn.demo.common.cache.IdempotencyCache;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.service.OrderDomainService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**

	* 주문 생성 유스케이스 (Application Service)

	*

	* Clean Architecture의 Application Layer에 해당합니다.

	* - 비즈니스 흐름 조율

	* - 도메인 서비스 호출

	* - 외부 시스템 연동 (Port 사용)

	* - 트랜잭션 관리

	*/

@Service

@RequiredArgsConstructor

@Slf4j

public class CreateOrderUseCase {



	private final OrderDomainService orderDomainService;

	private final FindOrderPort findOrderPort;

	private final SaveOrderPort saveOrderPort;

	private final ProcessOrderPort processOrderPort;

	private final FindOrderItemPricePort findOrderItemPricePort;

	private final IdempotencyCache idempotencyCache;

	private final ApplicationEventPublisher eventPublisher;



	/**

		* 주문을 생성합니다.

		*

		* @param command 주문 생성 명령

		* @return 주문 생성 응답

		*/

	@Transactional(transactionManager = "connectionFactoryTransactionManager")

	public Mono<CreateOrderResponse> createOrder(CreateOrderCommand command) {

		log.info("🎯 주문 생성 시작 - 사용자: {}, 멱등성키: {}", command.getUserId(), command.getIdempotencyKey());

		String idempotencyKey = normalizeIdempotencyKey(command.getIdempotencyKey());
		// 멱등성 키 중복 요청을 선제 차단합니다.
		Mono<Void> idempotencyCheck = checkIdempotency(command.getIdempotencyKey(), idempotencyKey);
		// 요청 아이템을 도메인 모델로 변환해 이후 검증/저장에 사용합니다.
		Mono<List<OrderItem>> orderItemsMono = convertToOrderItems(command.getItems()).collectList();

		return idempotencyCheck
				.then(orderItemsMono)
				.flatMap(orderItems -> {
					OrderType orderType = OrderType.valueOf(command.getOrderType());
					int totalQty = calculateTotalQuantity(orderItems);

					// 외부/비동기 검증 결과까지 반영해 주문 생성 가능 여부를 확정합니다.
					return processOrderPort.validateOrder(command.getUserId(), command.getProductId(), totalQty)
							.flatMap(result -> {
								if (!result) {
									return Mono.error(OrderException.invalidRequest());
								}
								// 도메인 규칙 검증은 생성 전에 한번 더 수행합니다.
								orderDomainService.validateOrderCreation(
										command.getUserId(),
										command.getStoreId(),
										command.getProductId(),
										orderItems
								);

								Order order = orderDomainService.createOrder(
										command.getUserId(),
										command.getStoreId(),
										command.getProductId(),
										orderType,
										orderItems,
										command.getIdempotencyKey()
								);

								// 주문 저장 후, 주문 아이템/이벤트 처리까지 이어집니다.
								return saveOrderPort.save(order)
										.flatMap(savedOrder -> {
											if (idempotencyKey != null) {
												idempotencyCache.mark(idempotencyKey);
											}

											List<OrderItem> itemsWithOrderId = savedOrder.getOrderItems().stream()
													.peek(item -> item.setOrderId(savedOrder.getId()))
													.toList();

											return saveOrderPort.saveOrderItems(itemsWithOrderId)
													.then(Mono.fromRunnable(() -> eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder))))
													.thenReturn(CreateOrderResponse.fromOrder(savedOrder));
										})
										.doOnSuccess(response ->
												log.info("✅ 주문 생성 완료 - 주문번호: {}, 사용자: {}", response.getOrderNo(), command.getUserId()));
							});
				});

	}



	private Mono<Void> checkIdempotency(String rawKey, String normalizedKey) {
		if (normalizedKey == null) {
			return Mono.empty();
		}
		if (idempotencyCache.isDuplicate(normalizedKey)) {
			log.warn("⚠️ 캐시 중복 주문 감지 - 멱등성키: {}", normalizedKey);
			return Mono.error(OrderException.duplicateIdempotencyKey());
		}
		return findOrderPort.findByIdempotencyKey(rawKey)
				.flatMap(existingOrder -> {
					if (orderDomainService.isDuplicateOrder(Optional.of(existingOrder), rawKey)) {
						log.warn("⚠️ 중복 주문 요청 - 멱등성키: {}", rawKey);
						idempotencyCache.mark(normalizedKey);
						return Mono.error(OrderException.duplicateIdempotencyKey());
					}
					return Mono.empty();
				})
				.then();
	}



	/**

		* Command의 주문 항목을 Domain 주문 항목으로 변환

		*/

	private Flux<OrderItem> convertToOrderItems(List<CreateOrderCommand.OrderItemCommand> itemCommands) {

		return Flux.fromIterable(itemCommands)
				.flatMap(this::convertToOrderItem);

	}



	/**

		* 개별 주문 항목 변환

		*/

	private Mono<OrderItem> convertToOrderItem(CreateOrderCommand.OrderItemCommand itemCommand) {
		return resolveUnitPrice(itemCommand)
				.map(unitPrice -> {
					Integer lineAmount = unitPrice * itemCommand.getQty();
					return OrderItem.builder()
							.id(java.util.UUID.randomUUID())
							.orderItemType(itemCommand.getOrderItemType())
							.qty(itemCommand.getQty())
							.unitPrice(unitPrice)
							.lineAmount(lineAmount)
							.sessionOptionId(itemCommand.getOptionId())
							.merchVariantId(itemCommand.getMerchVariantId())
							.build();
				});

	}



	private Mono<Integer> resolveUnitPrice(CreateOrderCommand.OrderItemCommand itemCommand) {
		OrderItemType orderItemType = itemCommand.getOrderItemType();
		if (OrderItemType.RESERVATION.equals(orderItemType)) {
			UUID optionId = itemCommand.getOptionId();
			if (optionId == null) {
				return Mono.error(OrderException.optionNotFound());
			}
			return findOrderItemPricePort.findSessionOptionPrice(optionId)
					.switchIfEmpty(Mono.error(OrderException.optionNotFound()));
		}
		if (OrderItemType.MERCH.equals(orderItemType)) {
			UUID merchVariantId = itemCommand.getMerchVariantId();
			if (merchVariantId == null) {
				return Mono.error(OrderException.merchVariantNotFound());
			}
			return findOrderItemPricePort.findMerchVariantPrice(merchVariantId)
					.switchIfEmpty(Mono.error(OrderException.merchVariantNotFound()));
		}
		return Mono.error(OrderException.invalidRequest());
	}



	private String normalizeIdempotencyKey(String idempotencyKey) {

		if (idempotencyKey == null) {

			return null;

		}

		String trimmed = idempotencyKey.trim();

		return trimmed.isEmpty() ? null : trimmed;

	}



	private int calculateTotalQuantity(List<OrderItem> orderItems) {

		return orderItems.stream()

				.mapToInt(OrderItem::getQty)

				.sum();

	}

}
