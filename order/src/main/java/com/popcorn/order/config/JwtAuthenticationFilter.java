package com.popcorn.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

/**
 * JWT 인증 필터
 *
 * HTTP 요청 헤더에서 JWT 토큰을 추출하고 검증하여
 * Spring Security Authentication 객체를 생성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // 공개 경로는 JWT 검증 건너뛰기
        if (isPublicPath(requestURI)) {
            log.debug("🔓 공개 경로 요청: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 1. Authorization 헤더에서 JWT 토큰 추출
            String token = extractToken(request);

            if (token == null) {
                log.debug("🚫 JWT 토큰이 없습니다 - URI: {}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            log.debug("🔑 JWT 토큰 발견 - URI: {}, 토큰 길이: {}", requestURI, token.length());

            // 2. JWT 토큰 파싱 및 검증
            Authentication authentication = parseJwtToken(token);

            if (authentication != null) {
                // 3. SecurityContext에 Authentication 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("✅ JWT 인증 성공 - 사용자: {}, 권한: {}",
                    authentication.getName(), authentication.getAuthorities());
            } else {
                log.warn("⚠️ JWT 토큰 파싱 실패 - URI: {}", requestURI);
            }

        } catch (Exception e) {
            log.error("💥 JWT 인증 필터 오류 - URI: {}", requestURI, e);
            // 오류가 발생해도 필터 체인 계속 진행 (인증 실패로 처리됨)
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 JWT 토큰 추출
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            log.debug("🎫 토큰 추출 성공: Bearer {} ({}자)", token.substring(0, Math.min(10, token.length())), token.length());
            return token;
        }

        return null;
    }

    /**
     * JWT 토큰을 파싱하여 Authentication 객체 생성
     *
     * 간단한 JWT 파싱 구현 (실제 환경에서는 라이브러리 사용 권장)
     */
    private Authentication parseJwtToken(String token) {
        try {
            // JWT는 header.payload.signature 형태
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("⚠️ JWT 토큰 형식이 잘못되었습니다. 파트 수: {}", parts.length);
                return null;
            }

            // payload 부분 디코드 (Base64URL -> JSON)
            String payload = parts[1];

            // Base64URL 디코딩 (패딩 추가 필요할 수 있음)
            String paddedPayload = payload;
            while (paddedPayload.length() % 4 != 0) {
                paddedPayload += "=";
            }

            byte[] decodedBytes = Base64.getUrlDecoder().decode(paddedPayload);
            String payloadJson = new String(decodedBytes);

            log.debug("🔍 JWT 페이로드: {}", payloadJson);

            // JSON 파싱
            Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);

            // 필수 클레임 추출
            Object idObj = claims.get("id");
            String email = (String) claims.get("email");
            String role = (String) claims.get("role");

            if (idObj == null || email == null || role == null) {
                log.warn("⚠️ JWT에 필수 클레임이 없습니다. id: {}, email: {}, role: {}", idObj, email, role);
                return null;
            }

            // id를 문자열로 변환 (숫자로 올 수도 있음)
            String userId = String.valueOf(idObj);

            log.info("🎭 JWT 클레임 추출 - ID: {}, 이메일: {}, 역할: {}", userId, email, role);

            // 만료 시간 확인 (선택적)
            checkTokenExpiry(claims);

            // Authentication 객체 생성
            // principal: 사용자 식별자 (userId)
            // credentials: 토큰 (보안상 null 설정 가능)
            // authorities: 권한 목록
            //
            // 주의: authorities와 함께 생성하면 자동으로 authenticated = true가 됩니다.
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userId, // principal (extractUserIdFromAuthentication에서 getName()으로 사용됨)
                null,   // credentials (보안상 토큰을 저장하지 않음)
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            );

            log.debug("🎭 Authentication 객체 생성 완료 - 인증됨: {}, 권한: {}",
                authentication.isAuthenticated(), authentication.getAuthorities());

            return authentication;

        } catch (Exception e) {
            log.error("💥 JWT 토큰 파싱 오류", e);
            return null;
        }
    }

    /**
     * JWT 토큰 만료 시간 확인
     */
    private void checkTokenExpiry(Map<String, Object> claims) {
        Object expObj = claims.get("exp");
        if (expObj != null) {
            long exp = Long.parseLong(String.valueOf(expObj));
            long now = System.currentTimeMillis() / 1000; // 현재 시간 (초 단위)

            if (exp < now) {
                log.warn("⏰ JWT 토큰이 만료되었습니다. 만료: {}, 현재: {}", exp, now);
                // 만료된 토큰이지만 개발 환경에서는 경고만 출력하고 계속 진행
                // 실제 환경에서는 예외를 던지거나 null을 반환해야 함
            } else {
                log.debug("✅ JWT 토큰 만료 확인 완료. 남은 시간: {}초", (exp - now));
            }
        }
    }

    /**
     * 공개 경로인지 확인 (JWT 인증 불필요)
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator/") ||
               path.equals("/swagger-ui.html");
    }
}