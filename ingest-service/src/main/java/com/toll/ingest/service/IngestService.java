package com.toll.ingest.service;

import com.toll.common.model.TagInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestService {

    private final KafkaTemplate<String, TagInfo> kafkaTemplate;
    private final RateService rateService;

    @Value("${ingest.topic:toll.tag.event}")
    private String topic;

    /**
     * Enrich TagInfo and publish to Kafka.
     * - Computes toll based on plaza + lane + vehicle type
     * - Generates eventId if missing
     * - Publishes one message per tag event
     */
    public void publishTags(List<TagInfo> tags) {

        for (TagInfo tag : tags) {

            if (tag.getCurrentTrip() == null) {
                log.error("Rejecting tag={} because currentTrip is null", tag.getTagId());
                continue; // skip bad message, do not crash entire batch
            }

            // Generate eventId if not present
            if (tag.getCurrentTrip().getEventId() == null) {
                tag.getCurrentTrip().setEventId(UUID.randomUUID().toString());
            }

            // Compute toll (central pricing logic)
            double toll = rateService.getToll(
                    tag.getCurrentTrip().getPlazaId(),
                    tag.getCurrentTrip().getLaneId(),
                    tag.getVehicleType()   //use vehicleType for realistic toll rules
            );
            tag.getCurrentTrip().setTollAmount(toll);

            // Publish to Kafka
            kafkaTemplate.send(topic, tag.getTagId(), tag)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Published tagId={} eventId={} toll={} to topic={}",
                                    tag.getTagId(), tag.getCurrentTrip().getEventId(), toll, topic);
                        } else {
                            log.error("Failed to publish tagId={} due to {}", tag.getTagId(), ex.getMessage());
                        }
                    });
        }
    }
}

