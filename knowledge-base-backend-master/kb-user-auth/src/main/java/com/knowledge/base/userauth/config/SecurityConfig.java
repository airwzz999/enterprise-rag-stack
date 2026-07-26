package com.knowledge.base.userauth.config;

import com.knowledge.base.common.config.CustomAccessDeniedHandler;
import com.knowledge.base.common.config.CustomAuthenticationEntryPoint;
import com.knowledge.base.userauth.filter.JwtAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import jakarta.annotation.Resource;

/**
 * Spring Security configuration class
 *
 * <p>Configuration notes:</p>
 * <ul>
 *   <li>CSRF disabled: not needed since JWT tokens are used</li>
 *   <li>Stateless sessions: no session is used, authentication relies entirely on the JWT token</li>
 *   <li>JWT authentication: the JWT filter is added before the username/password authentication filter</li>
 *   <li>CORS configuration: handled centrally by the gateway; disabled in business services</li>
 * </ul>
 *
 * <p>Security policy:</p>
 * <ul>
 *   <li>Login and register endpoints: fully open</li>
 *   <li>API documentation endpoints: fully open (development environment)</li>
 *   <li>All other business endpoints: require JWT authentication</li>
 *   <li>OPTIONS requests: fully open (supports CORS preflight)</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
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
     * <p>Filter chain execution order:</p>
     * <ol>
     *   <li>JwtAuthenticationFilter: parses the token and sets the user context</li>
     *   <li>UsernamePasswordAuthenticationFilter: username/password authentication</li>
     *   <li>ExceptionTranslationFilter: exception handling</li>
     *   <li>FilterSecurityInterceptor: authorization check</li>
     * </ol>
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception configuration error
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("==================== Configuring Spring Security ====================");

        http
                // Disable CSRF protection (not needed with JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // Configure stateless session management (no session is used)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Disable CORS (handled centrally by the gateway)
                .cors(AbstractHttpConfigurer::disable)

                // Add the JWT authentication filter (before the username/password authentication filter)
                .addFilterBefore(jwtAuthenticationFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Login, register, email verification, password reset endpoints: fully open
                        .requestMatchers("/auth/login", "/auth/register", "/auth/verify-email",
                                "/auth/password/reset/**", "/auth/auth/**").permitAll()
                        .requestMatchers("/teams/tree").permitAll()

                        // Internal Feign calls: token validation, role-based user lookup, reviewer ID lookup
                        .requestMatchers("/auth/validate", "/auth/users/by-role", "/auth/reviewer-ids").permitAll()

                        // /me endpoint: requires authentication (but no specific role)
                        .requestMatchers("/auth/me").authenticated()

                        // Test endpoints: used to diagnose 403 issues
                        .requestMatchers("/auth/debug/**", "/auth/test/**").permitAll()

                        // Public API: fully open
                        .requestMatchers("/public/**").permitAll()

                        // WebSocket: fully open
                        .requestMatchers("/ws/**").permitAll()

                        // Health checks and monitoring: fully open
                        .requestMatchers("/actuator/**").permitAll()

                        // API documentation: fully open (development environment)
                        .requestMatchers("/doc.html", "/swagger-resources/**",
                                "/v3/api-docs/**", "/webjars/**").permitAll()

                        // Test endpoints: fully open
                        .requestMatchers("/test", "/test-error").permitAll()

                        // OPTIONS preflight requests: fully open (supports CORS)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // All other requests: require authentication
                        .anyRequest().authenticated()
                )

                // Configure exception handling: return JSON-formatted 401/403 responses
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        log.info("Spring Security configuration complete: JWT authentication mode, stateless sessions");
        log.info("==================== Spring Security configuration finished ====================");
        return http.build();
    }

    /**
     * Configure the password encoder
     *
     * <p>Uses the BCrypt algorithm with strength 10</p>
     * <p>BCrypt characteristics:</p>
     * <ul>
     *   <li>Automatic salting: each encryption produces a different result</li>
     *   <li>Adjustable strength: default 10, range 4-31</li>
     *   <li>One-way encryption: cannot be reversed</li>
     * </ul>
     *
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
