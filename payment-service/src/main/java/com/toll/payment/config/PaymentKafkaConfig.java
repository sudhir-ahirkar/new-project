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
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@EnableKafka
@Configuration
public class PaymentKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrap;

    // ✅ Consumer: TagChargeRequest
    @Bean
    public ConsumerFactory<String, TagChargeRequest> chargeRequestConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap,
                ConsumerConfig.GROUP_ID_CONFIG, "payment-service-group",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                "spring.deserializer.value.delegate.class", JsonDeserializer.class,
                "spring.json.trusted.packages", "*",
                "spring.json.value.default.type", TagChargeRequest.class.getName()
        ));
    }

    @Bean(name = "requestKafkaListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, TagChargeRequest> chargeRequestListenerFactory(
            ConsumerFactory<String, TagChargeRequest> factory) {

        ConcurrentKafkaListenerContainerFactory<String, TagChargeRequest> listener =
                new ConcurrentKafkaListenerContainerFactory<>();
        listener.setConsumerFactory(factory);
        return listener;
    }

    // ✅ Producer: TagChargeResponse
    @Bean
    public ProducerFactory<String, TagChargeResponse> chargeResponseProducerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        ));
    }

    @Bean(name = "chargeResponseKafkaTemplate")
    public KafkaTemplate<String, TagChargeResponse> chargeResponseKafkaTemplate() {
        return new KafkaTemplate<>(chargeResponseProducerFactory());
    }
}
