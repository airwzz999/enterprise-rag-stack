package com.knowledge.base.ai.controller;

import com.knowledge.base.ai.dto.RagSearchRequestDTO;
import com.knowledge.base.ai.rag.service.RagRetrievalService;
import com.knowledge.base.ai.vo.RagSearchResultVO;
import com.knowledge.base.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RAG retrieval controller
 *
 * <p>Provides a standalone RAG retrieval API (without LLM generation), for debugging
 * and pure search scenarios.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/rag/search")
@RequiredArgsConstructor
@Tag(name = "RAG Retrieval", description = "Knowledge base RAG retrieval API")
public class RagSearchController {

    private final RagRetrievalService ragRetrievalService;

    /**
     * RAG retrieval
     */
    @PostMapping
    @Operation(summary = "RAG retrieval", description = "RAG-based knowledge base retrieval without generating an answer")
    public Result<List<RagSearchResultVO>> search(@Valid @RequestBody RagSearchRequestDTO requestDTO) {
        log.info("RAG retrieval request: query={}, topK={}", requestDTO.getQuery(), requestDTO.getTopK());
        List<RagSearchResultVO> results = ragRetrievalService.retrieve(
                requestDTO.getQuery(), requestDTO.getTopK(), requestDTO.isEnableRerank());
        return Result.success(results);
    }
}
