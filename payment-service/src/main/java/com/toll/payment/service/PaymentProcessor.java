package com.toll.payment.service;


import com.toll.common.model.ChargeStatus;
import com.toll.common.model.TagChargeRequest;
import com.toll.common.model.TagChargeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

/**
 * Consumes charge requests and publishes charge responses.
 * In real life: call issuer/bank/NPCI here with retries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessor {

    private final KafkaTemplate<String, TagChargeResponse> responseTemplate;

    @Value("${payment.topics.response}")
    private String responseTopic;

    @Value("${payment.simulate-failure-percent:10}")
    private int failurePercent;

    private final Random rnd = new Random();

    @KafkaListener(
            topics = "${payment.topics.request}",
            containerFactory = "chargeRequestListenerFactory"
    )
    public void onRequest(TagChargeRequest req) {
        log.info("💳 Received charge request eventId={} tagId={} amount={}",
                req.getEventId(), req.getTagId(), req.getAmount());

        boolean fail = rnd.nextInt(100) < failurePercent;

        TagChargeResponse resp = TagChargeResponse.builder()
                .eventId(req.getEventId())
                .tagId(req.getTagId())
                .status(fail ? ChargeStatus.FAILED : ChargeStatus.SUCCESS)
                .approvalCode(fail ? null : "APP-" + UUID.randomUUID())
                .failureReason(fail ? "Simulated gateway failure" : null)
                .build();

        responseTemplate.send(responseTopic, req.getTagId(), resp);
        log.info("Published charge response for eventId={} status={}", resp.getEventId(), resp.getStatus());
    }
}
