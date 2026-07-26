package com.knowledge.base.file.storage;

import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.file.config.FileStorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * File storage factory
 *
 * <p>Dynamically selects the file storage implementation based on configuration</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorageFactory {

    private final ApplicationContext applicationContext;
    private final FileStorageProperties storageProperties;
    
    private FileStorage currentStorage;

    @PostConstruct
    public void init() {
        StorageType storageType = StorageType.fromCode(storageProperties.getType());
        log.info("Initializing file storage: type={}", storageType);

        try {
            currentStorage = applicationContext.getBean(storageType.getBeanName(), FileStorage.class);
            log.info("File storage initialized successfully: type={}, implementation={}",
                    storageType, currentStorage.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Failed to initialize file storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize file storage", e);
        }
    }

    /**
     * Get the current storage implementation
     *
     * @return file storage implementation
     */
    public FileStorage getStorage() {
        if (currentStorage == null) {
            throw new BusinessException("File storage service is not initialized");
        }
        return currentStorage;
    }

    /**
     * Get the current storage type
     *
     * @return storage type
     */
    public String getStorageType() {
        return storageProperties.getType();
    }
}