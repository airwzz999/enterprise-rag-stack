package com.knowledge.base.foundation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.foundation.entity.SystemConfig;

import java.util.List;

/**
 * System configuration Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface SystemConfigService {

    /**
     * Paginated query of system configs
     *
     * @param current  current page
     * @param size     page size
     * @param category category (optional)
     * @return paginated result
     */
    IPage<SystemConfig> pageConfigs(Long current, Long size, String category);

    /**
     * Get a config by config key
     *
     * @param key config key
     * @return config information
     */
    SystemConfig getConfigByKey(String key);

    /**
     * Create a system config
     *
     * @param config config information
     * @return whether it succeeded
     */
    Boolean createConfig(SystemConfig config);

    /**
     * Update a system config
     *
     * @param key    config key
     * @param config new config information
     * @return whether it succeeded
     */
    Boolean updateConfig(String key, SystemConfig config);

    /**
     * Delete a system config
     *
     * @param key config key
     * @return whether it succeeded
     */
    Boolean deleteConfig(String key);

    /**
     * Get the config list by category
     *
     * @param category category
     * @return config list
     */
    List<SystemConfig> getConfigsByCategory(String category);

    /**
     * Get the list of publicly accessible configs
     *
     * @return public config list
     */
    List<SystemConfig> getPublicConfigs();
}
