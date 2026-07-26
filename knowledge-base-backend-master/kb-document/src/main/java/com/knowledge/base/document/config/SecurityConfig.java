package com.knowledge.base.document.config;

import com.knowledge.base.common.config.CustomAccessDeniedHandler;
import com.knowledge.base.common.config.CustomAuthenticationEntryPoint;
import com.knowledge.base.document.filter.JwtAuthenticationFilter;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * Document service Security configuration
 *
 * <p>Configuration notes:</p>
 * <ul>
 *   <li>CSRF disabled: JWT tokens do not require CSRF protection</li>
 *   <li>Stateless sessions: no Session is used, relying entirely on JWT tokens</li>
 *   <li>CORS disabled: handled centrally by the gateway, disabled in business services</li>
 * </ul>
 *
 * <p>Endpoint access control:</p>
 * <ul>
 *   <li>Category endpoints: publicly accessible, no authentication required</li>
 *   <li>Document endpoints: authentication required</li>
 *   <li>OPTIONS requests: fully open</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Resource
    private CustomAuthenticationEntryPoint authEntryPoint;

    @Resource
    private CustomAccessDeniedHandler accessDeniedHandler;

    /**
     * Configures the security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF protection (not needed with JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // Configure stateless sessions (no Session used)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Disable CORS (handled centrally by the gateway)
                .cors(AbstractHttpConfigurer::disable)

                // Add the JWT authentication filter
                .addFilterBefore(jwtAuthenticationFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public share access: allow anonymous viewing of share links
                        .requestMatchers("/share/**").permitAll()

                        // API documentation: fully open (development environment)
                        .requestMatchers("/doc.html", "/swagger-resources/**",
                                "/v3/api-docs/**", "/webjars/**").permitAll()

                        // Health checks and monitoring: fully open
                        .requestMatchers("/actuator/**").permitAll()

                        // Test endpoints: fully open
                        .requestMatchers("/test", "/test-error").permitAll()

                        // OPTIONS preflight requests: fully open
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        // All other requests: authentication required, fine-grained permissions controlled by method annotations
                        .anyRequest().authenticated()
                )

                // Configure exception handling: return JSON-formatted 401/403 responses
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }
}
