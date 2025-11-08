package com.toll.edge.controller;

import com.toll.edge.model.TagReadRequest;
import com.toll.edge.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reader")
@RequiredArgsConstructor
public class ReaderController {
    private final TagService tagService;

    @PostMapping("/simulate")
    public ResponseEntity<String> simulate(@RequestBody TagReadRequest req) {
        tagService.simulateRead(req);
        return ResponseEntity.accepted().body("ok");
    }
}
