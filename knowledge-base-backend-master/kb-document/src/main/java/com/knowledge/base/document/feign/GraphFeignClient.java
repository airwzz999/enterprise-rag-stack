package com.knowledge.base.document.feign;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.config.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * Knowledge graph service Feign client
 *
 * <p>Calls the kb-graph service's graph deletion endpoint to delete document graph data directly
 * in Neo4j, serving as a synchronous backup path for KAG's asynchronous RabbitMQ-based deletion.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@FeignClient(
        name = "kb-graph",
        path = "/graph",
        contextId = "graphFeignClient",
        configuration = InternalFeignConfig.class
)
public interface GraphFeignClient {

    /**
     * Deletes the knowledge graph data for a given document
     *
     * @param docId document ID
     * @return operation result
     */
    @DeleteMapping("/document/{docId}")
    Result<String> deleteDocumentGraph(@PathVariable("docId") Long docId);

    /**
     * Cleans up stale graph nodes (nodes whose docId is not in the whitelist are deleted)
     *
     * @param body request body containing the validDocIds list
     * @return operation result
     */
    @PostMapping("/document/cleanup")
    Result<String> cleanupDocumentGraph(@RequestBody Map<String, List<Long>> body);
}
