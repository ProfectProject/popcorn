package com.popcorn.demo.domain.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.manager.service.UserManagerService;
import com.popcorn.demo.domain.users.dto.manager.OwnerApproveResponse;
import com.popcorn.demo.domain.users.dto.manager.UserForceStopRequest;
import com.popcorn.demo.domain.users.dto.manager.UserForceStopResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager")
public class UserManagerController extends BaseController {

    private final UserManagerService userManagerService;

    @PostMapping("/owner/{userId}/approve")
    public ResponseEntity<BaseResponse<OwnerApproveResponse>> approveOwner(@PathVariable Long userId) {
        return ok(userManagerService.approveOwner(userId));
    }

    @PostMapping("/users/{userId}/force_stop")
    public ResponseEntity<BaseResponse<UserForceStopResponse>> forceStopUser(
            @PathVariable Long userId,
            @RequestBody UserForceStopRequest request) {
        return ok(userManagerService.forceStopUser(userId, request));
    }
}
