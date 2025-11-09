package com.toll.gate.service;

import com.toll.common.model.Decision;
import com.toll.common.model.OpenGateCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Simulates real-world gate hardware behavior.
 * - When OPEN received → waits (barrier rising time)
 * - Then calls callback to publish CLOSE automatically.
 */
@Slf4j
@Service   // ✅ Now Spring picks it up as a bean
public class SimulatedBarrierStrategy implements BarrierStrategy {

    private static final long OPEN_DURATION_MS = 3000;

    @Override
    public void handle(OpenGateCommand cmd, Runnable onCloseCallback) {

        if (cmd.getDecision() == Decision.OPEN) {
            log.info("OPEN signal → plaza={} lane={} tag={} eventId={} (will auto-close in {}ms)",
                    cmd.getPlazaId(), cmd.getLaneId(), cmd.getTagId(), cmd.getEventId(), OPEN_DURATION_MS);

            try { Thread.sleep(OPEN_DURATION_MS); }
            catch (InterruptedException ignored) {}

            log.info("✅ Barrier fully OPENED (simulated) eventId={}", cmd.getEventId());

            onCloseCallback.run(); // 🔥 triggers auto close

        } else {
            log.warn("🚫 Gate DENIED for tag={} eventId={} reason={}",
                    cmd.getTagId(), cmd.getEventId(), cmd.getReason());
        }
    }
}
