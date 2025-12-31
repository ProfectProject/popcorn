package com.popcorn.demo.common.filter;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class RequestTraceFilter implements WebFilter {

	private static final String TRACE_ID_KEY = "traceId";

	/**
	 * 요청 단위 추적 ID 생성 및 MDC에 주입
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String traceId = UUID.randomUUID().toString();
		MDC.put(TRACE_ID_KEY, traceId);
		return chain.filter(exchange)
				.doFinally(signalType -> MDC.remove(TRACE_ID_KEY));
	}
}
