package com.knowledge.base.userauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * WebMvc configuration
 *
 * <p>Configures HTTP message converters and response handling</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    public WebMvcConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // Allow automatic conversion of string-to-numeric types (String -> Long/Integer, etc.)
        // Fixes JavaScript precision loss when the frontend sends large numbers such as snowflake IDs
        objectMapper.coercionConfigFor(LogicalType.Integer)
                .setCoercion(CoercionInputShape.String, CoercionAction.TryConvert)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // String message converter, set to UTF-8 encoding
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        converters.add(stringConverter);

        // Jackson JSON converter, set to UTF-8 encoding
        // Uses the pre-configured ObjectMapper (already includes JavaTimeModule support)
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setDefaultCharset(StandardCharsets.UTF_8);
        jacksonConverter.setObjectMapper(objectMapper);

        converters.add(jacksonConverter);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    }
}
