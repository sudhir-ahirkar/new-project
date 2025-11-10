package com.toll.verify.api;


import com.toll.common.model.BlacklistEntry;
import com.toll.common.model.TagInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoDataController {

    private final RedisTemplate<String, TagInfo> tagRedisTemplate;
    private final RedisTemplate<String, BlacklistEntry> blacklistRedisTemplate;

    @PostMapping("/tag")
    public String createOrUpdateTag(@RequestBody TagInfo tag,
                                    @RequestParam(defaultValue = "300") long ttlSeconds) {

        String key = "TAG:" + tag.getTagId();
        tagRedisTemplate.opsForValue().set(key, tag, ttlSeconds, TimeUnit.SECONDS);
        return "✅ TAG stored: " + key + " (TTL " + ttlSeconds + " sec)";
    }

    @PostMapping("/balance/{tagId}")
    public String setBalance(@PathVariable String tagId, @RequestParam double balance) {
        String key = "TAG:" + tagId;
        TagInfo t = tagRedisTemplate.opsForValue().get(key);
        if (t == null) return "❌ Tag not found";

        t.setBalance(balance);
        tagRedisTemplate.opsForValue().set(key, t);
        return "✅ Balance updated → " + balance;
    }

    @PostMapping("/blacklist/{tagId}")
    public String blacklist(@PathVariable String tagId, @RequestParam(defaultValue = "PAYMENT_FAILED") String reason) {

        BlacklistEntry entry = BlacklistEntry.builder()
                .tagId(tagId)
                .reason(reason)
                .timestamp(Instant.now())
                .build();

        blacklistRedisTemplate.opsForValue().set("BLACKLIST:" + tagId, entry, 24, TimeUnit.HOURS);
        return "🚫 Tag BLACKLISTED: " + tagId + " (" + reason + ")";
    }

    @DeleteMapping("/blacklist/{tagId}")
    public String unBlacklist(@PathVariable String tagId) {
        blacklistRedisTemplate.delete("BLACKLIST:" + tagId);
        return "✅ Tag UN-BLACKLISTED: " + tagId;
    }

    @DeleteMapping("/reset")
    public String resetAll() {
        // for demo simplicity, flush all Redis
        tagRedisTemplate.getConnectionFactory().getConnection().flushAll();
        return "🧹 Redis cleared — clean demo state";
    }
}
