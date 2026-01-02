package com.popcorn.demo.domain.order.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.service.OrderService;
import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.order.dto.request.CreateOrderRequest;
import com.popcorn.demo.domain.order.dto.response.OrderCreatedDto;
import com.popcorn.demo.domain.order.dto.request.UpdateOrderStatusRequest;
import com.popcorn.demo.domain.order.dto.response.UpdateOrderStatusResponse;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItemType;

import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Orders", description = "주문 관리 API")
@RestController
@RequestMapping("/api/v1/orders")
@Slf4j
public class OrderController extends BaseController {

	private final OrderService orderService;
private final ObjectMapper objectMapper;



public OrderController(OrderService orderService, ObjectMapper objectMapper) {
	this.orderService = orderService;
	this.objectMapper = objectMapper;
}



	/**

		* 새로운 주문을 생성합니다.

		*

		* @param userId 주문 생성 사용자 ID

		* @param request 주문 생성 요청 데이터

		* @param idempotencyKey 멱등성을 위한 키 (선택)

		* @return 생성된 주문 정보

		*/

	@Operation(
			summary = "주문 생성",
			description = "새로운 주문을 생성합니다. 예약형(RESERVATION) 또는 구매형(PURCHASE) 주문을 지원합니다."
	)
	@ApiResponse(
		responseCode = "201",
		description = "주문 생성 성공",
		content = @Content(schema = @Schema(implementation = OrderCreatedDto.class))
	)
	@ApiResponse(
		responseCode = "400",
		description = "잘못된 요청 (필수값 누락, 형식 오류, 비즈니스 검증 실패)"
	)
	@ApiResponse(
		responseCode = "404",
		description = "리소스 없음 (스토어, 상품, 세션, 옵션, 굿즈변형)"
	)
	@ApiResponse(
		responseCode = "409",
		description = "비즈니스 규칙 위반 (재고부족, 정원초과, 상태오류 등)"
	)

	@PostMapping("/{userId}")

	public ResponseEntity<BaseResponse<OrderCreatedDto>> createOrder(

			@Parameter(description = "주문 생성 사용자 ID", required = true, example = "1001")

			@PathVariable Long userId,



			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "주문 생성 요청 데이터",
				required = true,
				content = @Content(
					schema = @Schema(implementation = CreateOrderRequest.class),
					examples = {
						@ExampleObject(
							name = "예약형 주문",
							summary = "예약 세션 + 옵션 기반 주문",
							value = """
								{
								  "orderType": "RESERVATION",
								  "storeId": "00000000-0000-0000-0000-000000000001",
								  "productId": "00000000-0000-0000-0000-000000000101",
								  "items": [
								    {
								      "orderItemType": "RESERVATION",
								      "sessionId": "00000000-0000-0000-0000-000000000201",
								      "optionId": "00000000-0000-0000-0000-000000000301",
								      "qty": 2
								    }
								  ]
								}
								"""
						),
						@ExampleObject(
							name = "구매형 주문",
							summary = "굿즈 구매 + 배송지 포함",
							value = """
								{
								  "orderType": "PURCHASE",
								  "storeId": "00000000-0000-0000-0000-000000000001",
								  "productId": "00000000-0000-0000-0000-000000000101",
								  "reservationId": "00000000-0000-0000-0000-000000000601",
								  "items": [
								    {
								      "orderItemType": "MERCH",
								      "merchVariantId": "00000000-0000-0000-0000-000000000401",
								      "qty": 2
								    }
								  ],
								  "address": {
								    "address1": "서울특별시 강남구 테헤란로 123",
								    "address2": "ABC빌딩 12층 1201호",
								    "receiverName": "홍길동",
								    "phone": "010-1234-5678"
								  }
								}
								"""
						)
					}
				)
			)
			@Valid @RequestBody CreateOrderRequest request,



			@Parameter(hidden = true)

			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {



	logRequestDebug("주문 생성 요청", request);
	// 요청 DTO를 Command로 변환해 유스케이스에 전달합니다.

	CreateOrderCommand command = CreateOrderCommand.builder()

				.userId(userId)

				.storeId(request.getStoreId())

				.productId(request.getProductId())

				.orderType(request.getOrderType())

				.idempotencyKey(idempotencyKey)

				.items(request.getItems().stream()

						.map(item -> CreateOrderCommand.OrderItemCommand.builder()

								.orderItemType(OrderItemType.valueOf(item.getOrderItemType()))

								.sessionId(item.getSessionId())

								.optionId(item.getOptionId())

								.merchVariantId(item.getMerchVariantId())

								.qty(item.getQty())

								.unitPrice(item.getUnitPrice())

								.build())

						.toList())

				.build();



		// 유스케이스 결과를 표준 응답으로 감싸서 반환합니다.
		CreateOrderResponse response = orderService.createOrder(command);
		OrderCreatedDto dto = convertToOrderCreatedDto(response);
		return new ResponseEntity<>(BaseResponse.success(dto), HttpStatus.CREATED);

	}



