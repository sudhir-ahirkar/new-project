package com.toll.edge.service;

import com.toll.common.model.BlacklistEntry;
import com.toll.common.model.CurrentTrip;
import com.toll.common.model.TagInfo;
import com.toll.edge.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {
    private final RedisTemplate<String, TagInfo> tagRedisTemplate;
    private final RedisTemplate<String, BlacklistEntry> blacklistRedisTemplate;
    private final RateService rateService;
    private final Queue<TagInfo> batchQueue = new ConcurrentLinkedQueue<>();

    public void simulateRead(TagReadRequest req) {

        // Blacklist check FIRST
        String blacklistKey = "BLACKLIST:" + req.getTagId();
        BlacklistEntry blocked = blacklistRedisTemplate.opsForValue().get(blacklistKey);

        if (blocked != null) {
            log.warn("BLOCKED — Tag {} denied. Reason={}", req.getTagId(), blocked.getReason());
            throw new IllegalStateException("Tag is blacklisted: " + blocked.getReason());
        }

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
                .balance(500.0)
                .currentTrip(trip)
                .build();

        String key = "TAG:" + tag.getTagId();
        tagRedisTemplate.opsForValue().set(key, tag);

        batchQueue.add(tag);
        log.info("Simulated read and cached {} ; queued for ingest", tag.getTagId());
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
