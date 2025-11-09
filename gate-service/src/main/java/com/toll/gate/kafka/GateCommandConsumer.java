package com.toll.gate.kafka;

import com.toll.common.model.OpenGateCommand;
import com.toll.gate.service.GateActuatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes gate commands and delegates to GateActuatorService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GateCommandConsumer {

    private final GateActuatorService actuatorService;

    @KafkaListener(
            topics = "${gate.command-topic:toll.gate.command}",
            containerFactory = "gateCommandListenerFactory"
    )
    public void onCommand(OpenGateCommand cmd) {
        log.info("⬇️ Received OpenGateCommand: {}", cmd);
        actuatorService.handle(cmd);
    }
}
