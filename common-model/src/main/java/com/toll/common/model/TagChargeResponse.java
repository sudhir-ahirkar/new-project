package com.toll.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Produced by payment-service after attempting the charge. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagChargeResponse implements Serializable {
    private String eventId; // Needed for idempotency and logs
    private String tagId;
    private ChargeStatus status; // // SUCCESS / INSUFFICIENT_FUNDS / ERROR
    private String approvalCode;  // if success
    private String failureReason; // if failed
}
