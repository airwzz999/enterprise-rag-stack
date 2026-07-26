package com.knowledge.base.ai.client;

import com.knowledge.base.ai.config.InternalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Feign client for the kb-document service
 *
 * <p>Used by kb-ai to call kb-document to retrieve document content and metadata,
 * supporting document indexing and reindexing scenarios.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@FeignClient(name = "kb-document", url = "${kb-document.url:#{null}}",
        configuration = InternalFeignConfig.class)
public interface DocumentFeignClient {

    /**
     * Get document details (including MongoDB content)
     */
    @GetMapping("/documents/{documentId}")
    Map<String, Object> getDocument(@PathVariable("documentId") Long documentId);

    /**
     * Get a paginated list of published documents
     */
    @GetMapping("/documents/page")
    Map<String, Object> pageDocuments(@RequestParam("current") Long current,
                                       @RequestParam("size") Long size,
                                       @RequestParam(value = "status", required = false) Integer status);
}
