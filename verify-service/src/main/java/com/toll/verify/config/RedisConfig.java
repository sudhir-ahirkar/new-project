package com.toll.verify.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toll.common.model.BlacklistEntry;
import com.toll.common.model.TagInfo;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

/*@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, TagInfo> tagRedisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, TagInfo> t = new RedisTemplate<>();
        t.setConnectionFactory(cf);
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        Jackson2JsonRedisSerializer<TagInfo> ser = new Jackson2JsonRedisSerializer<>(TagInfo.class);
        ser.setObjectMapper(om);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(ser);
        t.afterPropertiesSet();
        return t;
    }
}*/

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, TagInfo> tagRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, TagInfo> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // ✅ New constructor: directly pass ObjectMapper
        Jackson2JsonRedisSerializer<TagInfo> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, TagInfo.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
    @Bean
    public RedisTemplate<String, BlacklistEntry> blacklistRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, BlacklistEntry> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<BlacklistEntry> serializer =
                new Jackson2JsonRedisSerializer<>(mapper, BlacklistEntry.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}

