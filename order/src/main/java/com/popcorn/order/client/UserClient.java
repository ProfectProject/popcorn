package com.popcorn.order.client;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.popcorn.order.dto.user.UserAddressResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * User 마이크로서비스와 통신하는 HTTP Client
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${microservices.user.base-url}")
    private String userBaseUrl;

    @Value("${microservices.user.timeout:30s}")
    private Duration userTimeout;

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserAddressesFallback")
    @Retry(name = "userService")
    @RateLimiter(name = "userService")
    public List<UserAddressResponse> getUserAddresses(Long userId) {
        log.debug("User 주소 목록 조회 - userId: {}", userId);

        return webClientBuilder.build()
            .get()
            .uri(userBaseUrl + "/api/users/v1/users/{userId}/addresses", userId)
            .retrieve()
            .bodyToFlux(UserAddressResponse.class)
            .collectList()
            .block(userTimeout);
    }

    public List<UserAddressResponse> getUserAddressesFallback(Long userId, Exception ex) {
        log.warn("User 주소 조회 실패 - userId: {}, 이유: {}", userId, ex.getMessage());
        return List.of();
    }

    public Optional<UserAddressResponse> getDefaultAddress(Long userId) {
        List<UserAddressResponse> addresses = getUserAddresses(userId);
        if (addresses == null || addresses.isEmpty()) {
            return Optional.empty();
        }

        return addresses.stream()
            .filter(address -> Boolean.TRUE.equals(address.getIsDefault()))
            .findFirst();
    }
}
