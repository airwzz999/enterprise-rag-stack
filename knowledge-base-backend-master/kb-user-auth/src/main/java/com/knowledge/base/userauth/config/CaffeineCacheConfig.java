package com.knowledge.base.userauth.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine local cache configuration (L1 cache layer)
 *
 * <p>Caches frequently read data (such as the team space tree) in application memory,
 * avoiding a Redis network round trip on every menu render.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class CaffeineCacheConfig {

    /**
     * Caffeine local cache manager (L1)
     *
     * <p>TTL is set to 35 minutes, with active eviction on data changes.
     * Team structure changes very infrequently, so a short TTL is unnecessary.</p>
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
