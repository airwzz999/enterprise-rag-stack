package com.knowledge.base.foundation.config;

import com.knowledge.base.common.config.CustomAccessDeniedHandler;
import com.knowledge.base.common.config.CustomAuthenticationEntryPoint;
import com.knowledge.base.foundation.filter.JwtAuthenticationFilter;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Foundation service Security configuration
 *
 * <p>Configuration notes:</p>
 * <ul>
 *   <li>CSRF disabled: not needed when using JWT tokens</li>
 *   <li>Stateless sessions: no Session is used, relying entirely on the JWT token</li>
 *   <li>CORS disabled: handled centrally by the gateway; business services disable CORS</li>
 *   <li>WebSocket endpoint: allowed through at the HTTP layer; authentication is handled by the STOMP-layer WebSocketAuthInterceptor</li>
 * </ul>
 *
 * <p>WebSocket authentication mechanism:</p>
 * <ol>
 *   <li>The client connects to {@code /ws/notification} via SockJS</li>
 *   <li>The STOMP CONNECT frame carries {@code Authorization: Bearer <token>}</li>
 *   <li>{@link WebSocketAuthInterceptor} validates the JWT at the STOMP layer and sets the Principal</li>
 *   <li>The server performs point-to-point delivery via {@code convertAndSendToUser(userId, ...)}</li>
 *   <li>Anonymous connections (no token) can only receive broadcast messages (/topic/**)</li>
 * </ol>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Resource
    private CustomAuthenticationEntryPoint authEntryPoint;

    @Resource
    private CustomAccessDeniedHandler accessDeniedHandler;

    /**
     * Configure the security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF protection (not needed when using JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // Configure stateless sessions (no Session used)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Disable CORS (handled centrally by the gateway)
                .cors(AbstractHttpConfigurer::disable)

                // Add the JWT authentication filter
                .addFilterBefore(jwtAuthenticationFilter,
                        org.springframework.security.web.authentication
                                .UsernamePasswordAuthenticationFilter.class)

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // ===== WebSocket endpoint: fully allowed at the HTTP layer =====
                        // WebSocket authentication is handled by the STOMP-layer WebSocketAuthInterceptor
                        // SockJS HTTP handshake requests (/ws/notification/info, etc.) must also be allowed
                        .requestMatchers("/ws/**").permitAll()

                        // ===== API docs and monitoring: fully open =====
                        .requestMatchers("/doc.html", "/swagger-resources/**",
                                "/v3/api-docs/**", "/webjars/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // ===== OPTIONS preflight requests: fully open =====
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ===== Notification endpoints: authentication required =====
                        .requestMatchers("/notifications/**").authenticated()

                        // ===== Public config endpoint: no authentication required (needed by the login/register pages) =====
                        .requestMatchers("/config/public").permitAll()

                        // ===== All other requests: authentication required =====
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
