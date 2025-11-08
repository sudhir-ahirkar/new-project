package com.toll.verify.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toll.common.model.TagInfo;
import com.toll.verify.service.VerifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagEventConsumer {
    private final VerifyService verifyService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${verify.topic:toll.tag.event}", groupId = "verify-service-group")
    public void consume(TagInfo info) {
        try {
           // TagInfo info = objectMapper.readValue(payload, TagInfo.class);
            log.info("Received TagInfo from Kafka: {}", info.getTagId());
            verifyService.process(info);
        } catch (Exception e) {
            log.error("Failed to parse/handle event: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}

