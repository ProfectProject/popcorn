package com.popcorn.demo.domain.store.controller;

import com.popcorn.demo.domain.store.exception.StoreException;
import com.popcorn.demo.domain.users.entity.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.auth.dto.CustomUserDetails;
import com.popcorn.demo.domain.store.dto.CreateStoreRequest;
import com.popcorn.demo.domain.store.dto.StoreCreatedDto;
import com.popcorn.demo.domain.store.dto.StoreDeletedDto;
import com.popcorn.demo.domain.store.dto.StoreDetailDto;
import com.popcorn.demo.domain.store.dto.StoreListDto;
import com.popcorn.demo.domain.store.dto.StoreStatusUpdatedDto;
import com.popcorn.demo.domain.store.dto.StoreUpdatedDto;
import com.popcorn.demo.domain.store.dto.UpdateStoreRequest;
import com.popcorn.demo.domain.store.dto.UpdateStoreStatusRequest;
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
            @Parameter(description = "스토어 생성 요청 데이터", required = true) @Valid @RequestBody CreateStoreRequest request,
            Authentication authentication) {

        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(storeService.createStore(userId, request)));
    }


    @Operation(summary = "내 가게 목록 조회", description = "오너의 모든 가게 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping("")
    public ResponseEntity<BaseResponse<List<StoreListDto>>> getMyStores(Authentication authentication) {
        
        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(BaseResponse.success(storeService.getStoresByOwnerId(userId)));
    }


    @Operation(summary = "가게 상세 조회", description = "특정 가게의 상세 정보를 조회합니다.")
    @GetMapping("/{storeId}")
    public ResponseEntity<BaseResponse<StoreDetailDto>> getStoreDetail(Authentication authentication,
            @PathVariable UUID storeId) {
        
        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(BaseResponse.success(storeService.getStoreDetail(userId, storeId)));
    }


    @Operation(summary = "가게 정보 수정", description = "가게의 기본 정보를 수정합니다.")
    @PutMapping("/{storeId}")
    public ResponseEntity<BaseResponse<StoreUpdatedDto>> updateStore(
            Authentication authentication,
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreRequest request) {
        
        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(BaseResponse.success(storeService.updateStore(storeId, request, userId)));
    }


    @Operation(summary = "가게 삭제", description = "가게를 삭제합니다 (Soft Delete).")
    @DeleteMapping("/{storeId}")
    public ResponseEntity<BaseResponse<StoreDeletedDto>> deleteStore(
            Authentication authentication,
            @PathVariable UUID storeId) {
        
        Long userId = getCurrentOwnerId(authentication);
        StoreDeletedDto deletedStore = storeService.deleteStore(storeId, userId);
        return ResponseEntity.ok(BaseResponse.success(deletedStore));
    }

    @Operation(summary = "가게 상태 변경", description = "가게의 발행 상태를 변경합니다.")
    @PatchMapping("/{storeId}/status")
    public ResponseEntity<BaseResponse<StoreStatusUpdatedDto>> updateStoreStatus(
            Authentication authentication,
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreStatusRequest request) {
        
        Long userId = getCurrentOwnerId(authentication);
        return ResponseEntity.ok(BaseResponse.success(storeService.updateStoreStatus(storeId, request, userId)));
    }

    // 인증 정보에서 오너 ID를 추출하고 OWNER 권한을 확인합니다.
    private Long getCurrentOwnerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw StoreException.unauthenticated();
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw StoreException.invalidPrincipal();
        }

        Long userId = userDetails.getUserId();
        if (userId == null) {
            throw StoreException.userIdRequired();
        }

        UserRole role;
        try {
            role = UserRole.valueOf(userDetails.getRole());
        } catch (Exception e) {
            throw StoreException.invalidRole();
        }

        if (role != UserRole.OWNER) {
            throw StoreException.notOwner();
        }

        return userId;
    }

}
