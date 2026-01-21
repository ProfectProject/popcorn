package com.popcorn.order.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * JWT 인증 정보 추출 유틸리티
 *
 * [Java 초보자를 위한 가이드]
 *
 * 이 클래스가 하는 일:
 * - JWT 토큰에서 추출된 사용자 정보를 가져옴
 * - Spring Security의 Authentication 객체에서 데이터 추출
 * - 컨트롤러에서 하드코딩된 사용자 ID를 실제 JWT에서 추출된 값으로 대체
 *
 * JWT 인증 플로우:
 * 1. 클라이언트가 Authorization: Bearer <JWT_TOKEN> 헤더로 요청
 * 2. JwtAuthenticationFilter가 토큰을 파싱하여 Authentication 객체 생성
 * 3. 이 클래스가 Authentication 객체에서 사용자 정보 추출
 *
 * 보안 고려사항:
 * - 인증되지 않은 사용자는 기본값을 반환하거나 예외 발생
 * - 잘못된 JWT 토큰은 null 또는 예외 처리
 */
@Slf4j
@Component
public class AuthenticationUtil {

    /**
     * 현재 인증된 사용자의 ID를 추출
     *
     * [사용 예시]
     * @GetMapping("/api/orders/v1/me")
     * public ResponseEntity<?> getMyOrders(Authentication auth) {
     *     Long userId = AuthenticationUtil.extractUserIdFromAuthentication(auth);
     *     // userId를 사용하여 해당 사용자의 주문 조회
     * }
     *
     * @param authentication Spring Security Authentication 객체
     * @return 사용자 ID (Long 타입)
     * @throws IllegalArgumentException 인증 정보가 없거나 잘못된 경우
     */
    public static Long extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            throw new IllegalArgumentException("인증 정보가 올바르지 않습니다.");
        }

        try {
            // JwtAuthenticationFilter에서 userId를 String으로 설정하므로 변환 필요
            String userIdStr = principal.toString();
            Long userId = Long.parseLong(userIdStr);

            log.debug("🔍 사용자 ID 추출 성공: {}", userId);
            return userId;

        } catch (NumberFormatException e) {
            log.error("❌ 사용자 ID 형식 오류: principal = {}", principal);
            throw new IllegalArgumentException("사용자 ID 형식이 올바르지 않습니다: " + principal);
        }
    }

    /**
     * SecurityContext에서 현재 인증된 사용자의 ID를 추출
     *
     * [사용 예시]
     * @GetMapping("/api/orders/v1/me")
     * public ResponseEntity<?> getMyOrders() {
     *     Long userId = AuthenticationUtil.getCurrentUserId();
     *     // userId를 사용하여 해당 사용자의 주문 조회
     * }
     *
     * @return 현재 사용자 ID
     * @throws IllegalArgumentException 인증 정보가 없거나 잘못된 경우
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return extractUserIdFromAuthentication(authentication);
    }

    /**
     * 현재 인증된 사용자의 권한(role)을 추출
     *
     * [사용 예시]
     * 관리자만 접근 가능한 기능에서 권한 확인:
     * String role = AuthenticationUtil.getCurrentUserRole();
     * if (!"ADMIN".equals(role)) {
     *     throw new AccessDeniedException("관리자 권한이 필요합니다.");
     * }
     *
     * @param authentication Spring Security Authentication 객체
     * @return 사용자 권한 (ADMIN, USER 등)
     */
    public static String extractUserRoleFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        if (authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) {
            log.warn("⚠️ 사용자에게 권한이 설정되지 않음");
            return "USER"; // 기본 권한
        }

        // 첫 번째 권한 추출 (ROLE_ 제거)
        String authority = authentication.getAuthorities().iterator().next().getAuthority();
        String role = authority.startsWith("ROLE_") ? authority.substring(5) : authority;

        log.debug("🎭 사용자 권한 추출: {}", role);
        return role;
    }

    /**
     * SecurityContext에서 현재 사용자 권한 추출
     */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return extractUserRoleFromAuthentication(authentication);
    }

    /**
     * 인증 정보의 유효성을 검증
     *
     * 컨트롤러에서 사용자 인증 상태를 미리 확인하고 싶을 때 사용
     *
     * @param authentication 검증할 Authentication 객체
     * @return 유효한 인증 정보인지 여부
     */
    public static boolean isValidAuthentication(Authentication authentication) {
        if (authentication == null) {
            log.debug("🚫 Authentication 객체가 null");
            return false;
        }

        if (!authentication.isAuthenticated()) {
            log.debug("🚫 인증되지 않은 사용자");
            return false;
        }

        if (authentication.getPrincipal() == null) {
            log.debug("🚫 Principal이 null");
            return false;
        }

        try {
            // 사용자 ID 추출이 가능한지 확인
            extractUserIdFromAuthentication(authentication);
            return true;
        } catch (Exception e) {
            log.debug("🚫 사용자 ID 추출 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 관리자 권한 확인
     *
     * @param authentication 확인할 Authentication 객체
     * @return 관리자 권한 여부
     */
    public static boolean isAdmin(Authentication authentication) {
        try {
            String role = extractUserRoleFromAuthentication(authentication);
            return "ADMIN".equalsIgnoreCase(role);
        } catch (Exception e) {
            log.debug("🚫 관리자 권한 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 매장 운영자 권한 확인
     *
     * @param authentication 확인할 Authentication 객체
     * @return 매장 운영자 권한 여부
     */
    public static boolean isStoreOwner(Authentication authentication) {
        try {
            String role = extractUserRoleFromAuthentication(authentication);
            return "STORE_OWNER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
        } catch (Exception e) {
            log.debug("🚫 매장 운영자 권한 확인 실패: {}", e.getMessage());
            return false;
        }
    }
}