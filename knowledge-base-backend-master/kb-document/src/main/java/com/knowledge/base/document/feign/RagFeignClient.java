package com.knowledge.base.document.feign;

import com.knowledge.base.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * RAG indexing service Feign client
 *
 * <p>Calls the kb-ai service's RAG reindex and remove-index endpoints.
 * Used to asynchronously notify kb-ai to update the vector index when a document is
 * created/updated/published/deleted.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@FeignClient(
        name = "kb-ai",
        path = "/rag/reindex",
        contextId = "ragFeignClient"
)
public interface RagFeignClient {

    /**
     * Triggers a RAG reindex for a single document
     *
     * <p>Called after a document is created or its content updated; kb-ai processes this
     * asynchronously via RabbitMQ.</p>
     *
     * @param docId document ID
     * @return task ID
     */
    @PostMapping("/{docId}")
    Result<String> reindexDocument(@PathVariable("docId") Long docId);

    /**
     * Removes the RAG vector index for a single document
     *
     * <p>Called when a document is deleted, unpublished, or archived; kb-ai processes this
     * asynchronously via RabbitMQ.</p>
     *
     * @param docId document ID
     * @return task ID
     */
    @DeleteMapping("/{docId}")
    Result<String> removeFromIndex(@PathVariable("docId") Long docId);
}
