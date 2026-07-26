package com.knowledge.base.document.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j configuration class
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, configures the API documentation</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class Knife4jConfig {

    /**
     * Configures OpenAPI
     *
     * @return OpenAPI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Knowledge Base System - Document Service API Documentation")
                .version("1.0.0")
                .description("Provides document management, document search, document categorization, and other features")
                .contact(new Contact()
                    .name("airwzz999")
                    .email("support@knowledge-base.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
