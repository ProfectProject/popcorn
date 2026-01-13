package com.popcorn.demo.domain.order.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.popcorn.demo.domain.auth.dto.CustomUserDetails;
import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.response.CancelOrderResponse;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.domain.order.service.OrderPaymentFacade;
import com.popcorn.demo.domain.payment.service.PaymentCommandService;
import com.popcorn.demo.domain.payment.service.PaymentTokenService;
import com.popcorn.demo.domain.payment.toss.TossPaymentsProperties;
import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.order.dto.request.CreateOrderRequest;
import com.popcorn.demo.domain.order.dto.response.OrderCreatedDto;
import com.popcorn.demo.domain.order.dto.request.UpdateOrderStatusRequest;
import com.popcorn.demo.domain.order.dto.response.UpdateOrderStatusResponse;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.common.versioning.ApiVersion;

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
private final OrderPaymentFacade orderPaymentFacade;
private final ObjectMapper objectMapper;
private final TossPaymentsProperties tossPaymentsProperties;
private final PaymentTokenService paymentTokenService;

	/**
	 * 새로운 주문을 생성합니다.
	 *
	 * @param userId 주문 생성 사용자 ID
	 * @param request 주문 생성 요청 데이터
	 * @return 생성된 주문 정보
	 */
	@Operation(
			summary = "주문 생성",
			description = """
				새로운 주문을 생성합니다. 예약형(RESERVATION) 또는 구매형(PURCHASE) 주문을 지원합니다.

				**주요 기능:**
				- 예약형 주문: 팝업 세션 예약 (시간 지정 방문)
				- 구매형 주문: 굿즈 구매 (배송 또는 현장 픽업)
				- 실시간 재고 및 정원 체크
				- 자동 가격 계산 (세션/굿즈별 단가 기준)
				- 중복 주문 방지

				**주문 플로우:**
				1. REQUESTED → 2. ACCEPTED/REJECTED → 3. RESERVED/PAYMENT_PENDING → 4. PAID → 5. COMPLETED

				**사용 예시:**
				- 예약형: POST /api/v1/orders (세션 기반 예약)
				- 구매형: POST /api/v1/orders (굿즈 구매 + 배송지)
				"""
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
	@PostMapping
	public ResponseEntity<BaseResponse<OrderCreatedDto>> createOrder(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
				description = "주문 생성 요청 데이터",
				required = true,
				content = @Content(
					schema = @Schema(implementation = CreateOrderRequest.class),
					examples = {
						@ExampleObject(
							name = "예약형 주문",
							summary = "팝업 세션 예약 (2025-01-15 10:00-12:00)",
							value = """
								{
								  "orderType": "RESERVATION",
								  "popupId": "00000000-0000-0000-0000-000000000101",
								  "items": [
								    {
								      "orderItemType": "RESERVATION",
								      "sessionId": "00000000-0000-0000-0000-000000000201",
								      "qty": 2
								    }
								  ]
								}
								"""
						),
						@ExampleObject(
							name = "굿즈 구매형 주문",
							summary = "팝업 기념품 구매 (25,000원)",
							value = """
								{
								  "orderType": "PURCHASE",
								  "popupId": "00000000-0000-0000-0000-000000000101",
								  "items": [
								    {
								      "orderItemType": "GOODS",
								      "goodsVariantId": "00000000-0000-0000-0000-000000000301",
								      "qty": 1
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
						),
						@ExampleObject(
							name = "복합 주문 (예약+굿즈)",
							summary = "세션 예약과 굿즈 구매를 함께",
							value = """
								{
								  "orderType": "RESERVATION",
								  "popupId": "00000000-0000-0000-0000-000000000101",
								  "items": [
								    {
								      "orderItemType": "RESERVATION",
								      "sessionId": "00000000-0000-0000-0000-000000000201",
								      "qty": 1
								    },
								    {
								      "orderItemType": "GOODS",
								      "goodsVariantId": "00000000-0000-0000-0000-000000000301",
								      "qty": 1
								    }
								  ]
								}
								"""
						)
					}
				)
			)
			@Valid @RequestBody CreateOrderRequest request,
			Authentication authentication) {

		logRequestDebug("주문 생성 요청", request);

		// JWT에서 현재 인증된 사용자 정보 추출
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		Long userId = userDetails.getUserId();

		// 요청 DTO를 Command로 변환해 유스케이스에 전달합니다.
		CreateOrderCommand command = CreateOrderCommand.builder()
				.userId(userId)
				.popupId(request.getPopupId())
				.orderType(request.getOrderType())
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
		OrderPaymentFacade.OrderWithPaymentResult result = orderPaymentFacade.createOrderWithPayment(
				command,
				request.getPaymentMethod());
		OrderCreatedDto dto = convertToOrderCreatedDto(
				result.getOrderResponse(),
				result.getPaymentResult());
		return created(dto);
	}

	@Operation(
			summary = "주문 상태 변경",
			description = """
				운영자(OWNER/MANAGER)가 주문 상태를 변경합니다.

				**상태 전이 규칙:**
				- REQUESTED → ACCEPTED, REJECTED
				- ACCEPTED → RESERVED, PAYMENT_PENDING
				- RESERVED → PAID, CANCELLED
				- PAYMENT_PENDING → PAID, CANCELLED
				- PAID → COMPLETED, CANCELLED
				- COMPLETED → (최종 상태)
				- CANCELLED → (최종 상태)

				**주요 기능:**
				- 상태 전이 유효성 검증
				- 상태 변경 이력 자동 저장
				- 이벤트 기반 알림 발송
				- 취소 시 환불 로직 연동

				**권한:**
				- OWNER: 본인 스토어 주문만 관리
				- MANAGER: 권한 범위 내 주문 관리
				"""
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
			summary = "주문 취소",
			description = """
				고객(CUSTOMER)이 본인 주문을 취소합니다.

				**취소 가능 조건:**
				- 주문 상태가 CANCELLED가 아님
				- cancelableUntil 시간이 지나지 않음
				- 본인의 주문만 취소 가능

				**취소 처리:**
				- 주문 상태를 CANCELLED로 변경
				- 결제 금액 환불 처리 (별도 프로세스)
				- 예약 세션 정원 복구
				- 굿즈 재고 복구

				**주의사항:**
				- 취소 후에는 되돌릴 수 없음
				- 환불 처리는 3-5영업일 소요
				- 부분 취소는 지원하지 않음
				"""
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
			@PathVariable UUID orderId,
			Authentication authentication) {

		// JWT에서 현재 인증된 사용자 정보 추출
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		Long userId = userDetails.getUserId();
		String role = userDetails.getRole();

		// 주문을 CANCELLED 상태로 변경 (권한 검증은 서비스 레이어에서 처리)
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
	private OrderCreatedDto convertToOrderCreatedDto(
			CreateOrderResponse response,
			PaymentCommandService.PaymentCreationResult paymentResult) {
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
						.toList(),
				paymentResult != null ? paymentResult.getPaymentId() : null,
				paymentResult != null ? paymentResult.getAmount() : null,
				buildCheckoutUrl(response, paymentResult),
				toCustomerKey(paymentResult != null ? paymentResult.getCustomerId() : null),
				tossPaymentsProperties.getSuccessUrl(),
				tossPaymentsProperties.getFailUrl()
		);
	}

	private String buildCheckoutUrl(
			CreateOrderResponse response,
			PaymentCommandService.PaymentCreationResult paymentResult) {
		if (response == null || paymentResult == null) {
			return null;
		}
		String checkoutUrl = tossPaymentsProperties.getCheckoutUrl();
		if (checkoutUrl == null || checkoutUrl.isBlank()) {
			return null;
		}

		// JWT 토큰으로 결제 정보 암호화
		String paymentToken = paymentTokenService.createPaymentToken(
				PaymentTokenService.PaymentTokenInfo.builder()
						.orderNo(response.getOrderNo())
						.amount(paymentResult.getAmount())
						.customerKey(toCustomerKey(paymentResult.getCustomerId()))
						.paymentId(paymentResult.getPaymentId())
						.successUrl(tossPaymentsProperties.getSuccessUrl())
						.failUrl(tossPaymentsProperties.getFailUrl())
						.build());

		// 암호화된 토큰만 포함한 안전한 URL 생성
		return UriComponentsBuilder.fromHttpUrl(checkoutUrl)
				.queryParam("token", paymentToken)
				.build(true)
				.toUriString();
	}

	private String toCustomerKey(Long customerId) {
		if (customerId == null) {
			return "guest";
		}
		return customerId.toString();
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
