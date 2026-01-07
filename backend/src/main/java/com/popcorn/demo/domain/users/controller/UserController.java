package com.popcorn.demo.domain.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.domain.auth.dto.CustomUserDetails;
import com.popcorn.demo.domain.users.dto.SignupRequest;
import com.popcorn.demo.domain.users.dto.SignupResponse;
import com.popcorn.demo.domain.users.dto.UserResponse;
import com.popcorn.demo.domain.users.dto.UserUpdateRequest;
import com.popcorn.demo.domain.users.entity.User;
import com.popcorn.demo.domain.users.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public SignupResponse postMethodName(@RequestBody SignupRequest request) {
        
        return userService.register(request);
    }

    /**
     * 사용자 정보 조회
     */
    // TODO: filter 이용해서 현재 사용자 정보 조회하도록 수정
    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        System.out.println("user: " + user);
        return UserResponse.from(user);
    }

    //  인증된 사용자 조회
    @GetMapping("/mypage")
    public UserResponse getMyInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        // SecurityContext에서 userId 가져오기
        Long userId = customUserDetails.getUserId();
        System.out.println("SecurityContext에서 가져온 userId: " + userId);

        // DB에서 실제 유저 정보 조회
        User user = userService.getUserById(userId);

        return UserResponse.from(user);
    }

     /**
     * 사용자 정보 업데이트
     */
    @PutMapping("/{userId}")
    public UserResponse updateUser(@PathVariable Long userId, @RequestBody UserUpdateRequest request) {
        User updatedUser = userService.updateUser(userId, request);
        return UserResponse.from(updatedUser);
    }

    /**
     * 사용자 계정 탈퇴
     */
    /*@DeleteMapping("/me/deactivate")
    public ResponseEntity<Void> deactivateUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.deactivateUser(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }*/

}
