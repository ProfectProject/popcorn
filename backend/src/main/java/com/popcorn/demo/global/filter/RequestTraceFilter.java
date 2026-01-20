package com.popcorn.demo.global.filter;

import java.util.UUID;

import org.slf4j.MDC;
import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestTraceFilter extends OncePerRequestFilter {

	private static final String TRACE_ID_KEY = "traceId";

	/**
	 * 요청 단위 추적 ID 생성 및 MDC에 주입
	 */
	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		String existingTraceId = MDC.get(TRACE_ID_KEY);

		// 기존 traceId가 없을 때만 새로 생성
		if (existingTraceId == null) {
			String traceId = UUID.randomUUID().toString();
			MDC.put(TRACE_ID_KEY, traceId);
		}

        filterChain.doFilter(request, response);
    }
}
