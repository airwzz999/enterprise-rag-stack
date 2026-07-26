package com.knowledge.base.document.feign;

import com.knowledge.base.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * KAG graph construction service Feign client
 *
 * <p>Calls the kb-ai service's KAG graph construction and deletion endpoints.
 * Used to asynchronously notify kb-ai to update the Neo4j knowledge graph when a document is
 * created/updated/published/deleted.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@FeignClient(
        name = "kb-ai",
        path = "/kag/build",
        url = "${kb-ai.url:#{null}}",
        contextId = "kagFeignClient"
)
public interface KAGFeignClient {

    /**
     * Triggers KAG graph construction for a single document
     *
     * @param docId document ID
     * @return task ID
     */
    @PostMapping("/{docId}")
    Result<String> buildGraph(@PathVariable("docId") Long docId);

    /**
     * Deletes the knowledge graph for a single document
     *
     * @param docId document ID
     * @return task ID
     */
    @DeleteMapping("/{docId}")
    Result<String> deleteGraph(@PathVariable("docId") Long docId);
}
