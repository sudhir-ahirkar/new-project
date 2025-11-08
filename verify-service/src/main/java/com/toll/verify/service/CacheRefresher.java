package com.toll.verify.service;

import com.toll.common.model.TagInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheRefresher {

    private final RedisTemplate<String, TagInfo> redisTemplate;
    private final TagVendorClient vendorClient;

    /**
     * Scheduled job to refresh active tag cache every 5 minutes.
     * It scans Redis for keys with prefix "TAG:" and refreshes each entry from vendor.
     */
   // @Scheduled(fixedDelayString = "${cache.refresh-interval-ms:300000}") // default: 5 minutes
    public void refreshActiveTags() {
        Set<String> keys = redisTemplate.keys("TAG:*");
        if (keys == null || keys.isEmpty()) {
            return;
        }

        log.info("Refreshing {} cached tags from vendor...", keys.size());
        for (String key : keys) {
            try {
                String tagId = key.replace("TAG:", "");
                TagInfo updated = vendorClient.fetchTag(tagId);
                if (updated != null) {
                    redisTemplate.opsForValue().set(key, updated);
                    log.debug("Refreshed tag {} cache from vendor", tagId);
                }
            } catch (Exception e) {
                log.warn("Failed to refresh tag cache for key {}: {}", key, e.getMessage());
            }
        }
        log.info("Cache refresh cycle complete.");
    }
}

