package com.popcorn.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import com.popcorn.gateway.http.EventHttpLoggingFilter;

@RestController
public class OpenApiProxyController {

    private final WebClient webClient;

    public OpenApiProxyController(EventHttpLoggingFilter eventHttpLoggingFilter) {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8081")  // 모놀리식 백엔드
                .filter(eventHttpLoggingFilter)
                .build();
    }

    @GetMapping("/openapi/customer")
    public Mono<String> customerApi() {
        return proxy("/v3/api-docs/customer");
    }

    @GetMapping("/openapi/manager")
    public Mono<String> managerApi() {
        return proxy("/v3/api-docs/manager");
    }

    @GetMapping("/openapi/admin")
    public Mono<String> adminApi() {
        return proxy("/v3/api-docs/admin");
    }

    private Mono<String> proxy(String target) {
        return webClient.get()
                .uri(target)
                .retrieve()
                .bodyToMono(String.class);
    }
}
