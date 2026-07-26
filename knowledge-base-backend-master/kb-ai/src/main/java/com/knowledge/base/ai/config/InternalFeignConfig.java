package com.knowledge.base.ai.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * Internal service Feign call configuration
 *
 * <p>Adds an X-User-Id header for internal kb-ai → kb-document calls,
 * bypassing JwtAuthenticationFilter's token validation.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public class InternalFeignConfig {

    @Bean
    public RequestInterceptor internalAuthInterceptor() {
        return template -> template.header("X-User-Id", "0");
    }
}
