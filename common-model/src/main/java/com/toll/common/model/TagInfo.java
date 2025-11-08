package com.toll.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data @NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagInfo implements Serializable {
    private String tagId;
    private String vehicleNumber;
    private String vehicleType;
    private Double balance;
    private CurrentTrip currentTrip;
}

