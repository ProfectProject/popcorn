package com.popcorn.demo.domain.store.controller;

import com.popcorn.demo.domain.store.dto.StoreDetailDto;
import com.popcorn.demo.domain.store.dto.StoreListDto;
import com.popcorn.demo.domain.store.dto.UpdateStoreRequest;
import com.popcorn.demo.domain.store.dto.StoreUpdatedDto;
import com.popcorn.demo.domain.store.dto.UpdateStoreStatusRequest;
import com.popcorn.demo.domain.store.dto.StoreDeletedDto;
import com.popcorn.demo.domain.store.dto.StoreStatusUpdatedDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RequestMapping("/api/v1/owner/stores")
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
            @Parameter(description = "스토어 생성 요청 데이터", required = true) @Valid @RequestBody CreateStoreRequest request) {

        Long userId = getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(storeService.createStore(userId, request)));
    }


    @Operation(summary = "내 가게 목록 조회", description = "오너의 모든 가게 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("")
    public ResponseEntity<BaseResponse<List<StoreListDto>>> getMyStores() {
        
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(BaseResponse.success(storeService.getStoresByOwnerId(userId)));
    }

    // TODO: 2. 가게 상세 조회 API (우선순위: 2, 예상시간: 20분) - COMPLETED
    @Operation(summary = "가게 상세 조회", description = "특정 가게의 상세 정보를 조회합니다.")
    @GetMapping("/{storeId}")
    public ResponseEntity<BaseResponse<StoreDetailDto>> getStoreDetail(
            @PathVariable UUID storeId) {
        
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(BaseResponse.success(storeService.getStoreDetail(userId, storeId)));
    }

    // TODO: 3. 가게 기본 정보 수정 API (우선순위: 3, 예상시간: 40분) - COMPLETED
    @Operation(summary = "가게 정보 수정", description = "가게의 기본 정보를 수정합니다.")
    @PutMapping("/{storeId}")
    public ResponseEntity<BaseResponse<StoreUpdatedDto>> updateStore(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreRequest request) {
        
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(BaseResponse.success(storeService.updateStore(storeId, request, userId)));
    }

    // TODO: 4. 가게 삭제 API (우선순위: 4, 예상시간: 25분) - COMPLETED
    @Operation(summary = "가게 삭제", description = "가게를 삭제합니다 (Soft Delete).")
    @DeleteMapping("/{storeId}")
    public ResponseEntity<BaseResponse<StoreDeletedDto>> deleteStore(
            @PathVariable UUID storeId) {
        
        Long userId = getCurrentUserId();
        StoreDeletedDto deletedStore = storeService.deleteStore(storeId, userId);
        return ResponseEntity.ok(BaseResponse.success(deletedStore));
    }

    // TODO: 5. 가게 상태 변경 API (우선순위: 5, 예상시간: 30분) - COMPLETED
    @Operation(summary = "가게 상태 변경", description = "가게의 발행 상태를 변경합니다.")
    @PatchMapping("/{storeId}/status")
    public ResponseEntity<BaseResponse<StoreStatusUpdatedDto>> updateStoreStatus(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreStatusRequest request) {
        
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(BaseResponse.success(storeService.updateStoreStatus(storeId, request, userId)));
    }
    
    /**
     * JWT 토큰에서 현재 사용자 ID를 추출
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증되지 않은 사용자입니다");
        }
        

        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new RuntimeException("잘못된 사용자 ID 형식입니다");
        }
    }
}