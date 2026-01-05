package com.popcorn.demo.domain.store.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.store.dto.CreateStoreRequest;
import com.popcorn.demo.domain.store.dto.StoreCreatedDto;
import com.popcorn.demo.domain.store.service.StoreService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Stores", description = "스토어 관리 API")
@RestController
@RequestMapping("/api/v1/stores")
public class StoreController extends BaseController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }
    
    @Operation(summary = "스토어 생성", description = "새로운 스토어를 생성합니다. 오너 권한이 필요합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "스토어 생성 성공", 
                    content = @Content(schema = @Schema(implementation = StoreCreatedDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "409", description = "중복된 스토어 이름")
    })
    @PostMapping
    public ResponseEntity<BaseResponse<StoreCreatedDto>> createStore(
            @Parameter(description = "스토어 생성 요청 데이터", required = true) @Valid @RequestBody CreateStoreRequest request,
            @Parameter(description = "인증된 사용자 ID", required = true) @RequestHeader("X-User-Id") Long userId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(storeService.createStore(userId, request)));
    }

    // TODO: 1. 내 가게 정보 조회 API (우선순위: 1, 예상시간: 30분)
    @Operation(summary = "내 가게 목록 조회", description = "오너의 모든 가게 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<BaseResponse<List<StoreListDto>>> getMyStores(
            @PathVariable Long ownerId,
            @RequestHeader("X-User-Id") Long userId) {
        // 권한 체크: userId == ownerId
        // storeService.getStoresByOwnerId(ownerId) 호출
        // BaseResponse.success()로 래핑하여 반환
        return ResponseEntity.ok(BaseResponse.success(storeService.getStoresByOwnerId(ownerId)));
    }

    // TODO: 2. 가게 상세 조회 API (우선순위: 2, 예상시간: 20분)
    @Operation(summary = "가게 상세 조회", description = "특정 가게의 상세 정보를 조회합니다.")
    @GetMapping("/{storeId}")
    public ResponseEntity<BaseResponse<StoreDetailDto>> getStoreDetail(
            @PathVariable UUID storeId,
            @RequestHeader("X-User-Id") Long userId) {
        // storeService.getStoreDetail(storeId, userId) 호출
        return ResponseEntity.ok(BaseResponse.success(storeService.getStoreDetail(storeId, userId)));
    }

    // TODO: 3. 가게 기본 정보 수정 API (우선순위: 3, 예상시간: 40분)
    @Operation(summary = "가게 정보 수정", description = "가게의 기본 정보를 수정합니다.")
    @PutMapping("/{storeId}")
    public ResponseEntity<BaseResponse<StoreUpdatedDto>> updateStore(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        // storeService.updateStore(storeId, request, userId) 호출
        return ResponseEntity.ok(BaseResponse.success(storeService.updateStore(storeId, request, userId)));
    }

    // TODO: 4. 가게 삭제 API (우선순위: 4, 예상시간: 25분)
    @Operation(summary = "가게 삭제", description = "가게를 삭제합니다 (Soft Delete).")
    @DeleteMapping("/{storeId}")
    public ResponseEntity<BaseResponse<Void>> deleteStore(
            @PathVariable UUID storeId,
            @RequestHeader("X-User-Id") Long userId) {
        // storeService.deleteStore(storeId, userId) 호출
        storeService.deleteStore(storeId, userId);
        return ResponseEntity.ok(BaseResponse.success());
    }

    // TODO: 5. 가게 상태 변경 API (우선순위: 5, 예상시간: 30분)
    @Operation(summary = "가게 상태 변경", description = "가게의 발행 상태를 변경합니다.")
    @PatchMapping("/{storeId}/status")
    public ResponseEntity<BaseResponse<StoreStatusUpdatedDto>> updateStoreStatus(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreStatusRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        // storeService.updateStoreStatus(storeId, request, userId) 호출
        return ResponseEntity.ok(BaseResponse.success(storeService.updateStoreStatus(storeId, request, userId)));
    }
}