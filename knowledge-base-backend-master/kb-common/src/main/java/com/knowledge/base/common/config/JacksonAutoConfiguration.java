package com.knowledge.base.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.text.SimpleDateFormat;

/**
 * Jackson auto-configuration
 *
 * <p>Ensures Jackson correctly serializes and deserializes objects, and resolves the JavaScript large-integer precision loss issue</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Serializes Long values as strings: resolves the issue where JavaScript cannot safely represent integers larger than 2^53-1</li>
 *   <li>Supports Java 8 date/time types: LocalDateTime, LocalDate, etc.</li>
 *   <li>Ignores unknown properties: avoids deserialization errors caused by unrecognized fields</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class JacksonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();

        // Add Java 8 date/time module support
        builder.modules(new JavaTimeModule());

        // Set the date format
        builder.dateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        // Configure serialization features
        builder.featuresToDisable(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
        );

        // Configure a custom serializer
        builder.serializerByType(Long.class, ToStringSerializer.instance);

        return builder;
    }
}
