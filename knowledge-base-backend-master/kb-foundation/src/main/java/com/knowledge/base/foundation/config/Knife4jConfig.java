package com.knowledge.base.foundation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j configuration
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise Knowledge Base System - Foundation Service API")
                        .version("1.0.0")
                        .description("Provides foundation features such as system configuration, notification push, operation logs, and dictionary management")
                        .contact(new Contact()
                                .name("airwzz999")
                                .email("support@knowledge-base.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
