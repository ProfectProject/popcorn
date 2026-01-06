package com.popcorn.demo.domain.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.domain.users.dto.SignupRequest;
import com.popcorn.demo.domain.users.dto.SignupResponse;
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

    //@PreAuthorize("hasRole('USER')")
    @GetMapping("/roleTest")
    public ResponseEntity<?> adminSettings() {
        return ResponseEntity.ok("권한부여 테스트");
    }
}