	/**

		* CreateOrderResponse를 OrderCreatedDto로 변환하는 헬퍼 메서드

		* Clean Architecture의 Response를 Controller Layer의 DTO로 변환

		*/

	private OrderCreatedDto convertToOrderCreatedDto(CreateOrderResponse response) {

		return new OrderCreatedDto(

				response.getOrderId(),

				response.getOrderNo(),

				response.getOrderType(),

				response.getStatus(),

				response.getStoreId(),

				response.getProductId(),

				response.getTotalAmount(),

				response.getCancelableUntil(),

				response.getCreatedAt(),

				response.getItems().stream()

						.map(item -> new OrderCreatedDto.OrderItemDto(

								item.getItemId(),

								item.getOrderItemType(),

								item.getQty(),

								item.getUnitPrice(),

								item.getLineAmount()

						))

						.toList()

		);

	}

	@Operation(
			summary = "주문 상태 변경",
			description = "운영(OWNER/MANAGER)에서 주문 상태를 변경합니다."
	)
	@ApiResponse(
		responseCode = "200",
		description = "주문 상태 변경 성공",
		content = @Content(schema = @Schema(implementation = UpdateOrderStatusResponse.class))
	)
	@ApiResponse(responseCode = "400", description = "잘못된 요청 또는 상태 변경 불가")
	@ApiResponse(responseCode = "403", description = "권한 없음")
	@ApiResponse(responseCode = "404", description = "주문 없음")
	@ApiResponse(responseCode = "409", description = "이미 취소된 주문")
	@PatchMapping("/{orderId}/status")
	public ResponseEntity<BaseResponse<UpdateOrderStatusResponse>> updateOrderStatus(
			@Parameter(
				description = "주문 ID",
				required = true,
				example = "00000000-0000-0000-0000-000000001001"
			)
			@PathVariable UUID orderId,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "주문 상태 변경 요청",
				required = true,
				content = @Content(
					schema = @Schema(implementation = UpdateOrderStatusRequest.class),
					examples = {
						@ExampleObject(
							name = "운영 승인",
							summary = "운영자가 주문을 승인하는 경우",
							value = """
								{
								  "status": "OWNER_ACCEPTED",
								  "reason": "운영 승인"
								}
								"""
						),
						@ExampleObject(
							name = "운영 거절",
							summary = "운영자가 주문을 거절하는 경우",
							value = """
								{
								  "status": "OWNER_REJECTED",
								  "reason": "재고 부족으로 인한 거절"
								}
								"""
						),
						@ExampleObject(
							name = "주문 확인",
							summary = "주문이 확인되는 경우",
							value = """
								{
								  "status": "CONFIRMED",
								  "reason": "주문 확인 완료"
								}
								"""
						),
						@ExampleObject(
							name = "준비 중",
							summary = "주문 준비를 시작하는 경우",
							value = """
								{
								  "status": "PREPARING",
								  "reason": "주문 준비 시작"
								}
								"""
						),
						@ExampleObject(
							name = "준비 완료",
							summary = "주문 준비가 완료된 경우",
							value = """
								{
								  "status": "READY",
								  "reason": "주문 준비 완료"
								}
								"""
						),
						@ExampleObject(
							name = "완료",
							summary = "주문이 완료된 경우",
							value = """
								{
								  "status": "COMPLETED",
								  "reason": "서비스 완료"
								}
								"""
						),
						@ExampleObject(
							name = "취소",
							summary = "주문을 취소하는 경우",
							value = """
								{
								  "status": "CANCELLED",
								  "reason": "고객 요청에 의한 취소"
								}
								"""
						),
						@ExampleObject(
							name = "환불",
							summary = "주문을 환불하는 경우",
							value = """
								{
								  "status": "REFUNDED",
								  "reason": "결제 환불 처리"
								}
								"""
						)
					}
				)
			)
			@Valid @RequestBody UpdateOrderStatusRequest request) {

		// 요청 바디를 디버그 로그로 남겨 운영 이슈 원인을 빠르게 추적합니다.
		logRequestDebug("주문 상태 변경 요청", request);
		// 상태 변경 규칙은 유스케이스에서 처리해 비즈니스 규칙을 보장합니다.
		Order updatedOrder = orderService.updateStatus(
					orderId,
					request.getStatus(),
					request.getReason()
				);
		// 도메인 엔티티를 응답 DTO로 변환해 필요한 필드만 전달합니다.
		UpdateOrderStatusResponse response = UpdateOrderStatusResponse.builder()
				.id(updatedOrder.getId())
				.status(updatedOrder.getStatus().name())
				.updatedAt(updatedOrder.getUpdatedAt())
				.build();
		return ResponseEntity.ok(BaseResponse.success(response));
	}
	private void logRequestDebug(String label, Object request) {
		if (!log.isDebugEnabled()) {
			return;
		}
		try {
			log.debug("{}: {}", label, objectMapper.writeValueAsString(request));
		} catch (JsonProcessingException ex) {
			log.debug("{}: <failed to serialize request>", label, ex);
		}
	}

}
