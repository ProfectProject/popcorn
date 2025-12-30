package com.popcorn.demo.domain.order.controller;

import com.popcorn.demo.domain.order.dto.CreateOrderRequest;
import com.popcorn.demo.domain.order.dto.OrderCreatedDto;
import com.popcorn.demo.domain.order.service.OrderService;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 주문 관련 REST API를 처리하는 컨트롤러
 * BaseController를 상속받아 표준화된 응답 처리를 제공합니다.
 *
 * API Endpoints:
 * - POST /orders/{userId}: 새로운 주문 생성
 *
 * 공통 기능:
 * - 요청 검증 (Validation)
 * - 예외 처리 (Exception Handling)
 * - 표준 응답 형식 (Standard Response Format)
 * - 멱등성 지원 (Idempotency)
 * - Swagger API 문서화
 */
@Tag(name = "Orders", description = "주문 관리 API")
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController extends BaseController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
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
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "주문 생성 성공",
            content = @Content(schema = @Schema(implementation = OrderCreatedDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (필수값 누락, 형식 오류, 비즈니스 검증 실패)"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "리소스 없음 (스토어, 상품, 세션, 옵션, 굿즈변형)"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "비즈니스 규칙 위반 (재고부족, 정원초과, 상태오류 등)"
        )
    })
    @PostMapping("/{userId}")
    public ResponseEntity<BaseResponse<OrderCreatedDto>> createOrder(
            @Parameter(description = "주문 생성 사용자 ID", required = true)
            @PathVariable("userId") Long userId,

            @Parameter(description = "주문 생성 요청 데이터", required = true)
            @Valid @RequestBody CreateOrderRequest request,

            @Parameter(description = "멱등성 키 (중복 요청 방지)", required = false)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // 서비스 호출하여 주문 생성
        OrderCreatedDto orderCreatedDto = orderService.createOrder(userId, request, idempotencyKey);

        // BaseResponse 사용하여 성공 응답 생성
        BaseResponse<OrderCreatedDto> response = BaseResponse.success(orderCreatedDto);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /*
     * ==================== API 테스트용 Request Body 예시 ====================
     *
     * 1. 예약형 주문 생성 예시:
     * POST /api/v1/orders/123
     * Content-Type: application/json
     * Idempotency-Key: unique-key-12345
     *
     * {
     *   "orderType": "RESERVATION",
     *   "storeId": 10,
     *   "productId": 55,
     *   "items": [
     *     {
     *       "orderItemType": "RESERVATION",
     *       "sessionId": 777,
     *       "optionId": 88,
     *       "qty": 2
     *     }
     *   ]
     * }
     *
     * 2. 구매형 주문 생성 예시:
     * POST /api/v1/orders/123
     * Content-Type: application/json
     *
     * {
     *   "orderType": "PURCHASE",
     *   "storeId": 10,
     *   "productId": 55,
     *   "reservationId": 999,
     *   "items": [
     *     {
     *       "orderItemType": "MERCH",
     *       "merchVariantId": 456,
     *       "qty": 2
     *     }
     *   ],
     *   "address": {
     *     "address1": "서울특별시 강남구 테헤란로 123",
     *     "address2": "ABC빌딩 12층 1201호",
     *     "receiverName": "홍길동",
     *     "phone": "010-1234-5678"
     *   }
     * }
     *
     * ==================== 응답 예시 ====================
     *
     * 성공 응답 (201 Created):
     * {
     *   "code": "SUCCESS",
     *   "message": "요청이 성공했습니다.",
     *   "data": {
     *     "id": 501,
     *     "orderNo": "O20251230-000501",
     *     "orderType": "PURCHASE",
     *     "status": "REQUESTED",
     *     "storeId": 10,
     *     "productId": 55,
     *     "totalAmount": 29000,
     *     "cancelableUntil": "2025-12-30T14:15:00",
     *     "createdAt": "2025-12-30T13:10:00",
     *     "items": [
     *       {
     *         "id": 90001,
     *         "orderItemType": "MERCH",
     *         "qty": 2,
     *         "unitPrice": 14500,
     *         "lineAmount": 29000
     *       }
     *     ]
     *   }
     * }
     *
     * 오류 응답 (400 Bad Request):
     * {
     *   "code": "INVALID_QTY",
     *   "message": "수량은 1 이상이어야 합니다.",
     *   "data": null
     * }
     */
}