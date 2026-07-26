package com.knowledge.base.document.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Static resource configuration
 *
 * <p>Configures the file access path</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:/data/knowledge-base/uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configure the file access path
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
