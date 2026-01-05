package com.popcorn.demo.domain.order.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.response.CancelOrderResponse;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.order.dto.request.CreateOrderRequest;
import com.popcorn.demo.domain.order.dto.request.PaymentCreateRequest;
import com.popcorn.demo.domain.order.dto.response.OrderCreatedDto;
import com.popcorn.demo.domain.order.dto.response.OrderPaymentCreateResponse;
import com.popcorn.demo.domain.order.dto.response.ReservationPaymentCreateResponse;
import com.popcorn.demo.domain.order.dto.request.UpdateOrderStatusRequest;
import com.popcorn.demo.domain.order.dto.response.UpdateOrderStatusResponse;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.PaymentStatus;
import com.popcorn.demo.common.versioning.ApiVersion;
import com.popcorn.demo.domain.order.exception.PaymentException;
import com.popcorn.demo.domain.order.service.PaymentCommandService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 주문 명령 전용 컨트롤러 (Command - CQRS)
 *
 * 주문 관련 쓰기 작업만 담당:
 * - 주문 생성
 * - 주문 상태 변경
 * - 주문 취소
 */
@Tag(name = "Order", description = "주문 관련 API")
@RestController
@ApiVersion("v1")
@RequestMapping("/api/v1/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderCommandController extends BaseController {

private final OrderCommandService orderCommandService;
private final ObjectMapper objectMapper;
private final PaymentCommandService paymentCommandService;

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
								  "popupId": "00000000-0000-0000-0000-000000000101",
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
								  "popupId": "00000000-0000-0000-0000-000000000101",
								  "reservationId": "00000000-0000-0000-0000-000000000601",
								  "items": [
								    {
								      "orderItemType": "GOODS",
								      "goodsVariantId": "00000000-0000-0000-0000-000000000401",
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
				.popupId(request.getPopupId())
				.orderType(request.getOrderType())
				.idempotencyKey(idempotencyKey)
				.items(request.getItems().stream()
						.map(item -> CreateOrderCommand.OrderItemCommand.builder()
								.orderItemType(OrderItemType.valueOf(item.getOrderItemType()))
								.sessionId(item.getSessionId())
								.optionId(item.getOptionId())
								.goodsVariantId(item.getGoodsVariantId())
								.qty(item.getQty())
								.unitPrice(item.getUnitPrice())
								.build())
						.toList())
				.build();

		// 유스케이스 결과를 표준 응답으로 감싸서 반환합니다.
		CreateOrderResponse response = orderCommandService.createOrder(command);
		OrderCreatedDto dto = convertToOrderCreatedDto(response);
		return created(dto);
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
							name = "주문 승인",
							summary = "주문을 승인하는 경우",
							value = """
								{
								  "status": "ACCEPTED",
								  "reason": "주문 승인"
								}
								"""
						),
						@ExampleObject(
							name = "주문 거절",
							summary = "주문을 거절하는 경우",
							value = """
								{
								  "status": "REJECTED",
								  "reason": "재고 부족으로 인한 거절"
								}
								"""
						),
						@ExampleObject(
							name = "예약 확정",
							summary = "예약이 확정되는 경우",
							value = """
								{
								  "status": "RESERVED",
								  "reason": "예약 확정"
								}
								"""
						),
						@ExampleObject(
							name = "결제 대기",
							summary = "결제 대기 상태로 전환하는 경우",
							value = """
								{
								  "status": "PAYMENT_PENDING",
								  "reason": "결제 대기"
								}
								"""
						),
						@ExampleObject(
							name = "결제 완료",
							summary = "결제 완료로 전환하는 경우",
							value = """
								{
								  "status": "PAID",
								  "reason": "결제 완료"
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
					}
				)
			)
			@Valid @RequestBody UpdateOrderStatusRequest request) {

		// 요청 바디를 디버그 로그로 남겨 운영 이슈 원인을 빠르게 추적합니다.
		logRequestDebug("주문 상태 변경 요청", request);

		// 상태 변경 규칙은 유스케이스에서 처리해 비즈니스 규칙을 보장합니다.
		Order updatedOrder = orderCommandService.updateStatus(
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
		return ok(response);
	}

	@Operation(
			summary = "예약 결제 기록 생성",
			description = "예약 주문에 대한 결제 기록을 생성합니다."
	)
	@ApiResponse(
			responseCode = "201",
			description = "예약 결제 기록 생성 성공",
			content = @Content(schema = @Schema(implementation = ReservationPaymentCreateResponse.class))
	)
	@PostMapping("/{orderId}/reservation-payments")
	public ResponseEntity<BaseResponse<ReservationPaymentCreateResponse>> createReservationPayment(
			@Parameter(description = "주문 ID", required = true, example = "00000000-0000-0000-0000-000000001003")
			@PathVariable UUID orderId,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "예약 결제 생성 요청",
				required = true,
				content = @Content(
					schema = @Schema(implementation = PaymentCreateRequest.class),
					examples = {
						@ExampleObject(
							name = "CARD 결제",
							value = """
								{
								  "method": "CARD",
								  "amount": 4000,
								  "rawPayload": {
								    "pg": "example",
								    "transactionId": "T-20250101"
								  }
								}
								"""
						),
						@ExampleObject(
							name = "EASY_PAY 결제",
							value = """
								{
								  "method": "EASY_PAY",
								  "amount": 4000
								}
								"""
						),
						@ExampleObject(
							name = "TRANSFER 결제",
							value = """
								{
								  "method": "TRANSFER",
								  "amount": 4000,
								  "rawPayload": {
								    "bank": "K-BANK",
								    "account": "123-456-7890"
								  }
								}
								"""
						)
					}
				)
			)
			@Valid @RequestBody PaymentCreateRequest request) {

		String rawPayload = toPayloadJson(request.getRawPayload());
		PaymentCommandService.PaymentCreationResult result =
				paymentCommandService.createReservationPayment(
						orderId,
						request.getMethod(),
						request.getAmount(),
						rawPayload);

		ReservationPaymentCreateResponse response = ReservationPaymentCreateResponse.builder()
				.paymentId(result.getPaymentId())
				.paymentStatus(toApiPaymentStatus(result.getPaymentStatus()))
				.orderStatus(result.getOrderStatus().name())
				.approvedAt(result.getApprovedAt())
				.build();

		return created(response);
	}

	@Operation(
			summary = "주문 결제 기록 생성",
			description = "구매형 주문에 대한 결제 기록을 생성합니다."
	)
	@ApiResponse(
			responseCode = "201",
			description = "주문 결제 기록 생성 성공",
			content = @Content(schema = @Schema(implementation = OrderPaymentCreateResponse.class))
	)
	@PostMapping("/{orderId}/payments")
	public ResponseEntity<BaseResponse<OrderPaymentCreateResponse>> createOrderPayment(
			@Parameter(description = "주문 ID", required = true, example = "00000000-0000-0000-0000-000000001004")
			@PathVariable UUID orderId,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "구매 결제 생성 요청",
				required = true,
				content = @Content(
					schema = @Schema(implementation = PaymentCreateRequest.class),
					examples = @ExampleObject(
						name = "CARD 결제",
						value = """
							{
							  "method": "CARD",
							  "amount": 3000,
							  "rawPayload": {
							    "pg": "example",
							    "transactionId": "T-20250102"
							  }
							}
							"""
					)
				)
			)
			@Valid @RequestBody PaymentCreateRequest request) {

		String rawPayload = toPayloadJson(request.getRawPayload());
		PaymentCommandService.PaymentCreationResult result =
				paymentCommandService.createOrderPayment(
						orderId,
						request.getMethod(),
						request.getAmount(),
						rawPayload);

		OrderPaymentCreateResponse response = OrderPaymentCreateResponse.builder()
				.paymentId(result.getPaymentId())
				.status(toApiPaymentStatus(result.getPaymentStatus()))
				.orderStatus(result.getOrderStatus().name())
				.build();

		return created(response);
	}

	@Operation(
			summary = "주문 취소",
			description = "CUSTOMER가 본인 주문을 취소합니다."
	)
	@ApiResponse(
			responseCode = "200",
			description = "주문 취소 성공",
			content = @Content(schema = @Schema(implementation = CancelOrderResponse.class))
	)
	@ApiResponse(responseCode = "400", description = "취소할 수 없는 상태")
	@ApiResponse(responseCode = "403", description = "권한 없음")
	@ApiResponse(responseCode = "404", description = "주문 없음")
	@DeleteMapping("/{orderId}/cancel")
	public ResponseEntity<BaseResponse<CancelOrderResponse>> cancelOrder(
			@Parameter(
					description = "주문 ID",
					required = true,
					example = "00000000-0000-0000-0000-000000001001"
			)
			@PathVariable UUID orderId) {

		// 주문을 CANCELLED 상태로 변경
		Order cancelledOrder = orderCommandService.updateStatus(
				orderId,
				OrderStatus.CANCELLED.name(),
				"고객 요청에 의한 취소"
		);

		// 응답 DTO 생성
		CancelOrderResponse response = CancelOrderResponse.builder()
				.id(cancelledOrder.getId())
				.status("CANCELED")  // 사용자 스펙에 맞게 "CANCELED" 사용
				.build();

		return ok(response);
	}

	@Operation(
			summary = "모든 주문 데이터 삭제 (개발/테스트용)",
			description = "⚠️ 경고: 모든 주문 관련 데이터를 삭제합니다. 개발 및 테스트 환경에서만 사용하세요."
	)
	@ApiResponse(
			responseCode = "200",
			description = "모든 주문 데이터 삭제 완료"
	)
	@DeleteMapping("/all")
	public ResponseEntity<BaseResponse<String>> deleteAllOrders() {
		log.warn("🚨 모든 주문 데이터 삭제 요청");
		orderCommandService.deleteAllOrders();
		return ok("모든 주문 데이터가 삭제되었습니다.");
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
				response.getPopupId(),
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

	private String toPayloadJson(Object payload) {
		if (payload == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException ex) {
			throw PaymentException.invalidRequest();
		}
	}

	private String toApiPaymentStatus(PaymentStatus status) {
		return status == PaymentStatus.PAID ? "PAID" : "FAILED";
	}
}
