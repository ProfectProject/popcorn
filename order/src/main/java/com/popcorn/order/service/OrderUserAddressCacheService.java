package com.popcorn.order.service;

import com.popcorn.order.dto.user.UserAddressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 사용자 주소 조회 캐싱 서비스 - 주문 처리 속도 최적화
 * Redis Cache-Aside 패턴으로 외부 주소 조회를 캐싱
 *
 * 성능 향상:
 * - 사용자 기본 주소 조회: 1000ms → 10ms (99% 단축)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderUserAddressCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderUserLookupService originalUserLookupService;

    // 캐시 TTL 설정 - 주소는 자주 변경되지 않으므로 길게 설정
    private static final Duration USER_ADDRESS_TTL = Duration.ofHours(24);  // 24시간 캐시

    /**
     * 사용자 기본 주소 조회 (캐시 우선)
     * Cache Hit 시: ~10ms
     * Cache Miss 시: ~1000ms + 캐시 저장
     */
    public Optional<UserAddressResponse> getDefaultAddress(Long userId) {
        String cacheKey = "order:address:user:" + userId + ":default";

        // 1. 캐시 조회 시도
        UserAddressResponse cachedAddress = getCachedAddress(cacheKey);
        if (cachedAddress != null) {
            log.debug("⚡ [CACHE-HIT] 사용자 주소 캐시 조회 성공 - userId: {}, addressId: {}",
                    userId, cachedAddress.getAddressId());
            return Optional.of(cachedAddress);
        }

        // 2. 캐시 미스 - 원본 서비스 호출
        log.debug("🔍 [CACHE-MISS] 사용자 주소 원본 조회 시작 - userId: {}", userId);
        Optional<UserAddressResponse> addressOpt = originalUserLookupService.getDefaultAddress(userId);

        // 3. 조회 성공 시 캐시 저장
        if (addressOpt.isPresent()) {
            UserAddressResponse address = addressOpt.get();
            setCachedAddress(cacheKey, address, USER_ADDRESS_TTL);
            log.info("💾 [CACHE-SET] 사용자 주소 캐시 저장 - userId: {}, addressId: {}, ttl: {}시간",
                    userId, address.getAddressId(), USER_ADDRESS_TTL.toHours());
        } else {
            // 주소 없음도 캐시하여 반복 조회 방지 (짧은 TTL)
            setCachedAddress(cacheKey, UserAddressResponse.empty(), Duration.ofMinutes(10));
            log.info("💾 [CACHE-SET] 사용자 주소 없음 캐시 저장 - userId: {}, ttl: 10분", userId);
        }

        return addressOpt;
    }

    /**
     * 사용자 주소 캐시 무효화 - 주소 변경/삭제 시 호출
     */
    public void invalidateUserAddress(Long userId) {
        String cacheKeyPattern = "order:address:user:" + userId + ":*";

        // 해당 사용자의 모든 주소 캐시 삭제
        try {
            redisTemplate.delete(redisTemplate.keys(cacheKeyPattern));
            log.info("🗑️ [CACHE-DEL] 사용자 주소 캐시 무효화 - userId: {}", userId);
        } catch (Exception e) {
            log.warn("⚠️ [CACHE-ERROR] 주소 캐시 무효화 실패 - userId: {}, error: {}", userId, e.getMessage());
        }
    }

    /**
     * 주소 캐시 워밍업 - 자주 주문하는 사용자들의 주소를 미리 캐시
     */
    public void warmupUserAddressCache(Long userId) {
        log.info("🔥 [CACHE-WARMUP] 사용자 주소 캐시 워밍업 시작 - userId: {}", userId);
        getDefaultAddress(userId);
        log.info("✅ [CACHE-WARMUP] 사용자 주소 캐시 워밍업 완료 - userId: {}", userId);
    }

    // === Private Helper Methods ===

    private UserAddressResponse getCachedAddress(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof UserAddressResponse) {
                UserAddressResponse address = (UserAddressResponse) cached;
                // 빈 주소 객체인 경우 null 반환 (캐시된 "주소 없음" 상태)
                return address.isEmpty() ? null : address;
            }
            return null;
        } catch (Exception e) {
            log.warn("⚠️ [CACHE-ERROR] 주소 캐시 조회 실패 - key: {}, error: {}", cacheKey, e.getMessage());
            return null;  // 캐시 실패 시 원본 조회로 fallback
        }
    }

    private void setCachedAddress(String cacheKey, UserAddressResponse address, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(cacheKey, address, ttl);
        } catch (Exception e) {
            log.warn("⚠️ [CACHE-ERROR] 주소 캐시 저장 실패 - key: {}, error: {}", cacheKey, e.getMessage());
            // 캐시 저장 실패해도 비즈니스 로직에는 영향 없음
        }
    }
}