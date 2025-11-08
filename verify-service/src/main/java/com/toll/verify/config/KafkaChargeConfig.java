package com.toll.verify.config;

import com.toll.common.model.OpenGateCommand;
import com.toll.common.model.TagChargeRequest;
import com.toll.common.model.TagChargeResponse;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaChargeConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrap;

    /* ---------------------------------------------------
     *  PRODUCER: TagChargeRequest  →  payment-service
     * --------------------------------------------------- */
    @Bean
    public ProducerFactory<String, TagChargeRequest> chargeRequestProducerFactory() {
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap,
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
                )
        );
    }

    @Bean
    public KafkaTemplate<String, TagChargeRequest> chargeRequestKafkaTemplate() {
        return new KafkaTemplate<>(chargeRequestProducerFactory());
    }

    /* ---------------------------------------------------
     *  CONSUMER: TagChargeResponse  ←  from payment-service
     * --------------------------------------------------- */
    @Bean
    public ConsumerFactory<String, TagChargeResponse> chargeResponseConsumerFactory() {

        JsonDeserializer<TagChargeResponse> deserializer =
                new JsonDeserializer<>(TagChargeResponse.class);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap,
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
                        ConsumerConfig.GROUP_ID_CONFIG, "verify-payment-group"
                ),
                new StringDeserializer(),
                deserializer
        );
    }


    @Bean(name = "responseKafkaListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, TagChargeResponse> responseKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TagChargeResponse> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(chargeResponseConsumerFactory());
        factory.setCommonErrorHandler(new DefaultErrorHandler()); // keeps consumer running on error
        return factory;
    }
}
