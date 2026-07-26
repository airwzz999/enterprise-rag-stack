package com.knowledge.base.document.controller;

import com.knowledge.base.common.annotation.OperationLog;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.dto.BatchReviewDTO;
import com.knowledge.base.document.dto.DocumentReviewDTO;
import com.knowledge.base.document.dto.ReviewActionDTO;
import com.knowledge.base.document.dto.ReviewQueryDTO;
import com.knowledge.base.document.service.DocumentReviewService;
import com.knowledge.base.document.vo.DocumentReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Document review Controller
 *
 * <p>Mapped to path /review, accessed via the gateway at /api/document/review/**.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
@Tag(name = "Document Review", description = "Document review related endpoints")
public class DocumentReviewController {

    private final DocumentReviewService reviewService;

    /**
     * Submits a document for review
     */
    @PostMapping("/submit/{documentId}")
    @Operation(summary = "Submit document for review", description = "Submits a document for review")
    @OperationLog(module = "Document Review", operation = "Submit Review", description = "Submits a document for review")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_REVIEW)")
    public Result<Boolean> submitForReview(@PathVariable Long documentId) {
        Boolean result = reviewService.submitForReview(documentId);
        return Result.success(result);
    }

    /**
     * Gets the review task list (supports filtering by status)
     */
    @GetMapping("/tasks")
    @Operation(summary = "Get review task list", description = "Paginated query of review tasks, supports filtering by status. Reviewers can view all tasks; regular users can view their own via the authorId parameter")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_REVIEW, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_LIST)")
    public Result<PageResult<DocumentReviewVO>> getReviewTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long authorId) {
        ReviewQueryDTO dto = new ReviewQueryDTO();
        dto.setCurrent(page.longValue());
        dto.setSize(pageSize.longValue());
        dto.setKeyword(keyword);
        dto.setAuthorId(authorId);

        // Status mapping: pending->0, approved->1, rejected->2
        if (status != null) {
            dto.setStatus("pending".equals(status) ? 0 :
                          "approved".equals(status) ? 1 :
                          "rejected".equals(status) ? 2 : null);
        }

        PageResult<DocumentReviewVO> pageResult = reviewService.getPendingReviews(dto);
        return Result.success(pageResult);
    }

    /**
     * Gets the current review task for a document
     */
    @GetMapping("/documents/{documentId}/current")
    @Operation(summary = "Get current review task for a document", description = "Gets the latest review task for a document, used by the standalone review page")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_REVIEW)")
    public Result<DocumentReviewVO> getCurrentReviewTask(@PathVariable Long documentId) {
        return Result.success(reviewService.getCurrentReviewTask(documentId));
    }

    /**
     * Gets the count of pending review tasks
     */
    @GetMapping("/tasks/pending-count")
    @Operation(summary = "Get pending review task count", description = "Gets the number of documents currently pending review")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_REVIEW)")
    public Result<Long> getPendingCount() {
        Long count = reviewService.getPendingCount();
        return Result.success(count);
    }

    /**
     * Gets review statistics
     */
    @GetMapping("/tasks/stats")
    @Operation(summary = "Get review statistics", description = "Gets the counts of pending, approved, and rejected reviews")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_REVIEW)")
    public Result<Map<String, Long>> getReviewStats() {
        Map<String, Long> stats = reviewService.getReviewStats();
        return Result.success(stats);
    }

    /**
     * Single review action (approve or reject)
     */
    @PostMapping("/tasks/{taskId}/review")
    @Operation(summary = "Review document", description = "Reviews a document (approve or reject)")
    @OperationLog(module = "Document Review", operation = "Review Action", description = "Reviews a document")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_REVIEW)")
    public Result<Boolean> reviewDocument(@PathVariable Long taskId,
                                           @Valid @RequestBody ReviewActionDTO dto) {
        DocumentReviewDTO reviewDTO = new DocumentReviewDTO();
        reviewDTO.setReviewId(taskId);
        reviewDTO.setReviewComment(dto.getComment());

        Boolean result;
        if ("approved".equalsIgnoreCase(dto.getStatus())) {
            reviewDTO.setReviewResult(1);
            result = reviewService.approveReview(reviewDTO);
        } else if ("rejected".equalsIgnoreCase(dto.getStatus())) {
            reviewDTO.setReviewResult(2);
            result = reviewService.rejectReview(reviewDTO);
        } else {
            return Result.error("Invalid review result: " + dto.getStatus());
        }

        return Result.success(result);
    }

    /**
     * Batch review (approve or reject)
     */
    @PostMapping("/tasks/batch-review")
    @Operation(summary = "Batch review", description = "Reviews multiple documents in a batch (approve or reject)")
    @OperationLog(module = "Document Review", operation = "Batch Review", description = "Batch reviews documents")
    @PreAuthorize("hasAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_REVIEW)")
    public Result<String> batchReview(@Valid @RequestBody BatchReviewDTO dto) {
        reviewService.batchReview(dto.getTaskIds(), dto.getStatus(), dto.getComment());
        return Result.success("Batch review complete");
    }

    /**
     * Gets the review history for a document
     */
    @GetMapping("/documents/{documentId}/history")
    @Operation(summary = "Get review history", description = "Gets the review history for a document")
    @PreAuthorize("hasAnyAuthority(T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_EDIT, T(com.knowledge.base.document.constants.DocumentPermissionConstants).DOCUMENT_REVIEW)")
    public Result<List<DocumentReviewVO>> getDocumentReviewHistory(@PathVariable Long documentId) {
        List<DocumentReviewVO> history = reviewService.getDocumentReviewHistory(documentId);
        return Result.success(history);
    }
}
