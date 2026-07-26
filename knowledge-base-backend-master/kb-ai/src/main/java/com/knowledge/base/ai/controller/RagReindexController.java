package com.knowledge.base.ai.controller;

import com.knowledge.base.ai.dto.ReindexRequestDTO;
import com.knowledge.base.ai.rag.service.ReindexService;
import com.knowledge.base.ai.vo.ReindexProgressVO;
import com.knowledge.base.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * RAG reindex controller
 *
 * <p>Provides document indexing management APIs: single-document indexing, batch
 * indexing, full rebuild, and progress queries.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/rag/reindex")
@RequiredArgsConstructor
@Tag(name = "RAG Index Management", description = "Knowledge base RAG index management APIs")
public class RagReindexController {

    private final ReindexService reindexService;

    /**
     * Reindex a single document
     */
    @PostMapping("/{docId}")
    @Operation(summary = "Reindex a single document", description = "Re-chunk, re-embed, and reindex the specified document")
    public Result<String> reindexByDoc(@PathVariable Long docId) {
        log.info("Reindexing document: documentId={}", docId);
        String taskId = reindexService.reindexByDocId(docId);
        return Result.success(taskId);
    }

    /**
     * Batch-reindex documents
     */
    @PostMapping("/batch")
    @Operation(summary = "Batch-reindex documents", description = "Re-chunk, re-embed, and reindex the specified documents")
    public Result<String> reindexBatch(@Valid @RequestBody ReindexRequestDTO dto) {
        log.info("Batch-reindexing documents: count={}", dto.getDocumentIds().size());
        String taskId = reindexService.reindexBatch(dto.getDocumentIds());
        return Result.success(taskId);
    }

    /**
     * Reindex all documents
     */
    @PostMapping("/all")
    @Operation(summary = "Reindex all documents", description = "Re-chunk, re-embed, and reindex all published documents")
    public Result<String> reindexAll() {
        log.info("Fully reindexing documents");
        String taskId = reindexService.reindexAll();
        return Result.success(taskId);
    }

    /**
     * Delete the vector index for a single document
     */
    @DeleteMapping("/{docId}")
    @Operation(summary = "Delete a single document's vector index", description = "Delete all chunks for the specified document from the ES vector store")
    public Result<String> deleteByDoc(@PathVariable Long docId) {
        log.info("Deleting document vector index: documentId={}", docId);
        String taskId = reindexService.deleteByDocId(docId);
        return Result.success(taskId);
    }

    /**
     * Query reindex progress
     */
    @GetMapping("/progress/{taskId}")
    @Operation(summary = "Query reindex progress", description = "Query index rebuild progress by task ID")
    public Result<ReindexProgressVO> getProgress(@PathVariable String taskId) {
        ReindexProgressVO progress = reindexService.getProgress(taskId);
        return Result.success(progress);
    }
}
