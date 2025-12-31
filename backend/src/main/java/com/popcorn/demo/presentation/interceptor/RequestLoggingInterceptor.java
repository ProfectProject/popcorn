package com.popcorn.demo.presentation.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

public class RequestLoggingInterceptor implements WebFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
	private static final String START_TIME_ATTR = "requestStartTime";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

		log.info("Incoming request: {} {} traceId={}",
			exchange.getRequest().getMethod(),
			exchange.getRequest().getURI().getPath(),
			MDC.get("traceId"));

		return chain.filter(exchange)
				.doFinally(signalType -> {
					Object startTime = exchange.getAttribute(START_TIME_ATTR);
					long elapsedMs = startTime instanceof Long
							? System.currentTimeMillis() - (Long) startTime
							: -1L;
					log.info("Completed request: {} {} -> {} ({}ms) traceId={}",
						exchange.getRequest().getMethod(),
						exchange.getRequest().getURI().getPath(),
						exchange.getResponse().getStatusCode(),
						elapsedMs,
						MDC.get("traceId"));
				});
	}
}
