package com.knowledge.base.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS configuration class
 *
 * <p>Designed following the Alibaba Java Development Guidelines, configures cross-origin access</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class CorsConfig {

    /**
     * Build the CorsFilter
     *
     * @return the CorsFilter
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Allow sending cookies
        config.setAllowCredentials(true);

        // Allow all origins (in production, configure specific domains instead)
        config.addAllowedOriginPattern("*");

        // Allow all request headers
        config.addAllowedHeader("*");

        // Allow all request methods
        config.addAllowedMethod("*");

        // Exposed response headers
        config.addExposedHeader("Content-Disposition");

        // Preflight request cache duration (seconds)
        config.setMaxAge(3600L);

        // Apply the CORS configuration to all paths
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
