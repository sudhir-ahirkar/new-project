// gate-service/src/main/java/com/toll/gate/service/GateActuatorService.java
package com.toll.gate.service;

import com.toll.common.model.Decision;
import com.toll.common.model.OpenGateCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service orchestrating idempotency + strategy.
 * Open/deny only once per eventId.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GateActuatorService {

    private final BarrierStrategy barrierStrategy;
    private final GateStateStore stateStore;

    public void handle(OpenGateCommand cmd) {
        if (cmd == null || cmd.getEventId() == null) {
            log.warn("Ignoring null/invalid command: {}", cmd);
            return;
        }
        if (stateStore.isProcessed(cmd.getEventId())) {
            log.info("Duplicate command ignored (idempotent) eventId={}", cmd.getEventId());
            return;
        }

        if (cmd.getDecision() == Decision.OPEN) {
            barrierStrategy.open(cmd);
        } else {
            barrierStrategy.deny(cmd);
        }

        stateStore.apply(cmd);
        stateStore.markProcessed(cmd.getEventId());
    }
}

