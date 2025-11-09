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
