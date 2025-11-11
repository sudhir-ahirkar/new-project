package com.toll.verify.config;

import com.toll.common.model.TagChargeResponse;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.Map;

@EnableKafka
@Configuration
public class VerifyKafkaDLTConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrap;

    // Consumer for charge responses
    @Bean
    public ConsumerFactory<String, TagChargeResponse> chargeResponseConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap,
                ConsumerConfig.GROUP_ID_CONFIG, "verify-service-response-group",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                "spring.deserializer.value.delegate.class", JsonDeserializer.class,
                "spring.json.trusted.packages", "*",
                "spring.json.value.default.type", TagChargeResponse.class.getName()
        ));
    }

    // Producer for publishing failed messages
    @Bean
    public ProducerFactory<Object, Object> dltProducerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
        ));
    }

    @Bean
    public KafkaTemplate<Object, Object> dltKafkaTemplate() {
        return new KafkaTemplate<>(dltProducerFactory());
    }

    // Route failed messages to <original-topic>.DLT
    @Bean
    public DeadLetterPublishingRecoverer recoverer() {
        return new DeadLetterPublishingRecoverer(
                dltKafkaTemplate(),
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition())
        );
    }

    // Retry + Backoff Policy
    @Bean
    public DefaultErrorHandler verifyErrorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff(1000, 2.0); // 1s → 2s → 4s → 8s
        backOff.setMaxInterval(8000);

        return new DefaultErrorHandler(recoverer(), backOff);
    }

    // Listener container factory that applies retry + DLT
    @Bean(name = "chargeResponseListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, TagChargeResponse> chargeResponseListenerFactory(
            ConsumerFactory<String, TagChargeResponse> consumerFactory,
            DefaultErrorHandler verifyErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, TagChargeResponse> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(verifyErrorHandler);
        return factory;
    }
}
