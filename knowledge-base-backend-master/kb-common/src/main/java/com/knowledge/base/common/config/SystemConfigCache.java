package com.knowledge.base.common.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * System configuration Redis cache
 *
 * <p>All system configuration (kb_system_config) is stored centrally in a Redis Hash;
 * each microservice reads it from the cache instead of connecting directly to a shared database.</p>
 *
 * <ul>
 *   <li>Redis Key: {@code kb:system:config}</li>
 *   <li>Hash Field: config_key</li>
 *   <li>Hash Value: config_value</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.1.0
 */
@Slf4j
@Component
public class SystemConfigCache {

    /** Redis Hash key name */
    public static final String REDIS_HASH_KEY = "kb:system:config";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Get a single configuration value
     *
     * @param configKey the config key
     * @return the config value, or null if it does not exist
     */
    public String getConfig(String configKey) {
        Object value = stringRedisTemplate.opsForHash().get(REDIS_HASH_KEY, configKey);
        return value != null ? value.toString() : null;
    }

    /**
     * Get a single configuration value (with a default value)
     *
     * @param configKey    the config key
     * @param defaultValue the default value
     * @return the config value, or the default value if it does not exist
     */
    public String getConfig(String configKey, String defaultValue) {
        String value = getConfig(configKey);
        return value != null ? value : defaultValue;
    }

    /**
     * Get all configuration entries (key-value pairs)
     *
     * @return a Map of all configuration entries
     */
    public Map<String, String> getAllConfigs() {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(REDIS_HASH_KEY);
        Map<String, String> result = new HashMap<>();
        entries.forEach((k, v) -> result.put(k.toString(), v.toString()));
        return result;
    }

    /**
     * Write a single configuration entry to the cache
     *
     * @param configKey   the config key
     * @param configValue the config value
     */
    public void setConfig(String configKey, String configValue) {
        stringRedisTemplate.opsForHash().put(REDIS_HASH_KEY, configKey, configValue);
        log.debug("Configuration written to cache: {} = {}", configKey, configValue);
    }

    /**
     * Delete a single configuration entry from the cache
     *
     * @param configKey the config key
     */
    public void deleteConfig(String configKey) {
        stringRedisTemplate.opsForHash().delete(REDIS_HASH_KEY, configKey);
        log.debug("Configuration removed from cache: {}", configKey);
    }

    /**
     * Batch-load configuration into the cache (called by kb-foundation on startup or refresh)
     *
     * @param configs the configuration key-value pairs
     */
    public void loadAll(Map<String, String> configs) {
        if (configs == null || configs.isEmpty()) {
            log.warn("Batch configuration load is empty, skipping");
            return;
        }
        // Clear the old cache before writing the new batch
        stringRedisTemplate.delete(REDIS_HASH_KEY);
        stringRedisTemplate.opsForHash().putAll(REDIS_HASH_KEY, configs);
        log.info("Batch-loaded {} configuration entries into the Redis cache", configs.size());
    }

    /**
     * Refresh a single configuration entry (only updates if it already exists in the cache, to prevent concurrent write overwrites)
     *
     * @param configKey   the config key
     * @param configValue the config value
     */
    public void refreshConfig(String configKey, String configValue) {
        Boolean exists = stringRedisTemplate.opsForHash().hasKey(REDIS_HASH_KEY, configKey);
        if (Boolean.TRUE.equals(exists)) {
            setConfig(configKey, configValue);
        }
    }
}
