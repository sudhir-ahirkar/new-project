package com.toll.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Immutable message asking the gate controller to open/deny a barrier for a given lane.
 * Published by verify-service only on payment SUCCESS.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenGateCommand {
    private String eventId;     // unique per transaction (idempotency key)
    private String tagId;
    private String plazaId;
    private String laneId;
    private Decision decision;  // "OPEN" or "DENY"
    private String reason;      // optional (e.g., INSUFFICIENT_FUNDS)
    private Instant timestamp;  // when command created upstream
}

