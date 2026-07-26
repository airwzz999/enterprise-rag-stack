package com.knowledge.base.statistics.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine local cache configuration
 *
 * <p>Serves as the L1 local cache layer, used alongside the Redis L2 cache.
 * Frequently-read data such as popular documents is read from Caffeine first, reducing Redis network overhead.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class CacheConfig {

    /**
     * Caffeine local cache manager (L1)
     *
     * <p>The expiration time is set to 35 minutes, slightly longer than the 30-minute refresh cycle,
     * ensuring the cache does not expire before the scheduled task refreshes it.</p>
     *
     * @return the Caffeine CacheManager instance
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(35, TimeUnit.MINUTES)
                .maximumSize(10)
                .recordStats());
        return cacheManager;
    }
}
