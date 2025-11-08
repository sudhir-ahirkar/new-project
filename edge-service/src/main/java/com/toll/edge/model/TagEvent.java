package com.toll.edge.model;

import lombok.*;
import java.io.Serializable;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TagEvent implements Serializable {
    private String eventId;
    private String tagId;
    private String plazaId;
    private String laneId;
    private String timestamp;
}

