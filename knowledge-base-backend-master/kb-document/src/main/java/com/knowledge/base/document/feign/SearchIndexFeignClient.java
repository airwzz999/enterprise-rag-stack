package com.knowledge.base.document.feign;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.config.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Search index Feign client
 *
 * <p>Calls the kb-search service's document indexing endpoint to synchronize document metadata
 * to the ES kb_document index when a document is created/updated/published/deleted, for use in
 * keyword search.</p>
 *
 * <p>Uses {@link InternalFeignConfig} to authenticate as the internal-service identity
 * (X-User-Id: 0), matching kb-search's own {@code /index/**} endpoints, which are
 * {@code @PreAuthorize}-restricted to admins/internal callers since they write
 * unvalidated data directly into the shared search index.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@FeignClient(
        name = "kb-search",
        path = "/index",
        contextId = "searchIndexFeignClient",
        configuration = InternalFeignConfig.class
)
public interface SearchIndexFeignClient {

    /**
     * Indexes a document
     *
     * @param docData document data (id, title, summary, content, categoryId,
     *                categoryName, tags, authorId, authorName, status,
     *                isPublic, viewCount, likeCount, commentCount,
     *                publishTime, createTime, updateTime)
     * @return whether the operation succeeded
     */
    @PostMapping("/document")
    Result<Boolean> indexDocument(@RequestBody Map<String, Object> docData);

    /**
     * Deletes a document's index entry
     *
     * @param documentId document ID
     * @return whether the operation succeeded
     */
    @DeleteMapping("/document/{documentId}")
    Result<Boolean> deleteDocumentIndex(@PathVariable("documentId") Long documentId);
}
