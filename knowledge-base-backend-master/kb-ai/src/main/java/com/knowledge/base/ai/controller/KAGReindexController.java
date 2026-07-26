package com.knowledge.base.ai.controller;

import com.knowledge.base.ai.rag.kag.graph.GraphBuildService;
import com.knowledge.base.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * KAG graph build management controller
 *
 * <p>Receives frontend requests and delegates graph build/delete tasks to
 * {@link GraphBuildService} for publishing to RabbitMQ. The controller itself
 * contains no business logic and only handles parameter extraction and delegation
 * to the service layer.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/kag/build")
@Tag(name = "KAG Graph Management", description = "Knowledge graph build and index management APIs")
public class KAGReindexController {

    @Resource
    private GraphBuildService graphBuildService;

    /**
     * Build the knowledge graph for a single document
     *
     * <p>Publishes an asynchronous task to RabbitMQ; executes synchronously if MQ is unavailable.</p>
     *
     * @param docId document ID
     * @return task ID
     */
    @PostMapping("/{docId}")
    @Operation(summary = "Build the graph for a single document")
    public Result<String> buildByDoc(@PathVariable Long docId) {
        String taskId = graphBuildService.publishBuildTask(docId);
        return Result.success("Graph build task submitted", taskId);
    }

    /**
     * Batch-build the knowledge graph for the specified documents
     *
     * <p>Publishes an asynchronous batch task to RabbitMQ; executes synchronously if MQ is unavailable.</p>
     *
     * @param docIds list of document IDs
     * @return task ID
     */
    @PostMapping("/batch")
    @Operation(summary = "Batch-build document graphs")
    public Result<String> buildBatch(@RequestBody List<Long> docIds) {
        String taskId = graphBuildService.publishBuildBatchTask(docIds);
        return Result.success("Batch graph build task submitted", taskId);
    }

    /**
     * Fully build the knowledge graph for all published documents
     *
     * <p>Publishes an asynchronous full-build task to RabbitMQ; executes synchronously if MQ is unavailable.</p>
     *
     * @return task ID
     */
    @PostMapping("/all")
    @Operation(summary = "Fully build the graph for all published documents")
    public Result<String> buildAll() {
        String taskId = graphBuildService.publishBuildAllTask();
        return Result.success("Full graph build task submitted", taskId);
    }

    /**
     * Delete the knowledge graph nodes and relationships for the specified document
     *
     * <p>Publishes an asynchronous delete task to RabbitMQ; executes synchronously if MQ is unavailable.</p>
     *
     * @param docId document ID
     * @return task ID
     */
    @DeleteMapping("/{docId}")
    @Operation(summary = "Delete a document's graph")
    public Result<String> deleteByDoc(@PathVariable Long docId) {
        String taskId = graphBuildService.publishDeleteTask(docId);
        return Result.success("Graph delete task submitted", taskId);
    }
}
