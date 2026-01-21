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
import com.popcorn.order.dto.query.OrderListQuery;
import com.popcorn.order.dto.response.OrderDetailResponse;
import com.popcorn.order.dto.response.OrderListResponse;
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
     * 사용자별 주문 목록 조회 API (Store 서비스 방식 pagination)
     *
     * 사용 예시: GET /api/orders/v1/users/123?page=1&size=20&withTotal=true
     *
     * 실무 활용:
     * - 마이페이지의 "내 주문 내역" 화면
     * - 무한 스크롤 구현 시 페이지별 데이터 로드
     * - 대량의 주문 데이터를 효율적으로 처리
     */
    @GetMapping("/users/{userId}")
    @Operation(
        summary = "사용자별 주문 목록 조회 (Store 서비스 방식)",
        description = """
            특정 사용자의 주문 목록을 페이지별로 조회합니다.

            페이징 파라미터 (Store 서비스와 동일):
            - page: 페이지 번호 (1부터 시작)
            - size: 한 페이지당 항목 수 (기본 20개)
            - withTotal: 전체 개수 포함 여부 (기본 true)

            응답 정보:
            - items: 실제 주문 데이터 배열
            - page: 현재 페이지 번호
            - size: 페이지 크기
            - total: 전체 주문 개수
            """
    )
    public ResponseEntity<BaseResponse<OrderListResponse>> getOrdersByUserId(
            @Parameter(description = "사용자 ID", example = "1")
            @PathVariable Long userId,

            @Parameter(description = "주문 상태 필터", example = "PAID")
            @RequestParam(required = false) String status,

            @Parameter(description = "페이지(기본 1)", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer page,

            @Parameter(description = "사이즈(기본 20, 최대 100)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size,

            @Parameter(description = "전체 개수 포함 여부(기본 true)", example = "true")
            @RequestParam(required = false, defaultValue = "true") Boolean withTotal) {

        log.info("사용자별 주문 목록 조회 - 사용자ID: {}, 상태: {}, 페이지: {}, 사이즈: {}",
                userId, status, page, size);

        try {
            OrderListQuery query = OrderListQuery.builder()
                    .userId(userId)
                    .status(status)
                    .page(page)
                    .size(size)
                    .withTotal(withTotal)
                    .build();

            OrderListResponse response = orderQueryService.findOrdersWithQuery(query);

            log.info("사용자 주문 목록 조회 완료 - 사용자ID: {}, 조회된 주문: {}개",
                    userId, response.getItems().size());

            BaseResponse<OrderListResponse> baseResponse = BaseResponse.from(
                    OrderResponseCode.ORDER_LIST_RETRIEVED, response);

            return ResponseEntity.ok(baseResponse);

        } catch (Exception e) {
            log.error("사용자 주문 목록 조회 중 오류 발생 - 사용자ID: {}", userId, e);

            BaseResponse<OrderListResponse> errorResponse = BaseResponse.from(
                    OrderResponseCode.DATABASE_ERROR, null);

            return ResponseEntity.status(OrderResponseCode.DATABASE_ERROR.getHttpStatus())
                    .body(errorResponse);
        }
    }

    /**
     * 가게별 주문 목록 조회 API (Store 서비스 방식)
     *
     * 사용 예시: GET /api/orders/v1/stores/store-uuid?status=PAID&from=2024-01-01T00:00:00&page=1&size=20
     *
     * 실무 활용:
     * - 가게 사장님의 매출 현황 확인
     * - 특정 기간의 주문만 필터링
     * - 상태별 주문 관리 (결제대기, 완료 등)
     */
    @GetMapping("/stores/{storeId}")
    @Operation(
        summary = "가게별 주문 목록 조회 (Store 서비스 방식)",
        description = """
            특정 가게의 주문 목록을 다양한 조건으로 검색합니다.

            검색 조건:
            - status: 주문 상태 필터 (REQUESTED, PAID, COMPLETED 등)
            - orderType: 주문 타입 (RESERVATION, GOODS, MIXED)
            - from/to: 기간 검색 (ISO 8601 형식)
            - page: 페이지 번호 (1부터 시작)
            - size: 페이지 크기 (기본 20개)
            - withTotal: 전체 개수 포함 여부

            관리자 기능:
            - 실시간 주문 현황 모니터링
            - 기간별 매출 분석
            - 상태별 주문 처리 현황
            """
    )
    public ResponseEntity<BaseResponse<OrderListResponse>> getOrdersByStoreId(
            @Parameter(description = "가게 ID")
            @PathVariable UUID storeId,

            @Parameter(description = "주문 상태 필터", example = "PAID")
            @RequestParam(required = false) String status,

            @Parameter(description = "주문 타입 필터", example = "RESERVATION")
            @RequestParam(required = false) String orderType,

            @Parameter(description = "검색 시작 날짜")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @Parameter(description = "검색 종료 날짜")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,

            @Parameter(description = "페이지(기본 1)", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer page,

            @Parameter(description = "사이즈(기본 20, 최대 100)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size,

            @Parameter(description = "전체 개수 포함 여부(기본 true)", example = "true")
            @RequestParam(required = false, defaultValue = "true") Boolean withTotal) {

        log.info("가게별 주문 목록 조회 - 가게ID: {}, 상태: {}, 타입: {}, 기간: {} ~ {}, 페이지: {}",
                storeId, status, orderType, from, to, page);

        try {
            OrderListQuery query = OrderListQuery.builder()
                    .storeId(storeId)
                    .status(status)
                    .orderType(orderType)
                    .from(from)
                    .to(to)
                    .page(page)
                    .size(size)
                    .withTotal(withTotal)
                    .build();

            OrderListResponse response = orderQueryService.findOrdersWithQuery(query);

            log.info("가게 주문 목록 조회 완료 - 가게ID: {}, 조회된 주문: {}개",
                    storeId, response.getItems().size());

            BaseResponse<OrderListResponse> baseResponse = BaseResponse.from(
                    OrderResponseCode.ORDER_LIST_RETRIEVED, response);

            return ResponseEntity.ok(baseResponse);

        } catch (Exception e) {
            log.error("가게 주문 목록 조회 중 오류 발생 - 가게ID: {}", storeId, e);

            BaseResponse<OrderListResponse> errorResponse = BaseResponse.from(
                    OrderResponseCode.DATABASE_ERROR, null);

            return ResponseEntity.status(OrderResponseCode.DATABASE_ERROR.getHttpStatus())
                    .body(errorResponse);
        }
    }

    /**
     * 팝업별 주문 목록 조회 API (Store 서비스 pagination 방식)
     *
     * 사용 예시: GET /api/orders/v1/popups/popup-uuid?page=1&size=20&status=PAID
     *
     * 실무 활용:
     * - 특정 팝업의 주문 현황 확인
     * - 팝업별 매출 분석
     * - 예약 현황 모니터링
     */
    @GetMapping("/popups/{popupId}")
    @Operation(
        summary = "팝업별 주문 목록 조회",
        description = """
            특정 팝업의 주문 목록을 조회합니다.

            주요 기능:
            - 팝업별 주문 필터링
            - 상태별 필터링 (REQUESTED, PAID, COMPLETED 등)
            - 주문 타입별 필터링 (RESERVATION, GOODS, MIXED)
            - 페이징 처리 (기본 20개, 최대 100개)

            사용 예시:
            - 전체 조회: /api/orders/v1/popups/{popupId}
            - 상태 필터: /api/orders/v1/popups/{popupId}?status=PAID
            - 페이징: /api/orders/v1/popups/{popupId}?page=1&size=10
            """
    )
    public ResponseEntity<BaseResponse<OrderListResponse>> getOrdersByPopupId(
            @Parameter(description = "팝업 ID", example = "00000000-0000-0000-0000-000000000101")
            @PathVariable UUID popupId,

            @Parameter(description = "주문 상태 필터", example = "PAID")
            @RequestParam(required = false) String status,

            @Parameter(description = "주문 타입 필터", example = "RESERVATION")
            @RequestParam(required = false) String orderType,

            @Parameter(description = "페이지(기본 1)", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer page,

            @Parameter(description = "사이즈(기본 20, 최대 100)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size,

            @Parameter(description = "전체 개수 포함 여부(기본 true)", example = "true")
            @RequestParam(required = false, defaultValue = "true") Boolean withTotal) {

        log.info("팝업별 주문 목록 조회 - 팝업ID: {}, 상태: {}, 타입: {}, 페이지: {}, 사이즈: {}",
                popupId, status, orderType, page, size);

        try {
            OrderListQuery query = OrderListQuery.builder()
                    .popupId(popupId)
                    .status(status)
                    .orderType(orderType)
                    .page(page)
                    .size(size)
                    .withTotal(withTotal)
                    .build();

            OrderListResponse response = orderQueryService.findOrdersByPopupId(query);

            log.info("팝업 주문 목록 조회 완료 - 팝업ID: {}, 조회된 주문: {}개",
                    popupId, response.getItems().size());

            BaseResponse<OrderListResponse> baseResponse = BaseResponse.from(
                    OrderResponseCode.ORDER_LIST_RETRIEVED, response);

            return ResponseEntity.ok(baseResponse);

        } catch (Exception e) {
            log.error("팝업 주문 목록 조회 중 오류 발생 - 팝업ID: {}", popupId, e);

            BaseResponse<OrderListResponse> errorResponse = BaseResponse.from(
                    OrderResponseCode.DATABASE_ERROR, null);

            return ResponseEntity.status(OrderResponseCode.DATABASE_ERROR.getHttpStatus())
                    .body(errorResponse);
        }
    }

    /**
     * 카테고리별 주문 목록 조회 API
     *
     * 사용 예시: GET /api/orders/v1/categories/RESERVATION?page=1&size=20
     *
     * 실무 활용:
     * - 예약형 vs 구매형 주문 분석
     * - 카테고리별 매출 현황
     * - 비즈니스 성과 측정
     */
    @GetMapping("/categories/{category}")
    @Operation(
        summary = "카테고리별 주문 목록 조회",
        description = """
            주문 타입별로 주문 목록을 조회합니다.

            주문 카테고리:
            - RESERVATION: 예약형 주문 (팝업 체험)
            - GOODS: 구매형 주문 (굿즈 구매)
            - MIXED: 혼합형 주문 (예약 + 굿즈)

            검색 조건:
            - category: 주문 타입 (필수)
            - status: 주문 상태 필터
            - userId: 특정 사용자 필터
            - 페이징 처리
            """
    )
    public ResponseEntity<BaseResponse<OrderListResponse>> getOrdersByCategory(
            @Parameter(description = "주문 카테고리(RESERVATION/GOODS/MIXED)", example = "RESERVATION")
            @PathVariable String category,

            @Parameter(description = "주문 상태 필터", example = "PAID")
            @RequestParam(required = false) String status,

            @Parameter(description = "사용자 ID 필터", example = "123")
            @RequestParam(required = false) Long userId,

            @Parameter(description = "검색 시작 날짜")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @Parameter(description = "검색 종료 날짜")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,

            @Parameter(description = "페이지(기본 1)", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer page,

            @Parameter(description = "사이즈(기본 20, 최대 100)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size,

            @Parameter(description = "전체 개수 포함 여부(기본 true)", example = "true")
            @RequestParam(required = false, defaultValue = "true") Boolean withTotal) {

        log.info("카테고리별 주문 목록 조회 - 카테고리: {}, 상태: {}, 사용자ID: {}, 페이지: {}, 사이즈: {}",
                category, status, userId, page, size);

        try {
            OrderListQuery query = OrderListQuery.builder()
                    .orderType(category)
                    .status(status)
                    .userId(userId)
                    .from(from)
                    .to(to)
                    .page(page)
                    .size(size)
                    .withTotal(withTotal)
                    .build();

            OrderListResponse response = orderQueryService.findOrdersByCategory(query);

            log.info("카테고리 주문 목록 조회 완료 - 카테고리: {}, 조회된 주문: {}개",
                    category, response.getItems().size());

            BaseResponse<OrderListResponse> baseResponse = BaseResponse.from(
                    OrderResponseCode.ORDER_LIST_RETRIEVED, response);

            return ResponseEntity.ok(baseResponse);

        } catch (Exception e) {
            log.error("카테고리 주문 목록 조회 중 오류 발생 - 카테고리: {}", category, e);

            BaseResponse<OrderListResponse> errorResponse = BaseResponse.from(
                    OrderResponseCode.DATABASE_ERROR, null);

            return ResponseEntity.status(OrderResponseCode.DATABASE_ERROR.getHttpStatus())
                    .body(errorResponse);
        }
    }
}