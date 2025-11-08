package com.toll.gate.kafka;

import com.toll.gate.service.GateActuatorService;
import com.toll.common.model.OpenGateCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes OpenGateCommand from Kafka and forwards to the actuator.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GateCommandConsumer {

    private final GateActuatorService actuator;

    @Value("${gate.command-topic}")
    private String topic;

    @KafkaListener(
            topics = "${gate.command-topic}",
            containerFactory = "gateCommandListenerFactory"
    )
    public void onMessage(@Payload OpenGateCommand cmd) {
        log.info("⬇️  Received OpenGateCommand: {}", cmd);
        actuator.handle(cmd);
    }
}
