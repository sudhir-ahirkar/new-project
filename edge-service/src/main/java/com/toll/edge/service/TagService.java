package com.toll.edge.service;

import com.toll.common.model.BlacklistEntry;
import com.toll.common.model.CurrentTrip;
import com.toll.common.model.TagInfo;
import com.toll.edge.exception.ManualInterventionRequiredException;
import com.toll.edge.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {
    private final RedisTemplate<String, TagInfo> tagRedisTemplate;
    private final RedisTemplate<String, BlacklistEntry> blacklistRedisTemplate;
    private final Queue<TagInfo> batchQueue = new ConcurrentLinkedQueue<>();
    // 👇 inject TTL (minutes) from application.yml
    @Value("${cache.ttl-minutes:5}")
    private long cacheTtlMinutes;

    public void simulateRead(TagReadRequest req) {

        // Blacklist check FIRST
        String blacklistKey = "BLACKLIST:" + req.getTagId();
        BlacklistEntry blocked = blacklistRedisTemplate.opsForValue().get(blacklistKey);

        if (blocked != null) {
            log.warn("BLOCKED — Tag {} denied. Reason={}", req.getTagId(), blocked.getReason());

            throw new ManualInterventionRequiredException(
                    req.getTagId(),
                    blocked.getReason(),
                    "Please direct vehicle to manual lane for fee collection."
            );
        }

        // ✅ TRY TO FETCH EXISTING TAG FROM REDIS
        String redisKey = "TAG:" + req.getTagId();
        TagInfo existing = tagRedisTemplate.opsForValue().get(redisKey);

        double balance = existing != null ? existing.getBalance() : 500.0; // Keep existing balance

        CurrentTrip trip = CurrentTrip.builder()
                .plazaId(req.getPlazaId())
                .laneId(req.getLaneId())
                .timestamp(Instant.now().toString())
                .status("PENDING")
                .build();

        TagInfo tag = TagInfo.builder()
                .tagId(req.getTagId())
                .vehicleNumber(req.getVehicleNumber())
                .vehicleType(req.getVehicleType())
                .balance(balance) // Correct balance retained
                .currentTrip(trip)
                .build();

        tagRedisTemplate.opsForValue().set(redisKey, tag, cacheTtlMinutes, TimeUnit.MINUTES);

        batchQueue.add(tag);

        log.info("📡 Simulated read for {} (balance={}, status={}) → sent to ingest",
                tag.getTagId(), balance, trip.getStatus());
    }

    public List<TagInfo> drainBatch(int maxBatch) {
        List<TagInfo> out = new ArrayList<>();
        for (int i = 0; i < maxBatch; i++) {
            TagInfo t = batchQueue.poll();
            if (t == null) break;
            out.add(t);
        }
        return out;
    }
}
