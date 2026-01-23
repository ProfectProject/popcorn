package com.popcorn.gateway.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/user-service")
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback(ServerHttpRequest req) {
        Map<String, Object> body = Map.of(
                "code", 503,
                "message", "user-service temporarily unavailable",
                "path", req.getURI().getPath()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }

    //forward:/fallback/checkIn-service
    @RequestMapping("/fallback/checkIn-service")
    public Mono<ResponseEntity<Map<String, Object>>> checkInServiceFallback(ServerHttpRequest req) {
        Map<String, Object> body = Map.of(
                "code", 503,
                "message", "checkIn-service temporarily unavailable",
                "path", req.getURI().getPath()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
    //forward:/fallback/checkin-doc-service
    @RequestMapping("/fallback/checkin-doc-service")
    public Mono<ResponseEntity<Map<String, Object>>> checkInDocServiceFallback(ServerHttpRequest req) {
        Map<String, Object> body = Map.of(
                "code", 503,
                "message", "checkIn-doc-service temporarily unavailable",
                "path", req.getURI().getPath()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
    //forward:/fallback/store-service
    @RequestMapping("/fallback/store-service")
    public Mono<ResponseEntity<Map<String, Object>>> storeServiceFallback(ServerHttpRequest req) {
        Map<String, Object> body = Map.of(
                "code", 503,
                "message", "store-service temporarily unavailable",
                "path", req.getURI().getPath()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
    //forward:/fallback/order-service
    @RequestMapping("/fallback/order-service")
    public Mono<ResponseEntity<Map<String, Object>>> orderServiceFallback(ServerHttpRequest req) {
        Map<String, Object> body = Map.of(
                "code", 503,
                "message", "order-service temporarily unavailable",
                "path", req.getURI().getPath()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
    //forward:/fallback/payment-service
    @RequestMapping("/fallback/payment-service")
    public Mono<ResponseEntity<Map<String, Object>>> paymentServiceFallback(ServerHttpRequest req) {
        Map<String, Object> body = Map.of(
                "code", 503,
                "message", "payment-service temporarily unavailable",
                "path", req.getURI().getPath()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
    //forward:/fallback/orderQuery-service
    @RequestMapping("/fallback/orderQuery-service")
    public Mono<ResponseEntity<Map<String, Object>>> orderQueryServiceFallback(ServerHttpRequest req) {
        Map<String, Object> body = Map.of(
                "code", 503,
                "message", "orderQuery-service temporarily unavailable",
                "path", req.getURI().getPath()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
