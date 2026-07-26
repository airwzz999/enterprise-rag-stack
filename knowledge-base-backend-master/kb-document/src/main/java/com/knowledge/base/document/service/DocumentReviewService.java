package com.knowledge.base.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.document.dto.DocumentReviewDTO;
import com.knowledge.base.document.dto.ReviewQueryDTO;
import com.knowledge.base.document.entity.DocumentReview;
import com.knowledge.base.document.vo.DocumentReviewVO;

import java.util.List;
import java.util.Map;

/**
 * Document review Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface DocumentReviewService extends IService<DocumentReview> {

    /**
     * Submits a document for review
     *
     * @param documentId document ID
     * @return whether successful
     */
    Boolean submitForReview(Long documentId);

    /**
     * Approves a review
     *
     * @param dto review DTO
     * @return whether successful
     */
    Boolean approveReview(DocumentReviewDTO dto);

    /**
     * Rejects a review
     *
     * @param dto review DTO
     * @return whether successful
     */
    Boolean rejectReview(DocumentReviewDTO dto);

    /**
     * Gets the list of documents pending review
     *
     * @param dto query DTO
     * @return paginated result
     */
    PageResult<DocumentReviewVO> getPendingReviews(ReviewQueryDTO dto);

    /**
     * Gets the current review task for a document.
     *
     * @param documentId document ID
     * @return current review task
     */
    DocumentReviewVO getCurrentReviewTask(Long documentId);

    /**
     * Gets the review history for a document
     *
     * @param documentId document ID
     * @return review history list
     */
    List<DocumentReviewVO> getDocumentReviewHistory(Long documentId);

    /**
     * Gets the count of documents pending review
     *
     * @return pending count
     */
    Long getPendingCount();

    /**
     * Gets review statistics (pending, approved, rejected counts)
     *
     * @return statistics Map: pending/approved/rejected
     */
    Map<String, Long> getReviewStats();

    /**
     * Batch review (approve or reject)
     *
     * @param taskIds review task ID list
     * @param status  review result: approved-approved, rejected-rejected
     * @param comment review comment
     */
    void batchReview(List<Long> taskIds, String status, String comment);
}
