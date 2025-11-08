package com.toll.gate.service;

import com.toll.common.model.OpenGateCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Simulation: just logs the action.
 * In a real deployment, call the lane controller here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimulatedBarrierStrategy implements BarrierStrategy {

    @Value("${gate.openDurationMs:1500}")
    private long openDurationMs;

    @Override
    public void open(OpenGateCommand cmd) {
        log.info("OPEN signal for plaza={} lane={} tagId={} eventId={} (pulse={}ms)",
                cmd.getPlazaId(), cmd.getLaneId(), cmd.getTagId(), cmd.getEventId(), openDurationMs);
        // simulate relay pulse duration
        try {
            Thread.sleep(Math.min(openDurationMs, 2000));
        } catch (InterruptedException ignored) {
        }
        log.info("Barrier considered OPENED (simulated) for eventId={}", cmd.getEventId());
    }

    @Override
    public void deny(OpenGateCommand cmd) {
        log.warn("DENY for plaza={} lane={} tagId={} eventId={} reason={}",
                cmd.getPlazaId(), cmd.getLaneId(), cmd.getTagId(), cmd.getEventId(), cmd.getReason());
    }
}

