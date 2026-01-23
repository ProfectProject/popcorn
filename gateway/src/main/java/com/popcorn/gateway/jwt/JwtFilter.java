package com.popcorn.gateway.jwt;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter implements GlobalFilter, Ordered{
    private static final List<String> EXCLUDE_PATH_PREFIXES = List.of(
            // 인증 관련 경로
            "/api/users/v1/auth/login",
            "/api/users/v1/users/signup",
            "/api/pay/v1/payments/decode",
            "/api/pay/v1/payments/confirm-async",
            "/api/pay/v1/payments/orders",
            "/api/stores/v1/popups",

            // Swagger/OpenAPI 관련 경로 (전체) - 포괄적 설정
            "/v3/api-docs",           // 모든 서비스 OpenAPI 문서
            "/swagger-ui",            // Swagger UI 리소스
            "/webjars",               // Swagger UI 의존성 (CSS, JS 등)
            "/openapi",               // Gateway OpenAPI 프록시
            "/swagger-resources",     // Swagger 리소스
            "/configuration",         // Swagger 설정

            // 각 서비스별 Swagger 경로 (모든 하위 경로 포함)
            "/api/users/v3",
            "/api/users/swagger-ui",
            "/api/stores/v3",
            "/api/stores/swagger-ui",
            "/api/orders/v3",
            "/api/orders/swagger-ui",
            "/api/pay/v3",
            "/api/pay/swagger-ui",
            "/api/qr/v3",
            "/api/qr/swagger-ui",
            "/api/orderquery/v3",
            "/api/orderquery/swagger-ui",
            "/api/backend/v3",
            "/api/backend/swagger-ui",
            "/api/stores/v3/api-docs",
            "/api/stores/v3/api-docs/public",

            // 기타 정적 리소스
            "/favicon.ico",
            "/actuator/health"        // 헬스체크는 JWT 없이도 접근 가능
    );

    private final JwtUtil jwtUtils;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        // 모든 요청 로깅
        log.info("🌐 Gateway 요청 - Method: {}, Path: {}", method, path);

        // OPTIONS 요청은 항상 허용
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            log.info("🔓 OPTIONS 요청 통과: {}", path);
            return chain.filter(exchange);
        }

        // 모든 OpenAPI/Swagger 경로 무조건 허용 (최고 우선순위)
        if (path.contains("api-docs") ||
            path.contains("swagger") ||
            path.contains("webjars") ||
            path.contains("openapi") ||
            path.equals("/") ||
            path.equals("/swagger-ui.html")) {
            return chain.filter(exchange);
        }

        // 기타 예외 경로 처리
        if (EXCLUDE_PATH_PREFIXES.stream().anyMatch(path::startsWith)) {
            log.info("🔓 예외 경로 통과: {}", path);
            return chain.filter(exchange);
        }

        // 2) Authorization Header 체크
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        log.info("🔐 Authorization 헤더: {}", authHeader != null ? "Bearer ***" : "없음");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("❌ Authorization 헤더 없음 또는 형식 오류");
            return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        log.info("🔐 JWT 토큰 추출 완료 (길이: {})", token.length());

        // 3) JWT 유효성 검사
        if (!jwtUtils.validateToken(token)) {
            log.warn("❌ JWT 토큰 검증 실패");
            return onError(exchange, "Invalid Token", HttpStatus.UNAUTHORIZED);
        }

        log.info("✅ JWT 토큰 검증 성공");
        
        // 4) 검증 성공 → 사용자 정보 헤더 전달 후 PROXY 통과
        String userId = String.valueOf(jwtUtils.getUserId(token));
        String email = jwtUtils.getUsername(token);
        String role = jwtUtils.getRole(token);

        // 디버그 로깅 추가
        log.info("🔍 JWT 파싱 성공 - userId: {}, email: {}, role: {}", userId, email, role);
        log.info("🔍 요청 경로: {}", exchange.getRequest().getPath().value());
        log.info("🔍 헤더 설정 - X-User-Id: {}, X-User-Email: {}, X-User-Role: {}", userId, email, role);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(builder -> builder
                        .header("X-User-Id", userId)
                        .header("X-User-Email", email)
                        .header("X-User-Role", role)
                )
                .build();

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return -1; // GlobalFilter에서 가장 먼저 실행되도록
    }


    private boolean isSwaggerOrOpenApiPath(String path) {
        return path.contains("/v3/api-docs") ||
               path.contains("/swagger-ui") ||
               path.contains("/webjars") ||
               path.contains("/openapi") ||
               path.contains("/swagger-resources") ||
               path.contains("/configuration") ||
               path.equals("/swagger-ui.html") ||
               path.equals("/") ||  // 루트 경로도 허용
               path.startsWith("/api/") && (
                   path.contains("/v3/") ||
                   path.contains("/swagger-ui")
               );
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        log.error("🚫 Gateway 인증 실패 - 경로: {}, 오류: {}, 상태: {}",
                exchange.getRequest().getURI().getPath(), err, status);
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
    private boolean isDocumentationRequest(String path) {
        return path.contains("/v3/api-docs") || path.contains("/swagger-ui") || path.contains("/webjars");
    }
}
