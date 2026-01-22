package com.popcorn.common.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Gateway에서 전달받은 사용자 정보 헤더를 기반으로 Spring Security Context 설정
 *
 * Gateway JWT Filter에서 다음 헤더들을 전달받음:
 * - X-User-Id: 사용자 ID
 * - X-User-Email: 사용자 이메일
 * - X-User-Role: 사용자 역할
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Gateway에서 전달한 사용자 정보 헤더 추출
        String userId = request.getHeader(USER_ID_HEADER);
        String email = request.getHeader(USER_EMAIL_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);

        if (userId != null && email != null && role != null) {
            try {
                // PassportPrincipal 객체 생성
                PassportPrincipal principal = PassportPrincipal.builder()
                        .userId(Long.valueOf(userId))
                        .email(email)
                        .role(role)
                        .name(email) // 이름으로 이메일 사용
                        .build();

                // Spring Security Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

                // Security Context에 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authentication set for user: {} with role: {}", email, role);
            } catch (Exception e) {
                log.error("Failed to process user headers: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}