package com.toll.verify.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.Map;

@Configuration
@EnableKafka
public class VerifyKafkaDLTConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrap;

    @Value("${payment.topics.response}")
    private String paymentResponseTopic;

    // Producer for .DLT messages
    @Bean
    public ProducerFactory<Object, Object> verifyDLTProducerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        ));
    }

    @Bean
    public KafkaTemplate<Object, Object> verifyDLTKafkaTemplate() {
        return new KafkaTemplate<>(verifyDLTProducerFactory());
    }

    @Bean
    public DeadLetterPublishingRecoverer verifyRecoverer(KafkaTemplate<Object, Object> verifyDLTKafkaTemplate) {
        return new DeadLetterPublishingRecoverer(
                verifyDLTKafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition())
        );
    }

    @Bean
    public DefaultErrorHandler verifyErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(1000);  // 1s
        backOff.setMultiplier(2.0);        // 1s → 2s → 4s → 8s
        backOff.setMaxInterval(8000);
        return new DefaultErrorHandler(recoverer, backOff);
    }

}
