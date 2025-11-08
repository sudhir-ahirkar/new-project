package com.toll.gate.model;

import com.toll.common.model.Decision;
import lombok.*;

import java.time.Instant;

/**
 * Read model for queries / dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GateState {
    private String plazaId;
    private String laneId;
    private String lastEventId;
    private Decision lastDecision;
    private String lastReason;
    private Instant lastUpdatedAt;
    private long totalOpens;
    private long totalDenies;
}

