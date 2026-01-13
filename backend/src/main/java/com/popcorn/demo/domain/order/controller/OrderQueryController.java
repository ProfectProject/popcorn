package com.popcorn.demo.domain.order.controller;

import java.util.UUID;
import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.domain.auth.dto.CustomUserDetails;
import com.popcorn.demo.domain.order.dto.response.MyOrderTimelineResponse;
import com.popcorn.demo.domain.order.dto.response.OrderDetailDto;
import com.popcorn.demo.domain.order.dto.response.OrderStatusDto;
import com.popcorn.demo.domain.order.dto.response.StoreOrderReservationListResponse;
import com.popcorn.demo.domain.order.service.OrderQueryService;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.versioning.ApiVersion;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderValidationException;

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
@Tag(name = "Order", description = "주문 관리 API")
@RestController
@ApiVersion("v1")
@RequestMapping("/api/v1/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderQueryController extends BaseController {

	private final OrderQueryService orderQueryService;

	@Operation(
			summary = "주문 상세 조회",
			description = """
				주문의 상세 정보를 조회합니다.

				**권한별 접근:**
				- CUSTOMER: 본인 주문만 조회 가능
				- OWNER/MANAGER: 본인 스토어 주문 조회 가능

				**포함 정보:**
				- 기본 주문 정보 (주문번호, 상태, 총액)
				- 주문 아이템 목록 (세션/굿즈 상세)
				- 결제 정보 및 상태
				- 주문 상태 변경 이력
				- 취소 가능 여부 및 시간
				- 배송지 정보 (구매형 주문)

				**사용 케이스:**
				- 고객 주문 내역 확인
				- 운영자 주문 관리
				- 고객 서비스 지원
				"""
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
			@PathVariable UUID orderId,
			Authentication authentication) {

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		Long userId = userDetails.getUserId();
		String role = userDetails.getRole();

		OrderDetailDto detail = orderQueryService.getOrderDetail(orderId, userId, role);
		return ok(detail);
	}

	@Operation(
			summary = "주문 상태 조회 (CUSTOMER)",
			description = """
				고객이 본인 주문의 현재 상태를 조회합니다.

				**주요 용도:**
				- 결제 직후 상태 갱신 확인
				- 실시간 주문 진행 상황 추적
				- 모바일 앱 상태 동기화

				**응답 정보:**
				- 현재 주문 상태 (REQUESTED, PAID, COMPLETED 등)
				- 결제 상태 (READY, PAID, FAILED 등)
				- 취소 가능 시간
				- 마지막 업데이트 시간

				**사용 예시:**
				- 결제 완료 후 폴링으로 상태 확인
				- 푸시 알림 클릭 시 최신 상태 조회
				- 주문 목록에서 개별 상태 업데이트
				"""
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
			Authentication authentication) {

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		Long customerId = userDetails.getUserId();

		OrderStatusDto response = orderQueryService.getOrderStatusForCustomer(orderId, customerId);
		return ok(response);
	}

	@Operation(
			summary = "주문/예약 상태 단건 조회 (OWNER/MANAGER)",
			description = "OWNER/MANAGER가 주문 상태를 조회합니다."
	)
	@ApiResponse(
			responseCode = "200",
			description = "주문 상태 조회 성공",
			content = @Content(schema = @Schema(implementation = OrderStatusDto.class))
	)
	@ApiResponse(responseCode = "403", description = "권한 없음")
	@ApiResponse(responseCode = "404", description = "주문 없음")
	@GetMapping("/{orderId}/status/ops")
	public ResponseEntity<BaseResponse<OrderStatusDto>> getOrderStatusForStaff(
			@Parameter(description = "주문 ID", required = true,
					example = "00000000-0000-0000-0000-000000001001")
			@PathVariable UUID orderId,
			Authentication authentication) {

		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		Long userId = userDetails.getUserId();
		String role = userDetails.getRole();

		OrderStatusDto response = orderQueryService.getOrderStatusForStaff(orderId, userId, role);
		return ok(response);
	}

	@Operation(
			summary = "가게 주문/예약 상태 목록 조회 (OWNER/MANAGER)",
			description = "OWNER/MANAGER가 가게/상품 기준으로 주문 상태 목록을 조회합니다. storeId 또는 popupId 중 하나는 필수입니다."
	)
	@ApiResponse(
			responseCode = "200",
			description = "주문 상태 목록 조회 성공",
			content = @Content(schema = @Schema(implementation = StoreOrderReservationListResponse.class))
	)
	@ApiResponse(responseCode = "403", description = "권한 없음")
	@GetMapping("/status/ops")
	public ResponseEntity<BaseResponse<StoreOrderReservationListResponse>> getStoreOrderStatusesForStaff(
			@Parameter(description = "스토어 ID",
					example = "00000000-0000-0000-0000-000000000001")
			@RequestParam(required = false) UUID storeId,
			@Parameter(description = "상품 ID",
					example = "00000000-0000-0000-0000-000000000101")
			@RequestParam(required = false) UUID popupId,
			@Parameter(description = "스케줄 ID",
					example = "00000000-0000-0000-0000-000000000201")
			@RequestParam(required = false) UUID scheduleId,
			@Parameter(description = "주문 타입 (RESERVATION/PURCHASE)")
			@RequestParam(required = false) String orderType,
			@Parameter(description = "주문 상태",
					schema = @Schema(implementation = OrderStatus.class))
			@RequestParam(required = false) OrderStatus status,
			@Parameter(description = "페이지 (기본 1)")
			@RequestParam(required = false, defaultValue = "1") Integer page,
			@Parameter(description = "사이즈 (기본 20)")
			@RequestParam(required = false, defaultValue = "20") Integer size) {

		if (storeId == null && popupId == null && scheduleId == null) {
			throw OrderValidationException.invalidRequest();
		}

		Long offset = (long) (page - 1) * size;
		String statusStr = status == null ? null : status.name();
		StoreOrderReservationListResponse response = orderQueryService.getStoreOrderReservations(
				storeId, popupId, scheduleId, orderType, statusStr, null, null, size, offset
		);
		return ok(response);
	}

	@Operation(
			summary = "가게 주문 목록 조회",
			description = """
				운영자(OWNER/MANAGER)가 가게 기준으로 주문 목록을 조회합니다.

				**필터링 옵션:**
				- 스토어 ID: 특정 스토어 주문만
				- 팝업 ID: 특정 팝업 주문만
				- 주문 상태: 특정 상태 주문만
				- 기간 필터: from/to 날짜 범위
				- 페이징: page/size 파라미터

				**주요 기능:**
				- 실시간 주문 현황 모니터링
				- 상태별 주문 관리
				- 매출 분석 기초 데이터
				- 고객 응대 지원

				**응답 정보:**
				- 주문 기본 정보
				- 고객 정보 (이름, 연락처)
				- 주문 항목 상세
				- 결제 정보

				**권한:**
				- OWNER: 본인 스토어만
				- MANAGER: 관리 권한 범위 내
				"""
	)
	@ApiResponse(
			responseCode = "200",
			description = "가게 주문 목록 조회 성공",
			content = @Content(schema = @Schema(implementation = StoreOrderReservationListResponse.class))
	)
	@GetMapping("/store")
	public ResponseEntity<BaseResponse<StoreOrderReservationListResponse>> getStoreOrders(
			@Parameter(description = "스토어 ID",
					example = "00000000-0000-0000-0000-000000000001")
			@RequestParam(required = false,
					defaultValue = "00000000-0000-0000-0000-000000000001") UUID storeId,
			@Parameter(description = "상품 ID",
					example = "00000000-0000-0000-0000-000000000101")
			@RequestParam(required = false,
					defaultValue = "00000000-0000-0000-0000-000000000101") UUID popupId,
			@Parameter(description = "스케줄 ID",
					example = "00000000-0000-0000-0000-000000000201")
			@RequestParam(required = false) UUID scheduleId,
			@Parameter(description = "주문 타입 (RESERVATION/PURCHASE)")
			@RequestParam(required = false) String orderType,
			@Parameter(description = "주문 상태",
					schema = @Schema(implementation = OrderStatus.class))
			@RequestParam(required = false, defaultValue = "REQUESTED") OrderStatus status,
			@Parameter(description = "조회 시작 시각")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@Parameter(description = "조회 종료 시각")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@Parameter(description = "페이지 (기본 1)")
			@RequestParam(required = false, defaultValue = "1") Integer page,
			@Parameter(description = "사이즈 (기본 20)")
			@RequestParam(required = false, defaultValue = "20") Integer size) {

		// page/size를 limit/offset으로 변환
		Long offset = (long) (page - 1) * size;
		StoreOrderReservationListResponse response = orderQueryService.getStoreOrderReservations(
				storeId, popupId, scheduleId, orderType, status.name(), from, to, size, offset
		);
		return ok(response);
	}

	@Operation(
			summary = "팝업 예약형 주문 목록 조회 (OWNER/MANAGER)",
			description = "OWNER/MANAGER가 팝업별 예약형 주문 목록을 조회합니다."
	)
	@GetMapping("/popup/{popupId}/reservations")
	public ResponseEntity<BaseResponse<StoreOrderReservationListResponse>> getPopupReservationOrders(
			@Parameter(description = "상품 ID", example = "00000000-0000-0000-0000-000000000101")
			@PathVariable UUID popupId,
			@Parameter(description = "스케줄 ID", example = "00000000-0000-0000-0000-000000000201")
			@RequestParam(required = false) UUID scheduleId,
			@Parameter(description = "주문 상태",
					schema = @Schema(implementation = OrderStatus.class))
			@RequestParam(required = false) OrderStatus status,
			@Parameter(description = "조회 시작 시각")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@Parameter(description = "조회 종료 시각")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@Parameter(description = "페이지 (기본 1)")
			@RequestParam(required = false, defaultValue = "1") Integer page,
			@Parameter(description = "사이즈 (기본 20)")
			@RequestParam(required = false, defaultValue = "20") Integer size) {

		Long offset = (long) (page - 1) * size;
		String statusStr = status == null ? null : status.name();
		StoreOrderReservationListResponse response = orderQueryService.getStoreOrderReservations(
				null, popupId, scheduleId, "RESERVATION", statusStr, from, to, size, offset
		);
		return ok(response);
	}

	@Operation(
			summary = "팝업 구매형 주문 목록 조회 (OWNER/MANAGER)",
			description = "OWNER/MANAGER가 팝업별 구매형 주문 목록을 조회합니다."
	)
	@GetMapping("/popup/{popupId}/purchases")
	public ResponseEntity<BaseResponse<StoreOrderReservationListResponse>> getPopupPurchaseOrders(
			@Parameter(description = "상품 ID", example = "00000000-0000-0000-0000-000000000101")
			@PathVariable UUID popupId,
			@Parameter(description = "주문 상태",
					schema = @Schema(implementation = OrderStatus.class))
			@RequestParam(required = false) OrderStatus status,
			@Parameter(description = "조회 시작 시각")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@Parameter(description = "조회 종료 시각")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@Parameter(description = "페이지 (기본 1)")
			@RequestParam(required = false, defaultValue = "1") Integer page,
			@Parameter(description = "사이즈 (기본 20)")
			@RequestParam(required = false, defaultValue = "20") Integer size) {

		Long offset = (long) (page - 1) * size;
		String statusStr = status == null ? null : status.name();
		StoreOrderReservationListResponse response = orderQueryService.getStoreOrderReservations(
				null, popupId, null, "PURCHASE", statusStr, from, to, size, offset
		);
		return ok(response);
	}

	@Operation(
			summary = "내 주문 목록 조회",
			description = """
				고객(CUSTOMER)이 본인의 주문 목록을 타임라인 형태로 조회합니다.

				**필터링 옵션:**
				- 주문 타입: ALL/RESERVATION/PURCHASE
				- 주문 상태: ALL 또는 특정 상태
				- 기간 필터: from/to 날짜 범위
				- 페이징: page/size 파라미터

				**정렬 기준:**
				- 최신 주문 우선 (created_at DESC)
				- 동일 날짜 내에서는 주문번호 기준

				**응답 정보:**
				- 주문 기본 정보 (번호, 상태, 금액)
				- 팝업 정보 (제목, 카테고리, 스토어명)
				- 주문 항목 요약
				- 취소 가능 여부

				**사용 케이스:**
				- 모바일 앱 마이페이지
				- 주문 내역 관리
				- 리뷰 작성 대상 조회
				"""
	)
	@ApiResponse(
			responseCode = "200",
			description = "내 주문 목록 조회 성공",
			content = @Content(schema = @Schema(implementation = MyOrderTimelineResponse.class))
	)
	@GetMapping("/me")
	public ResponseEntity<BaseResponse<MyOrderTimelineResponse>> getMyOrders(
			@Parameter(description = "주문 타입 (ALL/RESERVATION/PURCHASE)")
			@RequestParam(required = false, defaultValue = "ALL") String orderType,
			@Parameter(description = "주문 상태 (ALL 또는 OrderStatus 값)")
			@RequestParam(required = false) String status,
			@Parameter(description = "조회 시작 시각")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@Parameter(description = "조회 종료 시각")
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@Parameter(description = "페이지 (기본 1)")
			@RequestParam(required = false, defaultValue = "1") Integer page,
			@Parameter(description = "사이즈 (기본 20)")
			@RequestParam(required = false, defaultValue = "20") Integer size,
			Authentication authentication) {

		// JWT에서 현재 인증된 사용자 정보 추출
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		Long customerId = userDetails.getUserId();

		// page/size를 limit/offset으로 변환하고 파라미터명 맞춤
		Long offset = (long) (page - 1) * size;
		String normalizedOrderType = "ALL".equalsIgnoreCase(orderType)
				? null
				: orderType;
		String normalizedStatus = (status == null || "ALL".equalsIgnoreCase(status))
				? null
				: status;
		MyOrderTimelineResponse response = orderQueryService.getMyOrderTimeline(
				customerId, normalizedOrderType, normalizedStatus, from, to, size, offset
		);
		return ok(response);
	}
}
