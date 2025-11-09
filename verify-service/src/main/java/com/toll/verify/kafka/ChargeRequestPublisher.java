package com.toll.verify.kafka;

import com.toll.common.model.TagChargeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChargeRequestPublisher {

    @Value("${payment.topics.request}")
    private String topic;

    @Qualifier("chargeRequestKafkaTemplate")
    private final KafkaTemplate<String, TagChargeRequest> chargeRequestKafkaTemplate;

    public void publish(TagChargeRequest req) {
        chargeRequestKafkaTemplate.send(topic, req.getEventId(), req);
        log.info("💳 Published charge request eventId={} tagId={} amount={}",
                req.getEventId(), req.getTagId(), req.getAmount());
    }
}
