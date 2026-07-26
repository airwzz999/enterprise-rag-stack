package com.knowledge.base.file.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * File service security configuration
 *
 * <p>Configuration notes:</p>
 * <ul>
 *   <li>CSRF disabled: file uploads do not need CSRF protection</li>
 *   <li>Stateless sessions: authentication is fully delegated to the gateway</li>
 *   <li>All endpoints permitted: authentication is handled centrally by the gateway</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Configure the security filter chain
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception on configuration error
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF protection (not needed for file uploads)
                .csrf(AbstractHttpConfigurer::disable)

                // Configure stateless sessions
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Disable CORS (handled centrally by the gateway)
                .cors(AbstractHttpConfigurer::disable)

                // Permit all requests (authentication is handled centrally by the gateway)
                .authorizeHttpRequests(auth -> auth
                        // Health checks and monitoring: fully open
                        .requestMatchers("/actuator/**").permitAll()

                        // API docs: fully open (development environment)
                        .requestMatchers("/doc.html", "/swagger-resources/**",
                                "/v3/api-docs/**", "/webjars/**", "/swagger-ui/**").permitAll()

                        // File upload and download endpoints: fully open (access controlled by the gateway)
                        .requestMatchers("/files/**").permitAll()

                        // All other requests: permitted (authentication handled centrally by the gateway)
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
