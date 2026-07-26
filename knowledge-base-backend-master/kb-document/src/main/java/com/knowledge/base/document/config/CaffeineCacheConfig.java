package com.knowledge.base.document.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine local cache configuration
 *
 * <p>Provides JVM in-memory caching for frequently-read data such as the category tree,
 * avoiding a Redis network call on every request.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class CaffeineCacheConfig {

    /**
     * Caffeine local cache manager
     *
     * <p>Default 5-minute expiration, suitable for low-frequency-change scenarios such as the
     * category tree. Data changes are actively evicted via @CacheEvict.</p>
     *
     * @return Caffeine CacheManager
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(20)
                .recordStats());
        return cacheManager;
    }
}
