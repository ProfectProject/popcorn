package com.popcorn.gateway.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import reactor.core.publisher.Mono;

@Component
public class EventHttpLoggingFilter implements ExchangeFilterFunction {

    private static final Logger log = LoggerFactory.getLogger(EventHttpLoggingFilter.class);

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        String eventName = MDC.get("event.name");
        String handler = MDC.get("event.handler");
        if (eventName == null || handler == null) {
            return next.exchange(request);
        }

        long startTime = System.currentTimeMillis();
        return next.exchange(request)
            .doOnNext(response -> log.info(
                "Event HTTP response: event={} handler={} method={} url={} status={} durationMs={}",
                eventName,
                handler,
                request.method(),
                request.url(),
                response.statusCode(),
                System.currentTimeMillis() - startTime))
            .doOnError(error -> log.error(
                "Event HTTP failed: event={} handler={} method={} url={} error={}",
                eventName,
                handler,
                request.method(),
                request.url(),
                error.toString()));
    }
}
