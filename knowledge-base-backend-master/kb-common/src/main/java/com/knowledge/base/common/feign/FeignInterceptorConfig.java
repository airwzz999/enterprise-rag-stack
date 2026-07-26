package com.knowledge.base.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * Feign request interceptor
 *
 * <p>Adds an INNER-REQUEST header to inter-service calls, marking them as internal calls</p>
 * <p>Referenced susan-mall-cloud's Feign configuration to implement special handling for inter-service calls</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignInterceptorConfig {

    public FeignInterceptorConfig() {
        // Configuration class, used for conditional bean registration
    }

    /**
     * Feign request interceptor bean
     */
    @org.springframework.context.annotation.Bean
    public RequestInterceptor feignInterceptor() {
        return new FeignInterceptor();
    }

    /**
     * Feign request interceptor implementation
     */
    public static class FeignInterceptor implements RequestInterceptor {

        private static final String INNER_REQUEST_HEADER = "INNER-REQUEST";

        @Override
        public void apply(RequestTemplate template) {
            // Add internal call marker
            template.header(INNER_REQUEST_HEADER, "true");
        }
    }
}
