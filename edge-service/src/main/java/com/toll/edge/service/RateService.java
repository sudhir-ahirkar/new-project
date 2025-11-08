package com.toll.edge.service;

import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class RateService {
    private final Map<String, Double> rates = Map.of(
            "PLZ1:L1:LIGHT", 30.0,
            "PLZ1:L1:HEAVY", 60.0,
            "PLZ6:L9:LIGHT", 50.0,
            "PLZ6:L9:HEAVY", 90.0
    );

    public double getToll(String plazaId, String laneId, String vehicleType) {
        return rates.getOrDefault(plazaId + ":" + laneId + ":" + vehicleType, 0.0);
    }
}
