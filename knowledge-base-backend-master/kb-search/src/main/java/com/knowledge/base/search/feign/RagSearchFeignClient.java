package com.knowledge.base.search.feign;

import com.knowledge.base.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * kb-ai RAG hybrid search Feign client
 *
 * @author airwzz999
 * @since 1.0.0
 */
@FeignClient(
        name = "kb-ai",
        path = "/rag",
        contextId = "ragSearchFeignClient"
)
public interface RagSearchFeignClient {

    /**
     * Hybrid search (BM25 + kNN + RRF fusion)
     *
     * @param request the search request
     * @return the list of search results
     */
    @PostMapping("/search")
    Result<List<RagSearchItemVO>> search(@RequestBody RagSearchRequest request);
}
