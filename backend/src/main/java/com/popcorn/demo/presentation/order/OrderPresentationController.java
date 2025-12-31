package com.popcorn.demo.presentation.order;

import com.popcorn.demo.application.order.port.in.CreateOrderCommand;
import com.popcorn.demo.application.order.port.in.CreateOrderResponse;
import com.popcorn.demo.application.order.usecase.CreateOrderUseCase;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.dto.ResponseCode;
import com.popcorn.demo.domain.order.dto.CreateOrderRequest;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 주문 Presentation 컨트롤러 (Clean Architecture 버전)
 *
 * Clean Architecture의 Presentation Layer에 해당합니다.
 * - HTTP 요청/응답 처리
 * - DTO 변환
 * - Use Case 호출
 * - 에러 핸들링
 */
@RestController
@RequestMapping("/api/v2/orders") // v2로 새 버전 제공
@Tag(name = "Order V2", description = "주문 관리 API (Clean Architecture)")
@RequiredArgsConstructor
@Slf4j
public class OrderPresentationController {

    private final CreateOrderUseCase createOrderUseCase;

    /**
     * 주문 생성 API (Clean Architecture 버전)
     *
     * @param userId 사용자 ID
     * @param request 주문 생성 요청
     * @param idempotencyKey 멱등성 키 (헤더)
     * @return 주문 생성 응답
     */
    @PostMapping("/{userId}")
    @Operation(
            summary = "주문 생성 (Clean Architecture)",
            description = "새로운 주문을 생성합니다. 멱등성을 지원하여 동일한 Idempotency-Key로는 중복 주문이 생성되지 않습니다."
    )
    public ResponseEntity<BaseResponse<CreateOrderResponse>> createOrder(
            @Parameter(description = "사용자 ID", example = "1001")
            @PathVariable Long userId,

            @RequestBody CreateOrderRequest request,

            @Parameter(description = "멱등성 키 (중복 주문 방지)", example = "order-20231230-001")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        log.info("🎯 주문 생성 요청 - 사용자: {}, 멱등성키: {}", userId, idempotencyKey);

        try {
            // 1. Request → Command 변환 (Presentation Layer 책임)
            CreateOrderCommand command = CreateOrderCommand.builder()
                    .userId(userId)
                    .storeId(request.getStoreId())
                    .productId(request.getProductId())
                    .orderType(request.getOrderType())
                    .idempotencyKey(idempotencyKey)
                    .items(convertToItemCommands(request.getItems()))
                    .build();

            // 2. Use Case 실행
            CreateOrderResponse response = createOrderUseCase.createOrder(command);

            // 3. 성공 응답
            return ResponseEntity.ok(
                    BaseResponse.success(response)
            );

        } catch (Exception e) {
            log.error("❌ 주문 생성 실패 - 사용자: {}, 에러: {}", userId, e.getMessage(), e);

            return ResponseEntity.badRequest().body(
                    BaseResponse.error(ResponseCode.INTERNAL_ERROR)
            );
        }
    }

    /**
     * Request DTO의 주문 항목을 Command의 주문 항목으로 변환
     */
    private List<CreateOrderCommand.OrderItemCommand> convertToItemCommands(
            List<com.popcorn.demo.domain.order.dto.OrderItemRequest> items) {

        return items.stream()
                .map(item -> CreateOrderCommand.OrderItemCommand.builder()
                        .orderItemType(OrderItemType.valueOf(item.getOrderItemType()))
                        .sessionId(item.getSessionId())
                        .merchVariantId(item.getMerchVariantId())
                        .qty(item.getQty())
                        .unitPrice(14500) // 임시 고정값 - 실제로는 가격 조회 서비스에서 계산
                        .build())
                .toList();
    }
}