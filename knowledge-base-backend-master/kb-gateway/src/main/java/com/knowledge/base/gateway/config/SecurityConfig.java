package com.knowledge.base.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway security configuration class
 *
 * <p>Configures Spring Security to allow CORS preflight requests through</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Configure the security filter chain
     *
     * @param http ServerHttpSecurity
     * @return SecurityWebFilterChain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        // Disable CSRF
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        // Disable the default CORS handling (we use our own custom CorsWebFilter)
        http.cors(ServerHttpSecurity.CorsSpec::disable);

        // Allow all requests through (authentication is handled by AuthFilter)
        http.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());

        return http.build();
    }
}
