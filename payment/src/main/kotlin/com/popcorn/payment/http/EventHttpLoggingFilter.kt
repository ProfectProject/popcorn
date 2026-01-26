package com.popcorn.payment.http

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

@Component
class EventHttpLoggingFilter : ExchangeFilterFunction {
    private val log = LoggerFactory.getLogger(EventHttpLoggingFilter::class.java)
    private val maxBodyLength = 1000

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        val eventName = MDC.get("event.name")
        val handler = MDC.get("event.handler")
        if (eventName == null || handler == null) {
            return next.exchange(request)
        }
        val startTime = System.currentTimeMillis()
        return next.exchange(request)
            .flatMap { response: ClientResponse ->
                response.bodyToMono(String::class.java)
                    .defaultIfEmpty("")
                    .map { body ->
                        log.info(
                            "Event HTTP response: event={} handler={} method={} url={} status={} durationMs={} body={}",
                            eventName,
                            handler,
                            request.method(),
                            request.url(),
                            response.statusCode(),
                            System.currentTimeMillis() - startTime,
                            summarizeBody(body)
                        )
                        ClientResponse.create(response.statusCode())
                            .headers { headers: HttpHeaders -> headers.addAll(response.headers().asHttpHeaders()) }
                            .cookies { cookies: MultiValueMap<String, ResponseCookie> -> cookies.addAll(response.cookies()) }
                            .body(body)
                            .build()
                    }
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

    private fun summarizeBody(body: String): String {
        return if (body.length <= maxBodyLength) {
            body
        } else {
            body.substring(0, maxBodyLength) + "...(truncated)"
        }
    }
}
