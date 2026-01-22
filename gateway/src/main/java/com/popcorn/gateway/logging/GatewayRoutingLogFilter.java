package com.popcorn.gateway.logging;

import java.net.URI;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class GatewayRoutingLogFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayRoutingLogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String method = Optional.ofNullable(exchange.getRequest().getMethod())
                .map(HttpMethod::name)
                .orElse("UNKNOWN");
        String incomingPath = exchange.getRequest().getURI().getPath();

        return chain.filter(exchange)
                .doOnError(throwable -> logOutcome(exchange, method, incomingPath, throwable))
                .doOnSuccess(unused -> logOutcome(exchange, method, incomingPath, null));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private void logOutcome(ServerWebExchange exchange, String method, String incomingPath, Throwable throwable) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "unknown";
        URI targetUri = route != null ? route.getUri() : null;

        Integer statusCode = Optional.ofNullable(exchange.getResponse().getStatusCode())
                .map(status -> status.value())
                .orElse(null);

        if (throwable != null) {
            log.warn("[gateway] {} {} -> route={} target={} status={} : {}",
                    method,
                    incomingPath,
                    routeId,
                    targetUri,
                    statusCode,
                    throwable.getMessage()
            );
        } else {
            log.info("[gateway] {} {} -> route={} target={} status={}",
                    method,
                    incomingPath,
                    routeId,
                    targetUri,
                    statusCode != null ? statusCode : "UNKNOWN"
            );
        }
    }
}
