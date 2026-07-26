package com.knowledge.base.document.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * File server configuration
 *
 * <p>Configures rustfs file server related information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "file.server.rustfs")
public class FileServerConfig {

    /**
     * File server address
     */
    private String baseUrl = "http://localhost:8080";

    /**
     * Upload endpoint path
     */
    private String uploadPath = "/api/upload";

    /**
     * Access path prefix
     */
    private String accessPrefix = "/files";

    /**
     * Auth token (if required)
     */
    private String authToken;

    /**
     * Connect timeout (milliseconds)
     */
    private Integer connectTimeout = 5000;

    /**
     * Read timeout (milliseconds)
     */
    private Integer readTimeout = 30000;

    /**
     * Maximum file size (bytes)
     */
    private Long maxFileSize = 20971520L; // 20MB
}
