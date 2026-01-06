package com.popcorn.demo.domain.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.popcorn.demo.domain.auth.dto.CustomUserDetails;
import com.popcorn.demo.domain.auth.dto.LoginRequest;
import com.popcorn.demo.domain.auth.dto.LoginResponse;
import com.popcorn.demo.domain.auth.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                );

        Authentication authentication = authenticationManager.authenticate(authToken);

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        String role = customUserDetails.getAuthorities()
                .iterator().next()
                .getAuthority()
                .replace("ROLE_", "");

        String jwt = jwtUtil.createJwt(customUserDetails.getUsername(), role, 60 * 60 * 1000L);

        return new LoginResponse(jwt);
    }
}
