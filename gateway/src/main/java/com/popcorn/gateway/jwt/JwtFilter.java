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
            "/api/users/v1/auth/login",
            "/api/users/v1/users/signup",
            "/api/users/v3/api-docs",
            "/api/users/swagger-ui",
            "/v3/api-docs",
            "/swagger-ui"
    );

    private final JwtUtil jwtUtils;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        // 1) 로그인/회원가입, 문서 요청은 필터 제외
        if (EXCLUDE_PATH_PREFIXES.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // 2) Authorization Header 체크
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);

        // 3) JWT 유효성 검사
        if (!jwtUtils.validateToken(token)) {
            return onError(exchange, "Invalid Token", HttpStatus.UNAUTHORIZED);
        }
        
        // 4) 검증 성공 → 사용자 정보 헤더 전달 후 PROXY 통과
        String userId = String.valueOf(jwtUtils.getUserId(token));
        String email = jwtUtils.getUsername(token);
        String role = jwtUtils.getRole(token);

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

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}