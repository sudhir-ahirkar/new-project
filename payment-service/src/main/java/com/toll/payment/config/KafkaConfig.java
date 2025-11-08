package com.toll.payment.config;


import com.toll.common.model.TagChargeRequest;
import com.toll.common.model.TagChargeResponse;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrap;

    // ---- Producer (responses) ----
    @Bean
    public ProducerFactory<String, TagChargeResponse> responseProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.JsonSerializer.class);
        // do NOT add type headers
        props.put("spring.json.add.type.headers", false);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, TagChargeResponse> responseKafkaTemplate() {
        return new KafkaTemplate<>(responseProducerFactory());
    }

    // ---- Consumer (requests) ----
    @Bean
    public ConsumerFactory<String, TagChargeRequest> requestConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.JsonDeserializer.class);
        // REQUIRED — overrides missing config
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-service-group");
        // accept all; map to our local class when no type headers
        props.put("spring.json.trusted.packages", "*");
        props.put("spring.json.value.default.type", "com.toll.common.model.TagChargeRequest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TagChargeRequest> requestKafkaListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TagChargeRequest> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(requestConsumerFactory());
        // Optional: if you want to log errors without stopping the consumer
        factory.setCommonErrorHandler(new DefaultErrorHandler());
        return factory;
    }
}
