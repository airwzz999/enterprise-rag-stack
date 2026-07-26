package com.knowledge.base.ai.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Local cache configuration
 *
 * <p>Uses Caffeine as the local cache implementation, providing high-performance caching
 * for frequently accessed read-only data.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure the Caffeine CacheManager
     *
     * <p>Caches read-only data such as writing templates for 24 hours, avoiding
     * rebuilding the list on every request.</p>
     *
     * @return the CacheManager instance
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)
                .maximumSize(100)
                .recordStats());
        return cacheManager;
    }
}
