package com.toll.gate.controller;

import com.toll.gate.model.GateState;
import com.toll.gate.service.GateStateStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Read-only endpoints to check gate state (useful for dashboards/tests).
 */
@RestController
@RequestMapping("/gate")
@RequiredArgsConstructor
public class GateQueryController {

    private final GateStateStore store;

    @GetMapping("/state/{plazaId}/{laneId}")
    public ResponseEntity<GateState> getState(@PathVariable String plazaId, @PathVariable String laneId) {
        Optional<GateState> gs = store.get(plazaId, laneId);
        return gs.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
