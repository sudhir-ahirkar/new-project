package com.toll.gate.service;

import com.toll.common.model.Decision;
import com.toll.common.model.OpenGateCommand;
import com.toll.gate.model.GateState;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory read model and idempotency store.
 * - Tracks processed eventIds to avoid duplicate actuation.
 * - Tracks last state per (plazaId:laneId) for querying.
 */
@Component
public class GateStateStore {

    private final Map<String, GateState> laneState = new ConcurrentHashMap<>();
    private final Map<String, Boolean> processed = new ConcurrentHashMap<>();

    public boolean isProcessed(String eventId) {
        return processed.containsKey(eventId);
    }

    public void markProcessed(String eventId) {
        processed.put(eventId, true);
    }

    public void apply(OpenGateCommand cmd) {
        String key = key(cmd.getPlazaId(), cmd.getLaneId());
        GateState current = laneState.getOrDefault(key,
                GateState.builder()
                        .plazaId(cmd.getPlazaId())
                        .laneId(cmd.getLaneId())
                        .totalDenies(0)
                        .totalOpens(0)
                        .build());

        current.setLastEventId(cmd.getEventId());
        current.setLastDecision(cmd.getDecision());
        current.setLastReason(cmd.getReason());
        current.setLastUpdatedAt(Instant.now());

        if (cmd.getDecision() == Decision.OPEN) {
            current.setTotalOpens(current.getTotalOpens() + 1);
        } else {
            current.setTotalDenies(current.getTotalDenies() + 1);
        }

        laneState.put(key, current);
    }

    public Optional<GateState> get(String plazaId, String laneId) {
        return Optional.ofNullable(laneState.get(key(plazaId, laneId)));
    }

    private static String key(String plazaId, String laneId) {
        return plazaId + ":" + laneId;
    }
}

