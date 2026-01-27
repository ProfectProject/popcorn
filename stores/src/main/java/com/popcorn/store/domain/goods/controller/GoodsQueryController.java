package com.popcorn.store.domain.goods.controller;

import com.popcorn.common.controller.BaseController;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.store.domain.goods.dto.GoodsListResponse;
import com.popcorn.store.domain.goods.dto.GoodsStockResponse;
import com.popcorn.store.domain.goods.exception.GoodsException;
import com.popcorn.store.domain.goods.service.GoodsInventoryApiService;
import com.popcorn.store.domain.goods.service.GoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Goods", description = "굿즈 관리 API")
@RequestMapping("/api/stores/v1/popups/{popupId}/goods")
public class GoodsQueryController extends BaseController {
    private final GoodsService goodsService;
    private final GoodsInventoryApiService inventoryApiService;

    @GetMapping
    @Operation(summary = "팝업 굿즈 목록 조회", description = "팝업에 등록된 활성 굿즈 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "굿즈 목록 조회 성공",
                    content = @Content(
                            schema = @Schema(implementation = GoodsListResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "요청이 성공했습니다.",
                                      "data": {
                                        "items": [
                                          {
                                            "goodsName": "팝콘 키링",
                                            "goodsPrice": 12000,
                                            "stock": 100
                                          }
                                        ]
                                      }
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<BaseResponse<GoodsListResponse>> list(
            @Parameter(description = "팝업 ID", required = true)
            @PathVariable UUID popupId
    ) {
        return ok(goodsService.listForUser(popupId));
    }

    @PostMapping("/{goodsId}/reservation")
    @Operation(summary = "굿즈 재고 예약", description = "굿즈 재고를 예약합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "예약 성공",
                    content = @Content(
                            schema = @Schema(implementation = GoodsStockResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "요청이 성공했습니다.",
                                      "data": {
                                        "goodsId": "00000000-0000-0000-0000-000000000401",
                                        "stock": 99,
                                        "reservationStock": 1
                                      }
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "409", description = "재고 부족")
    })
    public ResponseEntity<BaseResponse<GoodsStockResponse>> reserveGoods(
            @PathVariable UUID popupId,
            @PathVariable UUID goodsId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) UUID orderId
    ) {
        if (quantity == null || quantity <= 0) {
            throw GoodsException.invalidQuantity();
        }
        UUID resolvedOrderId = orderId == null ? UUID.randomUUID() : orderId;
        GoodsInventoryApiService.HoldResult result = inventoryApiService.reserve(popupId, goodsId, quantity, resolvedOrderId);
        GoodsStockResponse response = GoodsStockResponse.builder()
                .goodsId(goodsId)
                .orderId(result.getOrderId())
                .stock(result.getRemaining())
                .reservationStock(result.getQuantity())
                .build();
        return ok(response);
    }

    @PostMapping("/api/stores/v1/goods/{goodsId}/reservation")
    @Operation(summary = "굿즈 예약 (팝업 ID 없이)", description = "굿즈 ID로 팝업을 조회해 예약합니다.")
    public ResponseEntity<BaseResponse<GoodsStockResponse>> reserveGoodsWithoutPopup(
            @PathVariable UUID goodsId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) UUID orderId
    ) {
        if (quantity == null || quantity <= 0) {
            throw GoodsException.invalidQuantity();
        }
        UUID popupId = goodsService.resolvePopupId(goodsId);
        UUID resolvedOrderId = orderId == null ? UUID.randomUUID() : orderId;
        GoodsInventoryApiService.HoldResult result = inventoryApiService.reserve(popupId, goodsId, quantity, resolvedOrderId);
        GoodsStockResponse response = GoodsStockResponse.builder()
                .goodsId(goodsId)
                .orderId(result.getOrderId())
                .stock(result.getRemaining())
                .reservationStock(result.getQuantity())
                .build();
        return ok(response);
    }

    @PostMapping("/{goodsId}/reservation/cancel")
    @Operation(summary = "굿즈 예약 취소", description = "굿즈 예약을 취소합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "취소 성공",
                    content = @Content(schema = @Schema(implementation = GoodsStockResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "409", description = "예약 수량 부족")
    })
    public ResponseEntity<BaseResponse<GoodsStockResponse>> cancelGoodsReservation(
            @PathVariable UUID popupId,
            @PathVariable UUID goodsId,
            @RequestParam Integer quantity,
            @RequestParam UUID orderId
    ) {
        if (quantity == null || quantity <= 0) {
            throw GoodsException.invalidQuantity();
        }
        if (orderId == null) {
            throw GoodsException.missingOrderId();
        }
        inventoryApiService.release(orderId);
        GoodsStockResponse response = inventoryApiService.currentStock(popupId, goodsId, 0);
        response.setOrderId(orderId);
        return ok(response);
    }

    @PostMapping("/api/stores/v1/goods/{goodsId}/reservation/cancel")
    @Operation(summary = "굿즈 예약 취소 (팝업 ID 없이)", description = "굿즈 ID로 팝업을 조회해 예약을 취소합니다.")
    public ResponseEntity<BaseResponse<GoodsStockResponse>> cancelGoodsReservationWithoutPopup(
            @PathVariable UUID goodsId,
            @RequestParam Integer quantity,
            @RequestParam UUID orderId
    ) {
        if (quantity == null || quantity <= 0) {
            throw GoodsException.invalidQuantity();
        }
        if (orderId == null) {
            throw GoodsException.missingOrderId();
        }
        UUID popupId = goodsService.resolvePopupId(goodsId);
        inventoryApiService.release(orderId);
        GoodsStockResponse response = inventoryApiService.currentStock(popupId, goodsId, 0);
        response.setOrderId(orderId);
        return ok(response);
    }

    @PostMapping("/{goodsId}/reservation/fail")
    @Operation(summary = "굿즈 예약 실패 처리", description = "예약 실패 시 재고를 복구합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "실패 처리 성공",
                    content = @Content(schema = @Schema(implementation = GoodsStockResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "409", description = "예약 수량 부족")
    })
    public ResponseEntity<BaseResponse<GoodsStockResponse>> failGoodsReservation(
            @PathVariable UUID popupId,
            @PathVariable UUID goodsId,
            @RequestParam Integer quantity,
            @RequestParam UUID orderId
    ) {
        if (quantity == null || quantity <= 0) {
            throw GoodsException.invalidQuantity();
        }
        if (orderId == null) {
            throw GoodsException.missingOrderId();
        }
        inventoryApiService.release(orderId);
        GoodsStockResponse response = inventoryApiService.currentStock(popupId, goodsId, 0);
        response.setOrderId(orderId);
        return ok(response);
    }

    @PostMapping("/api/stores/v1/goods/{goodsId}/reservation/fail")
    @Operation(summary = "굿즈 예약 실패 처리 (팝업 ID 없이)", description = "굿즈 ID로 팝업을 조회해 예약 실패 처리를 합니다.")
    public ResponseEntity<BaseResponse<GoodsStockResponse>> failGoodsReservationWithoutPopup(
            @PathVariable UUID goodsId,
            @RequestParam Integer quantity,
            @RequestParam UUID orderId
    ) {
        if (quantity == null || quantity <= 0) {
            throw GoodsException.invalidQuantity();
        }
        if (orderId == null) {
            throw GoodsException.missingOrderId();
        }
        UUID popupId = goodsService.resolvePopupId(goodsId);
        inventoryApiService.release(orderId);
        GoodsStockResponse response = inventoryApiService.currentStock(popupId, goodsId, 0);
        response.setOrderId(orderId);
        return ok(response);
    }

    @PostMapping("/{goodsId}/reservation/complete")
    @Operation(summary = "굿즈 예약 완료", description = "예약 완료 후 재고를 확정합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "완료 성공",
                    content = @Content(schema = @Schema(implementation = GoodsStockResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "409", description = "예약 수량 부족")
    })
    public ResponseEntity<BaseResponse<GoodsStockResponse>> completeGoodsReservation(
            @PathVariable UUID popupId,
            @PathVariable UUID goodsId,
            @RequestParam Integer quantity,
            @RequestParam UUID orderId
    ) {
        if (quantity == null || quantity <= 0) {
            throw GoodsException.invalidQuantity();
        }
        if (orderId == null) {
            throw GoodsException.missingOrderId();
        }
        GoodsStockResponse response = goodsService.completeReservationGoods(popupId, goodsId, quantity);
        inventoryApiService.releaseQuietly(orderId);
        response.setOrderId(orderId);
        return ok(response);
    }

    @PostMapping("/api/stores/v1/goods/{goodsId}/reservation/complete")
    @Operation(summary = "굿즈 예약 완료 (팝업 ID 없이)", description = "굿즈 ID로 팝업을 조회해 예약을 확정합니다.")
    public ResponseEntity<BaseResponse<GoodsStockResponse>> completeGoodsReservationWithoutPopup(
            @PathVariable UUID goodsId,
            @RequestParam Integer quantity,
            @RequestParam UUID orderId
    ) {
        if (quantity == null || quantity <= 0) {
            throw GoodsException.invalidQuantity();
        }
        if (orderId == null) {
            throw GoodsException.missingOrderId();
        }
        UUID popupId = goodsService.resolvePopupId(goodsId);
        GoodsStockResponse response = goodsService.completeReservationGoods(popupId, goodsId, quantity);
        inventoryApiService.releaseQuietly(orderId);
        response.setOrderId(orderId);
        return ok(response);
    }

}
