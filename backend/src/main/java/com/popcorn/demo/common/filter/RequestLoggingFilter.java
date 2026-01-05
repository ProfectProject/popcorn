package com.popcorn.demo.common.filter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * API 요청/응답 공통 로그 필터
 *
 * - 모든 HTTP 요청에 대해 처리 시간과 응답 코드를 기록합니다.
 * - Swagger/Actuator 등 시스템 경로는 노이즈를 줄이기 위해 제외합니다.
 * - 요청 바디는 로그에 남기지 않습니다(민감정보/용량 문제 방지).
 */
@Component
@Order(2)
public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	@Override
	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.startsWith("/actuator")
				|| path.startsWith("/v3/api-docs")
				|| path.startsWith("/swagger-ui")
				|| path.startsWith("/webjars")
				|| "/swagger-ui.html".equals(path);
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {
		Instant start = Instant.now();
		try {
			filterChain.doFilter(request, response);
		} finally {
			long elapsedMs = Duration.between(start, Instant.now()).toMillis();
			String method = request.getMethod();
			String uri = request.getRequestURI();
			String query = request.getQueryString();
			String path = query == null ? uri : uri + "?" + query;
			int status = response.getStatus();
			log.info("API {} {} -> {} ({} ms)", method, path, status, elapsedMs);
		}
	}
}
