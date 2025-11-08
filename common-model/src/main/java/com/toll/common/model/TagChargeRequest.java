package com.toll.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Message produced by verify-service to request a debit. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagChargeRequest implements Serializable {
    private String eventId; // unique transaction reference
    private String tagId;
    private String vehicleNumber;
    private String vehicleType;
    private Double amount;       // toll to charge
    private String plazaId;
    private String laneId;
    private String timestampIso; // original event timestamp (optional)
}
