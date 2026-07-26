package com.knowledge.base.file.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File storage configuration class
 *
 * <p>Configures the local file storage service</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class FileStorageConfig {

    @Value("${file.upload.path:/tmp/knowledge-base/uploads}")
    private String uploadPath;

    /**
     * Create the upload directory on initialization
     */
    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(uploadPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created file upload directory: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to create file upload directory: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to create file upload directory", e);
        }
    }
}