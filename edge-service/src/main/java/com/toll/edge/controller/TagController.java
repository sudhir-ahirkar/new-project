package com.toll.edge.controller;

import com.toll.common.model.TagInfo;
import com.toll.edge.util.Constant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/reader")
@RequiredArgsConstructor
public class TagController {
    private final RedisTemplate<String, TagInfo> tagRedisTemplate;

    @PostMapping("/tag/update-balance")
    public ResponseEntity<?> updateBalance(@RequestParam String tagId, @RequestParam double balance) {
        String key = "TAG:" + tagId;
        TagInfo tagInfo = tagRedisTemplate.opsForValue().get(key);

        if (tagInfo == null) {
            return ResponseEntity.badRequest().body("Tag not found in Redis: " + tagId);
        }

        tagInfo.setBalance(balance);
        tagRedisTemplate.opsForValue().set(key, tagInfo, Constant.TIME_OUT, TimeUnit.HOURS);
        return ResponseEntity.ok("Balance updated for " + tagId + " → " + balance);
    }
}
