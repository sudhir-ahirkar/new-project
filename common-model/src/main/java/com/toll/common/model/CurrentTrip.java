package com.toll.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentTrip implements Serializable {
    private String eventId; // Unique event identifier for idempotency
    private String plazaId;
    private String laneId;
    private String timestamp;
    private Double tollAmount;
    private String status; // PENDING / SUCCESS / FAILED
}
