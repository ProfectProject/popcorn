package com.popcorn.store.domain.popup.cache;

import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PopupDetailCacheManager {

    private static final String POPUP_DETAIL_KEY_FORMAT = "popup:%s:detail";

    private final RedisTemplate<String, Object> redisTemplate;

    public String buildDetailKey(UUID popupId) {
        return String.format(POPUP_DETAIL_KEY_FORMAT, popupId);
    }

    public void evictDetail(UUID popupId) {
        if (popupId == null) {
            return;
        }
        // 팝업/스케줄/굿즈 변경 시 호출되어 상세 캐시를 삭제한다.
        redisTemplate.delete(buildDetailKey(popupId));
    }
}
