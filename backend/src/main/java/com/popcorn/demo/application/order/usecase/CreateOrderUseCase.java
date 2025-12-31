package com.popcorn.demo.application.order.usecase;

import java.util.List;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	@Transactional

	public CreateOrderResponse createOrder(CreateOrderCommand command) {

		log.info("🎯 주문 생성 시작 - 사용자: {}, 멱등성키: {}", command.getUserId(), command.getIdempotencyKey());



		// 1. 멱등성 검증 (중복 주문 방지)

		String idempotencyKey = normalizeIdempotencyKey(command.getIdempotencyKey());

		if (idempotencyKey != null) {

			if (idempotencyCache.isDuplicate(idempotencyKey)) {

				log.warn("⚠️ 캐시 중복 주문 감지 - 멱등성키: {}", idempotencyKey);

				throw OrderException.duplicateIdempotencyKey();

			}

			Optional<Order> existingOrder = findOrderPort.findByIdempotencyKey(command.getIdempotencyKey());

			if (orderDomainService.isDuplicateOrder(existingOrder, command.getIdempotencyKey())) {

				log.warn("⚠️ 중복 주문 요청 - 멱등성키: {}", command.getIdempotencyKey());

				idempotencyCache.mark(idempotencyKey);

				throw OrderException.duplicateIdempotencyKey();

			}

		}



		// 2. Command → Domain 객체 변환

		List<OrderItem> orderItems = convertToOrderItems(command.getItems());

		OrderType orderType = OrderType.valueOf(command.getOrderType());



		// 3. 검증 병렬 실행 (재고/유저/상품)

		int totalQty = calculateTotalQuantity(orderItems);

		try {

			boolean validationResult = processOrderPort.validateOrder(

					command.getUserId(),

					command.getProductId(),

					totalQty

			).join();

			if (!validationResult) {

				throw OrderException.invalidRequest();

			}

		} catch (RuntimeException ex) {

			log.error("❌ 검증 실패 - 사용자: {}, 상품: {}", command.getUserId(), command.getProductId());

			throw ex;

		}



		// 4. 도메인 서비스를 통한 주문 생성 (비즈니스 로직)

		Order order = orderDomainService.createOrder(

				command.getUserId(),

				command.getStoreId(),

				command.getProductId(),

				orderType,

				orderItems,

				command.getIdempotencyKey()

		);



		// 5. 주문 저장 (Infrastructure Layer)

		Order savedOrder = saveOrderPort.save(order);

		saveOrderPort.saveOrderItems(savedOrder.getOrderItems());

		log.info("💾 주문 저장 완료 - 주문번호: {}, ID: {}", savedOrder.getOrderNo(), savedOrder.getId());



		if (idempotencyKey != null) {

			idempotencyCache.mark(idempotencyKey);

		}



		// 6. 주문 생성 이벤트 발행 (AFTER_COMMIT 비동기 후처리)

		eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder));



		// 7. 응답 객체 생성

		CreateOrderResponse response = CreateOrderResponse.fromOrder(savedOrder);

		log.info("✅ 주문 생성 완료 - 주문번호: {}, 사용자: {}", response.getOrderNo(), command.getUserId());



		return response;

	}



	/**

		* Command의 주문 항목을 Domain 주문 항목으로 변환

		*/

	private List<OrderItem> convertToOrderItems(List<CreateOrderCommand.OrderItemCommand> itemCommands) {

		return itemCommands.stream()

				.map(this::convertToOrderItem)

				.toList();

	}



	/**

		* 개별 주문 항목 변환

		*/

	private OrderItem convertToOrderItem(CreateOrderCommand.OrderItemCommand itemCommand) {
		Integer unitPrice = resolveUnitPrice(itemCommand);
		Integer lineAmount = unitPrice * itemCommand.getQty();

		return OrderItem.builder()

				.orderItemType(itemCommand.getOrderItemType())

				.qty(itemCommand.getQty())

				.unitPrice(unitPrice)

				.lineAmount(lineAmount)

				.sessionOptionId(itemCommand.getOptionId())

				.merchVariantId(itemCommand.getMerchVariantId())

				.build();

	}



	private Integer resolveUnitPrice(CreateOrderCommand.OrderItemCommand itemCommand) {
		OrderItemType orderItemType = itemCommand.getOrderItemType();
		if (OrderItemType.RESERVATION.equals(orderItemType)) {
			Long optionId = itemCommand.getOptionId();
			if (optionId == null || optionId <= 0) {
				throw OrderException.optionNotFound();
			}
			return findOrderItemPricePort.findSessionOptionPrice(optionId)
					.orElseThrow(OrderException::optionNotFound);
		}
		if (OrderItemType.MERCH.equals(orderItemType)) {
			Long merchVariantId = itemCommand.getMerchVariantId();
			if (merchVariantId == null || merchVariantId <= 0) {
				throw OrderException.merchVariantNotFound();
			}
			return findOrderItemPricePort.findMerchVariantPrice(merchVariantId)
					.orElseThrow(OrderException::merchVariantNotFound);
		}
		throw OrderException.invalidRequest();
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
