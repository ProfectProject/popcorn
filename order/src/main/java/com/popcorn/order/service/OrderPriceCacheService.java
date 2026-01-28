package com.popcorn.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPriceCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderPriceLookupService originalPriceLookupService;

  
    private static final Duration SESSION_PRICE_TTL = Duration.ofDays(1);    // 세션 가격: 1일 캐시
    private static final Duration GOODS_PRICE_TTL = Duration.ofMinutes(30);  // 굿즈 가격: 30분 캐시


    public Integer getSessionPrice(UUID sessionId) {
        String cacheKey = "order:price:session:" + sessionId;

        // 1. 캐시 조회 시도
        Integer cachedPrice = getCachedPrice(cacheKey);
        if (cachedPrice != null) {
            log.debug("⚡ [CACHE-HIT] 세션 가격 캐시 조회 성공 - sessionId: {}, price: {}원", sessionId, cachedPrice);
            return cachedPrice;
        }

        // 2. 캐시 미스 - 원본 서비스 호출
        log.debug("🔍 [CACHE-MISS] 세션 가격 원본 조회 시작 - sessionId: {}", sessionId);
        Integer price = originalPriceLookupService.requestSessionPrice(sessionId);

        // 3. 조회 성공 시 캐시 저장
        if (price != null) {
            setCachedPrice(cacheKey, price, SESSION_PRICE_TTL);
            log.info("💾 [CACHE-SET] 세션 가격 캐시 저장 - sessionId: {}, price: {}원, ttl: {}일",
                    sessionId, price, SESSION_PRICE_TTL.toDays());
        }

        return price;
    }

 
    public Integer getGoodsPrice(UUID goodsId) {
        String cacheKey = "order:price:goods:" + goodsId;

        // 1. 캐시 조회 시도
        Integer cachedPrice = getCachedPrice(cacheKey);
        if (cachedPrice != null) {
            log.debug("⚡ [CACHE-HIT] 굿즈 가격 캐시 조회 성공 - goodsId: {}, price: {}원", goodsId, cachedPrice);
            return cachedPrice;
        }

        // 2. 캐시 미스 - 원본 서비스 호출
        log.debug("🔍 [CACHE-MISS] 굿즈 가격 원본 조회 시작 - goodsId: {}", goodsId);
        Integer price = originalPriceLookupService.requestGoodsPrice(goodsId);

        // 3. 조회 성공 시 캐시 저장
        if (price != null) {
            setCachedPrice(cacheKey, price, GOODS_PRICE_TTL);
            log.info("💾 [CACHE-SET] 굿즈 가격 캐시 저장 - goodsId: {}, price: {}원, ttl: {}분",
                    goodsId, price, GOODS_PRICE_TTL.toMinutes());
        }

        return price;
    }

  
    public CompletableFuture<PriceResult> getSessionAndGoodsPrice(UUID sessionId, UUID goodsId) {
        CompletableFuture<Integer> sessionPriceFuture = CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            Integer price = getSessionPrice(sessionId);
            log.debug("⏱️ 세션 가격 조회 완료 - {}ms", System.currentTimeMillis() - startTime);
            return price;
        });

        CompletableFuture<Integer> goodsPriceFuture = CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            Integer price = getGoodsPrice(goodsId);
            log.debug("⏱️ 굿즈 가격 조회 완료 - {}ms", System.currentTimeMillis() - startTime);
            return price;
        });

        return CompletableFuture.allOf(sessionPriceFuture, goodsPriceFuture)
                .thenApply(v -> new PriceResult(sessionPriceFuture.join(), goodsPriceFuture.join()));
    }

   
    public void invalidateSessionPrice(UUID sessionId) {
        String cacheKey = "order:price:session:" + sessionId;
        redisTemplate.delete(cacheKey);
        log.info("🗑️ [CACHE-DEL] 세션 가격 캐시 무효화 - sessionId: {}", sessionId);
    }

    public void invalidateGoodsPrice(UUID goodsId) {
        String cacheKey = "order:price:goods:" + goodsId;
        redisTemplate.delete(cacheKey);
        log.info("🗑️ [CACHE-DEL] 굿즈 가격 캐시 무효화 - goodsId: {}", goodsId);
    }

  
    public void warmupCache(UUID sessionId, UUID goodsId) {
        CompletableFuture.runAsync(() -> {
            log.info("🔥 [CACHE-WARMUP] 가격 캐시 워밍업 시작 - session: {}, goods: {}", sessionId, goodsId);
            getSessionPrice(sessionId);
            getGoodsPrice(goodsId);
            log.info("✅ [CACHE-WARMUP] 가격 캐시 워밍업 완료");
        });
    }

  

    private Integer getCachedPrice(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            return cached != null ? (Integer) cached : null;
        } catch (Exception e) {
            log.warn("⚠️ [CACHE-ERROR] 캐시 조회 실패 - key: {}, error: {}", cacheKey, e.getMessage());
            return null;  // 캐시 실패 시 원본 조회로 fallback
        }
    }

    private void setCachedPrice(String cacheKey, Integer price, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(cacheKey, price, ttl);
        } catch (Exception e) {
            log.warn("⚠️ [CACHE-ERROR] 캐시 저장 실패 - key: {}, error: {}", cacheKey, e.getMessage());
            // 캐시 저장 실패해도 비즈니스 로직에는 영향 없음
        }
    }

   
    public static class PriceResult {
        private final Integer sessionPrice;
        private final Integer goodsPrice;

        public PriceResult(Integer sessionPrice, Integer goodsPrice) {
            this.sessionPrice = sessionPrice;
            this.goodsPrice = goodsPrice;
        }

        public Integer getSessionPrice() { return sessionPrice; }
        public Integer getGoodsPrice() { return goodsPrice; }
        public Integer getTotalPrice() {
            return (sessionPrice != null ? sessionPrice : 0) +
                   (goodsPrice != null ? goodsPrice : 0);
        }
    }
}
