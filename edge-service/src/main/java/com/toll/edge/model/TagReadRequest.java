package com.toll.edge.model;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class TagReadRequest {
    private String tagId;
    private String vehicleNumber;
    private String vehicleType;
    private String plazaId;
    private String laneId;
}
