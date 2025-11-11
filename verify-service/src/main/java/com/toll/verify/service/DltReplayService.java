package com.toll.verify.service;

import com.toll.common.model.TagChargeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DltReplayService {

    @Qualifier("dltConsumerFactory")
    private final ConsumerFactory<String, TagChargeRequest> dltConsumerFactory;

    private final KafkaTemplate<String, TagChargeRequest> chargeRequestKafkaTemplate;

    /**
     * Replays all messages from DLT topic back into the main topic.
     */
    public int replayDLT(String dltTopic, String mainTopic) {
        var consumer = dltConsumerFactory.createConsumer();
        consumer.subscribe(List.of(dltTopic));

        int replayed = 0;

        log.warn("Starting DLT replay from {} → {}", dltTopic, mainTopic);

        var records = consumer.poll(Duration.ofSeconds(3));

        for (var record : records) {
            log.warn("Replaying DLT message key={} value={}", record.key(), record.value());
            chargeRequestKafkaTemplate.send(mainTopic, record.key(), record.value());
            replayed++;
        }

        consumer.close();
        log.warn("DLT replay complete — {} messages resent from {} → {}", replayed, dltTopic, mainTopic);

        return replayed;
    }
}
