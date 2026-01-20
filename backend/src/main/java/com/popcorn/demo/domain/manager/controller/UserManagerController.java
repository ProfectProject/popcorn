package com.popcorn.demo.domain.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.common.controller.BaseController;
import com.popcorn.common.dto.BaseResponse;
import com.popcorn.demo.domain.manager.service.UserManagerService;
import com.popcorn.demo.domain.users.dto.manager.OwnerApproveResponse;
import com.popcorn.demo.domain.users.dto.manager.OwnerForceStopRequest;
import com.popcorn.demo.domain.users.dto.manager.OwnerForceStopResponse;
import com.popcorn.demo.domain.users.dto.manager.UserForceStopRequest;
import com.popcorn.demo.domain.users.dto.manager.UserForceStopResponse;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "유저매니저", description = "유저 매니저 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager")
public class UserManagerController extends BaseController {

    private final UserManagerService userManagerService;

    @PostMapping("/owner/{userId}/approve")
    public ResponseEntity<BaseResponse<OwnerApproveResponse>> approveOwner(@PathVariable Long userId) {
        return ok(userManagerService.approveOwner(userId));
    }

    @PostMapping("/owner/{userId}/force_stop")
    public ResponseEntity<BaseResponse<OwnerForceStopResponse>> forceStopOwner(
            @PathVariable Long userId,
            @RequestBody(required = false) OwnerForceStopRequest request) {
        return ok(userManagerService.forceStopOwner(userId));
    }

    @PostMapping("/users/{userId}/force_stop")
    public ResponseEntity<BaseResponse<UserForceStopResponse>> forceStopUser(
            @PathVariable Long userId,
            @RequestBody UserForceStopRequest request) {
        return ok(userManagerService.forceStopUser(userId, request));
    }
}
