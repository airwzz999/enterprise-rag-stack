package com.knowledge.base.gateway.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import reactor.core.publisher.Mono;

/**
 * Gateway configuration class
 *
 * <p>Configures the gateway's global filters; routes are configured in application.yml</p>
 * <p>CORS is managed centrally via spring.cloud.gateway.globalcors in application.yml,
 *    avoiding conflicts between CorsWebFilter and globalcors that could cause SockJS
 *    cross-origin issues</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class GatewayConfig {

    /**
     * Global authentication filter
     *
     * @return AuthFilter
     */
    @Bean
    public GlobalFilter authFilter() {
        return new AuthFilter();
    }

    /**
     * Authentication filter
     */
    public static class AuthFilter implements GlobalFilter, Ordered {

        @Override
        public Mono<Void> filter(
                org.springframework.web.server.ServerWebExchange exchange,
                org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
            // JWT validation logic could be added here
            // Currently all requests are passed through
            return chain.filter(exchange);
        }

        @Override
        public int getOrder() {
            return -100; // High priority
        }
    }
}
