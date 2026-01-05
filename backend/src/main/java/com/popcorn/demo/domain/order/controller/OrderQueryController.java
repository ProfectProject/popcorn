package com.popcorn.demo.domain.order.controller;

import java.util.UUID;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.order.dto.response.MyOrderTimelineResponse;
import com.popcorn.demo.domain.order.dto.response.OrderDetailDto;
import com.popcorn.demo.domain.order.dto.response.OrderStatusDto;
import com.popcorn.demo.domain.order.dto.response.StoreOrderReservationListResponse;
import com.popcorn.demo.domain.order.service.OrderQueryService;
import com.popcorn.demo.common.versioning.ApiVersion;
import com.popcorn.demo.domain.order.entity.OrderStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 주문 조회 전용 컨트롤러 (Query - CQRS)
 *
 * 주문 관련 읽기 작업만 담당:
 * - 주문 상세 조회
 * - 내 주문 목록 조회
 * - 가게 주문 목록 조회
 */
@Tag(name = "Order", description = "주문 관련 API")
@RestController
@ApiVersion("v1")
@RequestMapping("/api/v1/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderQueryController extends BaseController {

	private final OrderQueryService orderQueryService;

	@Operation(
			summary = "주문 상세 조회",
			description = "주문 상세 정보를 조회합니다. CUSTOMER는 본인 주문만 조회할 수 있습니다."
	)
	@ApiResponse(
			responseCode = "200",
			description = "주문 상세 조회 성공",
			content = @Content(schema = @Schema(implementation = OrderDetailDto.class))
	)
	@ApiResponse(responseCode = "403", description = "권한 없음")
	@ApiResponse(responseCode = "404", description = "주문 없음")
	@GetMapping("/{orderId}")
	public ResponseEntity<BaseResponse<OrderDetailDto>> getOrderDetail(
			@Parameter(
					description = "주문 ID",
					required = true,
					example = "00000000-0000-0000-0000-000000001001"
			)
			@PathVariable UUID orderId) {

		OrderDetailDto detail = orderQueryService.getOrderDetail(orderId, null, null);
		return ok(detail);
	}

	@Operation(
			summary = "주문/예약 상태 조회 (CUSTOMER)",
			description = "결제 직후 상태 갱신용으로 주문 상태를 조회합니다."
	)
	@ApiResponse(
			responseCode = "200",
			description = "주문 상태 조회 성공",
			content = @Content(
					schema = @Schema(implementation = OrderStatusDto.class),
					examples = @ExampleObject(value = """
							{
							  "code": 200,
							  "message": "요청이 성공했습니다.",
							  "data": {
							    "orderId": "00000000-0000-0000-0000-000000001001",
							    "orderNo": "O20251231-001001",
							    "status": "REQUESTED",
							    "paymentStatus": "READY",
							    "cancelableUntil": "2025-01-01T10:30:00",
							    "updatedAt": "2025-01-01T10:05:00"
							  }
							}
							""")
			)
	)
	@ApiResponse(responseCode = "404", description = "주문 없음")
	@GetMapping("/{orderId}/status")
	public ResponseEntity<BaseResponse<OrderStatusDto>> getOrderStatus(
			@Parameter(description = "주문 ID", required = true,
					example = "00000000-0000-0000-0000-000000001001")
			@PathVariable UUID orderId,
			@Parameter(hidden = true)
			@RequestParam(required = false) Long customerId) {

		OrderStatusDto response = orderQueryService.getOrderStatusForCustomer(orderId, customerId);
		return ok(response);
	}

	@Operation(
			summary = "내 가게 주문/예약 목록",
			description = "OWNER/MANAGER가 가게 기준으로 주문/예약 목록을 조회합니다."
	)
	@ApiResponse(
			responseCode = "200",
			description = "가게 주문 목록 조회 성공",
			content = @Content(schema = @Schema(implementation = StoreOrderReservationListResponse.class))
	)
	@GetMapping("/store")
	public ResponseEntity<BaseResponse<StoreOrderReservationListResponse>> getStoreOrders(
			@Parameter(description = "스토어 ID", required = false,
					example = "00000000-0000-0000-0000-000000000001")
			@RequestParam(required = false,
					defaultValue = "00000000-0000-0000-0000-000000000001") UUID storeId,
			@Parameter(description = "상품 ID", required = false,
					example = "00000000-0000-0000-0000-000000000101")
			@RequestParam(required = false,
					defaultValue = "00000000-0000-0000-0000-000000000101") UUID productId,
			@Parameter(description = "주문 상태", required = false,
					schema = @Schema(implementation = OrderStatus.class))
			@RequestParam(required = false, defaultValue = "REQUESTED") OrderStatus status,
			@Parameter(description = "조회 시작 시각", required = false)
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@Parameter(description = "조회 종료 시각", required = false)
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@Parameter(description = "페이지 (기본 1)", required = false)
			@RequestParam(required = false, defaultValue = "1") Integer page,
			@Parameter(description = "사이즈 (기본 20)", required = false)
			@RequestParam(required = false, defaultValue = "20") Integer size) {

		// page/size를 limit/offset으로 변환
		Long offset = (long) (page - 1) * size;
		StoreOrderReservationListResponse response = orderQueryService.getStoreOrderReservations(
				storeId, productId, status.name(), from, to, size, offset
		);
		return ok(response);
	}

	@Operation(
			summary = "내 주문/예약 목록",
			description = "CUSTOMER가 본인 주문/예약 목록을 타임라인 형태로 조회합니다."
	)
	@ApiResponse(
			responseCode = "200",
			description = "내 주문 목록 조회 성공",
			content = @Content(schema = @Schema(implementation = MyOrderTimelineResponse.class))
	)
	@GetMapping("/me")
	public ResponseEntity<BaseResponse<MyOrderTimelineResponse>> getMyOrders(
			@Parameter(hidden = true)
			@RequestParam(required = false) Long customerId,
			@Parameter(description = "주문 타입 (ALL/RESERVATION/PURCHASE)", required = false)
			@RequestParam(required = false, defaultValue = "ALL") String orderType,
			@Parameter(description = "주문 상태", required = false,
					schema = @Schema(implementation = OrderStatus.class))
			@RequestParam(required = false) OrderStatus status,
			@Parameter(description = "조회 시작 시각", required = false)
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@Parameter(description = "조회 종료 시각", required = false)
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@Parameter(description = "페이지 (기본 1)", required = false)
			@RequestParam(required = false, defaultValue = "1") Integer page,
			@Parameter(description = "사이즈 (기본 20)", required = false)
			@RequestParam(required = false, defaultValue = "20") Integer size) {

		// page/size를 limit/offset으로 변환하고 파라미터명 맞춤
		Long offset = (long) (page - 1) * size;
		Long resolvedCustomerId = customerId != null ? customerId : 1001L;
		String normalizedOrderType = (orderType != null && "ALL".equalsIgnoreCase(orderType))
				? null
				: orderType;
		String statusStr = status == null ? null : status.name();
		MyOrderTimelineResponse response = orderQueryService.getMyOrderTimeline(
				resolvedCustomerId, normalizedOrderType, statusStr, from, to, size, offset
		);
		return ok(response);
	}
}
