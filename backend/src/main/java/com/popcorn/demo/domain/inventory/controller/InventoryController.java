package com.popcorn.demo.domain.inventory.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.common.versioning.ApiVersion;
import com.popcorn.demo.domain.inventory.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Inventory", description = "재고 관리 API")
@ApiVersion("v1")
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController extends BaseController {

	private final InventoryService inventoryService;

	@Operation(
			summary = "상품 재고 조회",
			description = "특정 상품의 현재 재고 수량을 조회합니다."
	)
	@ApiResponse(
			responseCode = "200",
			description = "재고 조회 성공"
	)
	@GetMapping("/goods/{goodsVariantId}")
	public ResponseEntity<BaseResponse<GoodsStockResponse>> getGoodsStock(
			@Parameter(description = "상품 변형 ID", required = true)
			@PathVariable UUID goodsVariantId) {

		Integer stock = inventoryService.getCurrentGoodsStock(goodsVariantId);

		if (stock == null) {
			throw new IllegalArgumentException("상품을 찾을 수 없습니다: " + goodsVariantId);
		}

		GoodsStockResponse response = GoodsStockResponse.builder()
				.goodsVariantId(goodsVariantId)
				.currentStock(stock)
				.status(getStockStatus(stock))
				.build();

		return ok(response);
	}

	@Operation(
			summary = "예약 좌석 조회",
			description = "특정 스케줄의 현재 잔여 좌석 수를 조회합니다."
	)
	@ApiResponse(
			responseCode = "200",
			description = "좌석 조회 성공"
	)
	@GetMapping("/schedules/{scheduleId}")
	public ResponseEntity<BaseResponse<ScheduleCapacityResponse>> getScheduleCapacity(
			@Parameter(description = "스케줄 ID", required = true)
			@PathVariable UUID scheduleId) {

		Integer capacity = inventoryService.getCurrentScheduleCapacity(scheduleId);

		if (capacity == null) {
			throw new IllegalArgumentException("스케줄을 찾을 수 없습니다: " + scheduleId);
		}

		ScheduleCapacityResponse response = ScheduleCapacityResponse.builder()
				.scheduleId(scheduleId)
				.remainingCapacity(capacity)
				.status(getCapacityStatus(capacity))
				.build();

		return ok(response);
	}

	private String getStockStatus(Integer stock) {
		if (stock == 0) return "SOLD_OUT";
		if (stock <= 5) return "LOW_STOCK";
		if (stock <= 20) return "NORMAL";
		return "SUFFICIENT";
	}

	private String getCapacityStatus(Integer capacity) {
		if (capacity == 0) return "FULL";
		if (capacity <= 3) return "ALMOST_FULL";
		if (capacity <= 10) return "NORMAL";
		return "AVAILABLE";
	}

	@Getter
	@Builder
	public static class GoodsStockResponse {
		private UUID goodsVariantId;
		private Integer currentStock;
		private String status; // SOLD_OUT, LOW_STOCK, NORMAL, SUFFICIENT
	}

	@Getter
	@Builder
	public static class ScheduleCapacityResponse {
		private UUID scheduleId;
		private Integer remainingCapacity;
		private String status; // FULL, ALMOST_FULL, NORMAL, AVAILABLE
	}
}