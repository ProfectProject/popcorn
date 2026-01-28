package com.popcorn.order.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.popcorn.order.annotation.PerformanceMonitoring;
import com.popcorn.order.dto.response.OrderDetailResponse;
import com.popcorn.order.dto.response.OrderSummaryResponse;
import com.popcorn.order.dto.payment.PaymentUrlResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    
    private static final String CACHE_KEY_PREFIX = "order:";
    private static final String ORDER_DETAIL_PREFIX = CACHE_KEY_PREFIX + "detail:";
    private static final String ORDER_LIST_PREFIX = CACHE_KEY_PREFIX + "list:user:";
    private static final String ORDER_STATS_PREFIX = CACHE_KEY_PREFIX + "stats:";
    private static final String MY_ORDERS_PREFIX = "my-orders::";
    private static final String PAYMENT_URL_PREFIX = CACHE_KEY_PREFIX + "payment-url:";

  
    @Cacheable(value = "orderDetail", key = "#orderId.toString()")
    @PerformanceMonitoring(threshold = 100, category = "cache")
    public OrderDetailResponse getOrderDetailFromCache(UUID orderId) {
        log.debug("주문 상세 캐시 조회 - 주문ID: {}", orderId);
       
        return null;  
    }


    @CachePut(value = "orderDetail", key = "#orderDetail.orderId.toString()")
    @PerformanceMonitoring(threshold = 200, category = "cache")
    public OrderDetailResponse putOrderDetailToCache(OrderDetailResponse orderDetail) {
        log.debug("주문 상세 캐시 저장 - 주문ID: {}, 주문번호: {}",
                orderDetail.getOrderId(), orderDetail.getOrderNo());
        return orderDetail;
    }


    @CacheEvict(value = "orderDetail", key = "#orderId.toString()")
    @PerformanceMonitoring(threshold = 50, category = "cache")
    public void evictOrderDetailFromCache(UUID orderId) {
        log.debug("주문 상세 캐시 제거 - 주문ID: {}", orderId);
    }

    
    @Cacheable(value = "orderList",
               key = "#userId + ':' + #page + ':' + #size + ':' + #sort")
    @PerformanceMonitoring(threshold = 300, category = "cache")
    public List<OrderSummaryResponse> getUserOrderListFromCache(Long userId, int page, int size, String sort) {
        log.debug("사용자 주문 목록 캐시 조회 - 사용자ID: {}, 페이지: {}/{}", userId, page, size);
        return null;  
    }

   
    @PerformanceMonitoring(threshold = 100, category = "cache")
    public void evictUserOrderListCache(Long userId) {
        String pattern = ORDER_LIST_PREFIX + userId + ":*";

        try {
          
            var keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("사용자 주문 목록 캐시 제거 - 사용자ID: {}, 제거된 키 개수: {}", userId, keys.size());
            }
        } catch (Exception e) {
            log.error("사용자 주문 목록 캐시 제거 실패 - 사용자ID: {}, 오류: {}", userId, e.getMessage());
        }
    }

    
    @PerformanceMonitoring(threshold = 100, category = "cache")
    public void storePaymentUrl(UUID orderId, PaymentUrlResponse paymentUrlResponse) {
        if (orderId == null || paymentUrlResponse == null) {
            return;
        }
        String key = PAYMENT_URL_PREFIX + orderId;
        long ttlSeconds = 1800; // 기본 30분
        if (paymentUrlResponse.getExpiresAt() != null) {
            long seconds = java.time.Duration.between(
                    java.time.LocalDateTime.now(), paymentUrlResponse.getExpiresAt()).getSeconds();
            ttlSeconds = Math.max(seconds, 60);
        }
        redisTemplate.opsForValue().set(key, paymentUrlResponse, ttlSeconds, TimeUnit.SECONDS);
    }

    @PerformanceMonitoring(threshold = 50, category = "cache")
    public PaymentUrlResponse getPaymentUrl(UUID orderId) {
        if (orderId == null) {
            return null;
        }
        String key = PAYMENT_URL_PREFIX + orderId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof PaymentUrlResponse response) {
            return response;
        }
        return null;
    }

 
    @PerformanceMonitoring(threshold = 100, category = "cache")
    public void evictMyOrdersCache(Long userId) {
        String pattern = MY_ORDERS_PREFIX + userId + ":*";

        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.debug("내 주문 타임라인 캐시 제거 - 사용자ID: {}, 제거된 키 개수: {}", userId, keys.size());
            }
        } catch (Exception e) {
            log.error("내 주문 타임라인 캐시 제거 실패 - 사용자ID: {}, 오류: {}", userId, e.getMessage());
        }
    }

 
    @Cacheable(value = "orderStatistics", key = "#statsType + ':' + #period")
    @PerformanceMonitoring(threshold = 1000, category = "cache")
    public Map<String, Object> getOrderStatisticsFromCache(String statsType, String period) {
        log.debug("주문 통계 캐시 조회 - 타입: {}, 기간: {}", statsType, period);
        return null;  // 실제로는 복잡한 통계 계산 로직
    }

  
    @PerformanceMonitoring(threshold = 5000, category = "cache")
    public void warmUpCache() {
        log.info("캐시 워밍업 시작");

        try {
            // 최근 24시간 내 주문이 많은 사용자들의 데이터를 미리 캐시
            List<Long> activeUsers = getActiveUserIds();  // 구현 필요

            for (Long userId : activeUsers) {
                // 첫 페이지 데이터를 미리 캐시
                getUserOrderListFromCache(userId, 0, 10, "createdAt");
            }

            // 자주 조회되는 통계 데이터 미리 캐시
            getOrderStatisticsFromCache("daily", "today");
            getOrderStatisticsFromCache("hourly", "today");

            log.info("캐시 워밍업 완료 - 대상 사용자: {}명", activeUsers.size());

        } catch (Exception e) {
            log.error("캐시 워밍업 실패", e);
        }
    }

  
    @PerformanceMonitoring(threshold = 500, category = "cache")
    public Map<String, Object> getCacheStatistics() {
        try {
            // Redis INFO 명령어로 통계 정보 수집
            var info = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .info("stats");

            // 캐시별 키 개수 계산
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("orderDetailCacheSize", countCacheKeys(ORDER_DETAIL_PREFIX + "*"));
            stats.put("orderListCacheSize", countCacheKeys(ORDER_LIST_PREFIX + "*"));
            stats.put("orderStatsCacheSize", countCacheKeys(ORDER_STATS_PREFIX + "*"));
            stats.put("redisInfo", info);
            stats.put("timestamp", System.currentTimeMillis());

            log.debug("캐시 통계 수집 완료 - 상세:{}, 목록:{}, 통계:{}",
                    stats.get("orderDetailCacheSize"),
                    stats.get("orderListCacheSize"),
                    stats.get("orderStatsCacheSize"));

            return stats;

        } catch (Exception e) {
            log.error("캐시 통계 수집 실패", e);
            return Map.of("error", e.getMessage());
        }
    }

  
    @CacheEvict(value = {"orderDetail", "orderList", "orderStatistics"}, allEntries = true)
    @PerformanceMonitoring(threshold = 1000, category = "cache")
    public void clearAllOrderCache() {
        log.warn("모든 주문 캐시 클리어 실행");

        // 패턴 기반 캐시 클리어 (Spring Cache가 처리하지 못하는 부분)
        clearCacheByPattern(CACHE_KEY_PREFIX + "*");
    }

   
    @PerformanceMonitoring(threshold = 2000, category = "cache")
    public int cleanExpiredCache() {
        int cleanedCount = 0;

        try {
            // TTL이 0이거나 음수인 키들을 찾아서 제거
            var allKeys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");

            if (allKeys != null) {
                for (String key : allKeys) {
                    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    if (ttl != null && ttl <= 0) {
                        redisTemplate.delete(key);
                        cleanedCount++;
                    }
                }
            }

            log.info("만료된 캐시 정리 완료 - 정리된 키: {}개", cleanedCount);

        } catch (Exception e) {
            log.error("만료된 캐시 정리 실패", e);
        }

        return cleanedCount;
    }

  
    private List<Long> getActiveUserIds() {
        // TODO: 실제 구현 - 최근 활성 사용자 조회 로직
        return List.of(1L, 2L, 3L, 4L, 5L);
    }

    private long countCacheKeys(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("캐시 키 개수 계산 실패 - 패턴: {}", pattern, e);
            return 0;
        }
    }

    private void clearCacheByPattern(String pattern) {
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("패턴 기반 캐시 클리어 완료 - 패턴: {}, 삭제된 키: {}개", pattern, keys.size());
            }
        } catch (Exception e) {
            log.error("패턴 기반 캐시 클리어 실패 - 패턴: {}", pattern, e);
        }
    }

}
