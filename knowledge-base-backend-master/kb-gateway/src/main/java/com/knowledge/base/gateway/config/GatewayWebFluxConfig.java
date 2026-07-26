package com.knowledge.base.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Gateway WebFlux configuration
 *
 * <p>Ensures the gateway correctly handles JSON response bodies</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class GatewayWebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        // Configure the message codecs to ensure response bodies are handled correctly
        configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024); // 16MB
    }
}
