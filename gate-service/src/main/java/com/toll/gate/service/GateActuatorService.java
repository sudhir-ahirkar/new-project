package com.toll.gate.service;

import com.toll.common.model.Decision;
import com.toll.common.model.OpenGateCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GateActuatorService {

    private final BarrierStrategy strategy;
    private final GateStateStore store;
    private final KafkaTemplate<String, OpenGateCommand> gateCommandKafkaTemplate;

    public void handle(OpenGateCommand cmd) {

        if (store.isProcessed(cmd.getEventId())) {
            log.info("⏸ Ignoring duplicate eventId={}", cmd.getEventId());
            return;
        }

        strategy.handle(cmd, () -> {
            // ⏳ AUTO CLOSE
            OpenGateCommand closeCmd = OpenGateCommand.builder()
                    .eventId(cmd.getEventId())
                    .tagId(cmd.getTagId())
                    .plazaId(cmd.getPlazaId())
                    .laneId(cmd.getLaneId())
                    .decision(Decision.CLOSE)
                    .reason("AUTO_CLOSE")
                    .timestamp(java.time.Instant.now())
                    .build();

            gateCommandKafkaTemplate.send(
                    "toll.gate.command",
                    cmd.getPlazaId() + ":" + cmd.getLaneId(),
                    closeCmd
            );

            log.info("⬇️ AUTO CLOSE published eventId={}", cmd.getEventId());
        });

        store.apply(cmd);
        store.markProcessed(cmd.getEventId());
    }
}
