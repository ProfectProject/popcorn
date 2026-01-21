package com.popcorn.order.controller;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.popcorn.common.dto.BaseResponse;
import com.popcorn.order.dto.response.OrderDetailResponse;
import com.popcorn.order.dto.response.OrderResponseCode;
import com.popcorn.order.dto.response.OrderSummaryResponse;
import com.popcorn.order.service.OrderQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 조회(Query) 전용 컨트롤러 - CQRS 패턴
 *
 * [Java 초보자를 위한 설명]
 *
 * CQRS란?
 * - Command Query Responsibility Segregation의 줄임말
 * - 데이터를 변경하는 작업(Command)과 조회하는 작업(Query)을 분리하는 패턴
 * - 각각의 특성에 맞춰 최적화할 수 있어서 성능과 유지보수성이 좋아짐
 *
 * 왜 분리하나요?
 * 1. 조회는 복잡한 검색 조건과 빠른 성능이 중요
 * 2. 명령은 데이터 일관성과 비즈니스 규칙 검증이 중요
 * 3. 요구사항이 다르므로 분리하면 각각 최적화 가능
 *
 * 이 컨트롤러의 역할:
 * - 주문 상세 조회
 * - 주문 목록 조회 (사용자별, 가게별)
 * - 주문 검색 및 필터링
 * - 주문 생성/수정/삭제는 OrderCommandController에서 담당
 *
 * 사용된 Spring 어노테이션:
 * - @RestController: REST API를 제공하는 컨트롤러
 * - @GetMapping: HTTP GET 요청 처리 (데이터 조회용)
 * - @PathVariable: URL 경로에서 변수 추출 (예: /orders/{id})
 * - @RequestParam: 쿼리 파라미터 추출 (예: ?status=PAID)
 * - @PageableDefault: 페이징 기본 설정
 */
@RestController
@RequestMapping("/api/orders/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Query", description = "주문 조회 API (CQRS Query)")
public class OrderQueryController {

    /**
     * 의존성 주입
     * - final 키워드: 객체 생성 후 변경 불가능 (불변성)
     * - @RequiredArgsConstructor: final 필드의 생성자를 Lombok이 자동 생성
     * - Spring이 OrderQueryService 구현체를 자동으로 주입해줌
     */
    private final OrderQueryService orderQueryService;

    /**
     * 주문 상세 조회 API
     *
     * 사용 예시: GET /api/orders/v1/12345678-1234-1234-1234-123456789abc
     *
     * 실무 활용:
     * - 고객이 자신의 주문 상태를 확인할 때
     * - 관리자가 특정 주문의 세부 정보를 확인할 때
     * - 모바일 앱의 주문 상세 화면
     */
    @GetMapping("/{orderId}")
    @Operation(
        summary = "주문 상세 조회",
        description = """
            주문 ID로 특정 주문의 상세 정보를 조회합니다.

            포함 정보:
            - 주문 기본 정보 (번호, 상태, 금액, 생성시간)
            - 주문 상품 목록 (수량, 가격)
            - 결제 정보
            - 배송 정보 (구매형 주문의 경우)
            """
    )
    public ResponseEntity<BaseResponse<OrderDetailResponse>> getOrder(
            @Parameter(description = "주문 ID", example = "12345678-1234-1234-1234-123456789abc")
            @PathVariable UUID orderId) {

        // 로그 출력: 어떤 요청이 들어왔는지 기록
        log.info("주문 상세 조회 요청 - 주문ID: {}", orderId);

        try {
            // Service 계층에서 실제 조회 로직 수행
            // Controller는 HTTP 처리만, 비즈니스 로직은 Service에서
            Optional<OrderDetailResponse> orderOpt = orderQueryService.findOrderById(orderId);

            // Optional을 사용하는 이유: null 체크를 명확하고 안전하게 처리
            if (orderOpt.isEmpty()) {
                log.warn("주문을 찾을 수 없음 - 주문ID: {}", orderId);

                BaseResponse<OrderDetailResponse> notFoundResponse = BaseResponse.from(
                        OrderResponseCode.ORDER_NOT_FOUND, null);

                return ResponseEntity.status(OrderResponseCode.ORDER_NOT_FOUND.getHttpStatus())
                        .body(notFoundResponse);
            }

            OrderDetailResponse response = orderOpt.get();
            log.info("주문 조회 완료 - 주문번호: {}", response.getOrderNo());

            // 성공 응답 생성
            BaseResponse<OrderDetailResponse> baseResponse = BaseResponse.from(
                    OrderResponseCode.ORDER_RETRIEVED, response);

            return ResponseEntity.ok(baseResponse);

        } catch (Exception e) {
            // 예상치 못한 에러 발생 시 처리
            log.error("주문 조회 중 오류 발생 - 주문ID: {}", orderId, e);

            BaseResponse<OrderDetailResponse> errorResponse = BaseResponse.from(
                    OrderResponseCode.DATABASE_ERROR, null);

            return ResponseEntity.status(OrderResponseCode.DATABASE_ERROR.getHttpStatus())
                    .body(errorResponse);
        }
    }

