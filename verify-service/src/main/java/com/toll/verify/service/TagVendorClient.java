package com.toll.verify.service;

import com.toll.common.model.CurrentTrip;
import com.toll.common.model.TagInfo;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TagVendorClient {
    // Mock: In real implementation call actual vendor API
    public TagInfo fetchTag(String tagId) {
        // simulated
        CurrentTrip trip = CurrentTrip.builder()
                .plazaId(null).laneId(null).timestamp(Instant.now().toString())
                .tollAmount(0.0).status("IDLE").build();
        return TagInfo.builder()
                .tagId(tagId).vehicleNumber("MH01X0001")
                .vehicleType("LIGHT").balance(500.0)
                .currentTrip(trip).build();
    }
}



/*
import com.toll.common.model.TagInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagVendorClient {

    private final RestTemplate restTemplate;

    @Value("${vendor.service-url}")
    private String vendorUrl;

    */
/**
     * Fetch tag info from vendor service.
     * Uses a circuit breaker to protect against downstream failures.
     *//*

    @CircuitBreaker(name = "vendorService", fallbackMethod = "fallbackTag")
    public TagInfo fetchTag(String tagId) {
        try {
            TagInfo response = restTemplate.getForObject(vendorUrl + tagId, TagInfo.class);
            log.info("Fetched tag {} from vendor successfully", tagId);
            return response;
        } catch (Exception e) {
            log.error("Vendor fetch failed for tag {}: {}", tagId, e.getMessage());
            throw e;
        }
    }

    */
/**
     * Fallback when vendor service is down or breaker is open.
     *//*

    public TagInfo fallbackTag(String tagId, Throwable throwable) {
        log.warn("Vendor unavailable for tag {}, fallback invoked: {}", tagId, throwable.getMessage());
        // Return a minimal TagInfo so processing can continue safely
        TagInfo fallback = new TagInfo();
        fallback.setTagId(tagId);
        fallback.setBalance(0.0);
        return fallback;
    }
}
*/


