package com.knowledge.base.document.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * Internal service Feign call configuration
 *
 * <p>Adds an X-User-Id header for internal kb-document to kb-graph/kb-file calls,
 * bypassing JwtAuthenticationFilter's token validation. Mirrors kb-ai's
 * InternalFeignConfig, needed now that kb-graph and kb-file require authentication
 * instead of trusting the gateway alone.</p>
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
