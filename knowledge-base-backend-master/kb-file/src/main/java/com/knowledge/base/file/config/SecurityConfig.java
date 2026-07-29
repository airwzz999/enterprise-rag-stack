package com.knowledge.base.file.config;

import com.knowledge.base.common.config.CustomAccessDeniedHandler;
import com.knowledge.base.common.config.CustomAuthenticationEntryPoint;
import com.knowledge.base.file.filter.JwtAuthenticationFilter;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * File service security configuration
 *
 * <p>Authentication is verified by {@link JwtAuthenticationFilter} via a Feign call to
 * kb-user-auth. Previously this service trusted the gateway alone (anyRequest().permitAll(),
 * with /files/** explicitly marked "fully open"), so upload/download/delete endpoints
 * were reachable with zero authentication by anything that could reach the service
 * directly, and FileController derived the uploader identity from an unverified
 * X-User-Id header.</p>
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

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Health checks and monitoring: fully open
                        .requestMatchers("/actuator/**").permitAll()

                        // API docs: fully open (development environment)
                        .requestMatchers("/doc.html", "/swagger-resources/**",
                                "/v3/api-docs/**", "/webjars/**", "/swagger-ui/**").permitAll()

                        // All other requests require a valid JWT
                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }
}
