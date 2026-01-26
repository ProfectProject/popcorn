package com.popcorn.payment.http

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

@Component
class EventHttpLoggingFilter : ExchangeFilterFunction {
    private val log = LoggerFactory.getLogger(EventHttpLoggingFilter::class.java)

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        val eventName = MDC.get("event.name")
        val handler = MDC.get("event.handler")
        if (eventName == null || handler == null) {
            return next.exchange(request)
        }
        val startTime = System.currentTimeMillis()
        return next.exchange(request)
            .doOnNext { response ->
                log.info(
                    "Event HTTP response: event={} handler={} method={} url={} status={} durationMs={}",
                    eventName,
                    handler,
                    request.method(),
                    request.url(),
                    response.statusCode(),
                    System.currentTimeMillis() - startTime
                )
            }
            .doOnError { error ->
                log.error(
                    "Event HTTP failed: event={} handler={} method={} url={} error={}",
                    eventName,
                    handler,
                    request.method(),
                    request.url(),
                    error.toString()
                )
            }
    }
}
