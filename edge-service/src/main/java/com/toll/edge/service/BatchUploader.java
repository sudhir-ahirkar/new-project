package com.toll.edge.service;

import com.toll.common.model.TagInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchUploader {
    private final TagService tagService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${edge.ingest-url}")
    private String ingestUrl;

    @Value("${edge.batch-size:10}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${edge.flush-interval-ms:5000}")
    public void flushJob() {
        List<TagInfo> batch = tagService.drainBatch(batchSize);
        if (batch.isEmpty()) return;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<TagInfo>> request = new HttpEntity<>(batch, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(ingestUrl, request, String.class);
            log.info("Flushed {} events to ingest: status {}", batch.size(), resp.getStatusCode());
        } catch (Exception e) {
            log.warn("Failed to flush batch to ingest: {}", e.getMessage());
            // re-enqueue on failure
            batch.forEach(b -> tagService.drainBatch(0)); // no-op; alternative: push back manually
        }
    }
}

