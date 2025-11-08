package com.toll.gate.config;

import com.toll.common.model.OpenGateCommand;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrap;

    @Bean
    public ConsumerFactory<String, OpenGateCommand> gateCommandConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "gate-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Use ErrorHandlingDeserializer delegating to JsonDeserializer
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.ErrorHandlingDeserializer.class);
        props.put("spring.deserializer.value.delegate.class",
                "org.springframework.kafka.support.serializer.JsonDeserializer");
        props.put("spring.json.trusted.packages", "*");
        props.put("spring.json.value.default.type", OpenGateCommand.class.getName());

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean(name = "gateCommandListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, OpenGateCommand> gateCommandListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OpenGateCommand> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(gateCommandConsumerFactory());
        factory.getContainerProperties().setObservationEnabled(true);
        factory.setConcurrency(1); // one lane controller per instance; scale horizontally if needed
        return factory;
    }
}
