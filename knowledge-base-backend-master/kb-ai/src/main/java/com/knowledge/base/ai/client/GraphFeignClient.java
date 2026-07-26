package com.knowledge.base.ai.client;

import com.knowledge.base.ai.config.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

/**
 * Feign client for the kb-graph service
 *
 * <p>Used by kb-ai to clear kb-graph's Redis cache after a knowledge graph rebuild
 * completes, ensuring the frontend gets the latest data when querying the knowledge graph.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@FeignClient(name = "kb-graph", url = "${kb-graph.url:#{null}}",
        path = "/graph",
        configuration = InternalFeignConfig.class)
public interface GraphFeignClient {

    /**
     * Clear all Redis caches for the graph
     *
     * <p>Called after a knowledge graph rebuild to invalidate all of kb-graph's
     * @Cacheable caches, forcing the next query to reload from Neo4j.</p>
     *
     * @return operation result
     */
    @PostMapping("/cache/evict")
    Map<String, Object> evictAllGraphCaches();
}
