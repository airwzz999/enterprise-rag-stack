package com.knowledge.base.graph.config;

import com.knowledge.base.common.config.CustomAccessDeniedHandler;
import com.knowledge.base.common.config.CustomAuthenticationEntryPoint;
import com.knowledge.base.graph.filter.JwtAuthenticationFilter;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Graph service security configuration
 *
 * <p>Authentication is verified by {@link JwtAuthenticationFilter} via a Feign call to
 * kb-user-auth. Previously this service trusted the gateway alone (anyRequest().permitAll()
 * with no filter), so destructive endpoints like document graph deletion and cache
 * eviction were reachable with zero authentication by anything that could reach the
 * service directly.</p>
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(AbstractHttpConfigurer::disable)

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/doc.html", "/swagger-resources/**",
                                "/v3/api-docs/**", "/webjars/**", "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated()
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }
}
