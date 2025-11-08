package com.toll.ingest.controller;

import com.toll.common.model.TagInfo;
import com.toll.ingest.service.IngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingest")
@RequiredArgsConstructor
public class IngestController {

    private final IngestService ingestService;

    @PostMapping("/tags")
    public ResponseEntity<String> ingestTags(@RequestBody List<TagInfo> tags) {
        ingestService.publishTags(tags);
        return ResponseEntity.accepted().body("Published " + tags.size() + " tag(s) to Kafka");
    }
}
