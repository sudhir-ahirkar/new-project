package com.toll.verify.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "toll_transaction", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TollTransaction {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    private String tagId;
    private String vehicleNumber;
    private String vehicleType;
    private String plazaId;
    private String laneId;
    private Instant timestamp;
    private Double tollAmount;
    private Double previousBalance;
    private Double newBalance;
    private String status; // SUCCESS, INSUFFICIENT_FUNDS
    private Instant createdAt;
}

