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
    private static final int MAX_BODY_LENGTH = 1000;

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        String eventName = MDC.get("event.name");
        String handler = MDC.get("event.handler");
        if (eventName == null || handler == null) {
            return next.exchange(request);
        }

        long startTime = System.currentTimeMillis();
        return next.exchange(request)
            .flatMap(response -> response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> {
                    log.info(
                        "Event HTTP response: event={} handler={} method={} url={} status={} durationMs={} body={}",
                        eventName,
                        handler,
                        request.method(),
                        request.url(),
                        response.statusCode(),
                        System.currentTimeMillis() - startTime,
                        summarizeBody(body)
                    );
                    return ClientResponse.create(response.statusCode())
                        .headers(headers -> headers.addAll(response.headers().asHttpHeaders()))
                        .cookies(cookies -> cookies.addAll(response.cookies()))
                        .body(body)
                        .build();
                }))
            .doOnError(error -> log.error(
                "Event HTTP failed: event={} handler={} method={} url={} error={}",
                eventName,
                handler,
                request.method(),
                request.url(),
                error.toString()));
    }

    private static String summarizeBody(String body) {
        if (body.length() <= MAX_BODY_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_BODY_LENGTH) + "...(truncated)";
    }
}