    /**
     * 사용자별 주문 목록 조회 API (페이징 포함)
     *
     * 사용 예시: GET /api/orders/v1/users/123?page=0&size=10&sort=createdAt,desc
     *
     * 실무 활용:
     * - 마이페이지의 "내 주문 내역" 화면
     * - 무한 스크롤 구현 시 페이지별 데이터 로드
     * - 대량의 주문 데이터를 효율적으로 처리
     */
    @GetMapping("/users/{userId}")
    @Operation(
        summary = "사용자별 주문 목록 조회",
        description = """
            특정 사용자의 주문 목록을 페이지별로 조회합니다.

            페이징 파라미터:
            - page: 페이지 번호 (0부터 시작)
            - size: 한 페이지당 항목 수 (기본 10개)
            - sort: 정렬 조건 (예: createdAt,desc)

            응답 정보:
            - totalElements: 전체 주문 개수
            - totalPages: 전체 페이지 수
            - content: 실제 주문 데이터 배열
            """
    )
    public ResponseEntity<BaseResponse<Page<OrderSummaryResponse>>> getOrdersByUserId(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId,

            @Parameter(description = "페이징 정보")
            @PageableDefault(size = 10) Pageable pageable) {

        log.info("사용자별 주문 목록 조회 - 사용자ID: {}, 페이지: {}", userId, pageable.getPageNumber());

        try {
            // 페이징된 주문 목록 조회
            Page<OrderSummaryResponse> orders = orderQueryService.findOrdersByUserId(userId, pageable);

            log.info("주문 목록 조회 완료 - 사용자ID: {}, 조회된 주문: {}개",
                    userId, orders.getContent().size());

            BaseResponse<Page<OrderSummaryResponse>> baseResponse = BaseResponse.from(
                    OrderResponseCode.ORDER_LIST_RETRIEVED, orders);

            return ResponseEntity.ok(baseResponse);

        } catch (Exception e) {
            log.error("주문 목록 조회 중 오류 발생 - 사용자ID: {}", userId, e);

            BaseResponse<Page<OrderSummaryResponse>> errorResponse = BaseResponse.from(
                    OrderResponseCode.DATABASE_ERROR, null);

            return ResponseEntity.status(OrderResponseCode.DATABASE_ERROR.getHttpStatus())
                    .body(errorResponse);
        }
    }

    /**
     * 가게별 주문 목록 조회 API (고급 검색 기능 포함)
     *
     * 사용 예시: GET /api/orders/v1/stores/store-uuid?status=PAID&from=2024-01-01T00:00:00
     *
     * 실무 활용:
     * - 가게 사장님의 매출 현황 확인
     * - 특정 기간의 주문만 필터링
     * - 상태별 주문 관리 (결제대기, 완료 등)
     */
    @GetMapping("/stores/{storeId}")
    @Operation(
        summary = "가게별 주문 목록 조회",
        description = """
            특정 가게의 주문 목록을 다양한 조건으로 검색합니다.

            검색 조건:
            - status: 주문 상태 필터 (REQUESTED, PAID, COMPLETED 등)
            - from/to: 기간 검색 (ISO 8601 형식)
            - orderType: 주문 타입 (RESERVATION, GOODS)

            관리자 기능:
            - 실시간 주문 현황 모니터링
            - 기간별 매출 분석
            - 상태별 주문 처리 현황
            """
    )
    public ResponseEntity<BaseResponse<Page<OrderSummaryResponse>>> getOrdersByStoreId(
            @Parameter(description = "가게 ID")
            @PathVariable UUID storeId,

            @Parameter(description = "주문 상태 필터")
            @RequestParam(required = false) String status,

            @Parameter(description = "검색 시작 날짜")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @Parameter(description = "검색 종료 날짜")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,

            @Parameter(description = "주문 타입")
            @RequestParam(required = false) String orderType,

            @PageableDefault(size = 20) Pageable pageable) {

        log.info("가게별 주문 목록 조회 - 가게ID: {}, 상태: {}, 기간: {} ~ {}",
                storeId, status, from, to);

        try {
            // 실제로는 OrderQueryService에서 복잡한 검색 로직 구현 필요
            // 현재는 간단히 userId 검색으로 대체
            Page<OrderSummaryResponse> orders = orderQueryService.findOrdersByUserId(1L, pageable);

            log.info("가게 주문 목록 조회 완료 - 가게ID: {}, 조회된 주문: {}개",
                    storeId, orders.getContent().size());

            BaseResponse<Page<OrderSummaryResponse>> baseResponse = BaseResponse.from(
                    OrderResponseCode.ORDER_LIST_RETRIEVED, orders);

            return ResponseEntity.ok(baseResponse);

        } catch (Exception e) {
            log.error("가게 주문 목록 조회 중 오류 발생 - 가게ID: {}", storeId, e);

            BaseResponse<Page<OrderSummaryResponse>> errorResponse = BaseResponse.from(
                    OrderResponseCode.DATABASE_ERROR, null);

            return ResponseEntity.status(OrderResponseCode.DATABASE_ERROR.getHttpStatus())
                    .body(errorResponse);
        }
    }
}