package com.knowledge.base.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.StrUtil;
import com.knowledge.base.common.enums.DocumentStatus;
import com.knowledge.base.common.event.ReviewEventDTO;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.document.dto.DocumentReviewDTO;
import com.knowledge.base.document.dto.ReviewQueryDTO;
import com.knowledge.base.document.entity.Document;
import com.knowledge.base.document.entity.DocumentReview;
import com.knowledge.base.document.feign.KAGFeignClient;
import com.knowledge.base.document.feign.RagFeignClient;
import com.knowledge.base.document.feign.SearchIndexFeignClient;
import com.knowledge.base.document.mapper.DocumentMapper;
import com.knowledge.base.document.mapper.DocumentReviewMapper;
import com.knowledge.base.document.service.DocumentReviewService;
import com.knowledge.base.document.utils.UserContext;
import com.knowledge.base.document.vo.DocumentReviewVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.knowledge.base.common.config.SystemConfigCache;
import com.knowledge.base.common.config.InstanceIdentifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Document review Service implementation class
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, implements document review related business logic</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class DocumentReviewServiceImpl extends ServiceImpl<DocumentReviewMapper, DocumentReview> implements DocumentReviewService {

    @Resource
    private DocumentReviewMapper documentReviewMapper;

    @Resource
    private DocumentMapper documentMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private SystemConfigCache systemConfigCache;

    @Resource
    private RagFeignClient ragFeignClient;

    @Resource
    private KAGFeignClient kagFeignClient;

    @Resource
    private SearchIndexFeignClient searchIndexFeignClient;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ThreadPoolTaskExecutor asyncTaskExecutor;

    @Resource
    private InstanceIdentifier instanceIdentifier;

    private static final String REVIEW_EXCHANGE = "kb.notification.exchange";

    /** Builds the instance-isolated review notification routing key */
    private String reviewRoutingKey(String eventType) {
        return "notification.review." + instanceIdentifier.getId() + "." + eventType;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean submitForReview(Long documentId) {
        log.info("Submit document for review: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        // Check whether the document exists
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("Document does not exist");
        }

        // Check system configuration: if document review is disabled, publish the document directly
        if (!checkRequireApproval()) {
            log.info("System configuration has document review disabled, publishing document directly: documentId={}", documentId);
            return directPublishDocument(document);
        }

        // Check document status: draft or published documents can be submitted for review (a published document needs re-review after editing)
        if (!document.getStatus().equals(DocumentStatus.DRAFT.getCode())
                && !document.getStatus().equals(DocumentStatus.PUBLISHED.getCode())) {
            throw new BusinessException("Only draft or published documents can be submitted for review");
        }

        // Get the current round
        Integer currentRound = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(review_round), 0) FROM tb_document_review WHERE document_id = ?",
                Integer.class,
                documentId
        );

        // Create the review record; reviewer is not yet assigned, reviewer_id is left empty
        DocumentReview review = new DocumentReview();
        review.setId(SnowflakeIdGenerator.getInstance().nextId());
        review.setDocumentId(documentId);
        review.setReviewerId(null);
        review.setReviewerName(null);
        review.setReviewResult(null);
        review.setReviewComment(null);
        review.setBeforeStatus(document.getStatus());
        review.setReviewedAt(null);
        review.setReviewRound((currentRound != null ? currentRound : 0) + 1);
        review.setReviewLevel(1);
        review.setCreatedAt(LocalDateTime.now());

        int count = documentReviewMapper.insert(review);
        if (count <= 0) {
            throw new BusinessException("Failed to submit for review");
        }

        // Update the document status to pending review
        document.setStatus(DocumentStatus.PENDING_REVIEW.getCode());
        documentMapper.updateById(document);

        // Publish a review-submitted event -> RabbitMQ, notifying reviewers
        try {
            ReviewEventDTO event = ReviewEventDTO.builder()
                    .eventType("SUBMITTED")
                    .documentId(documentId)
                    .documentTitle(document.getTitle())
                    .authorId(document.getAuthorId())
                    .authorName(document.getAuthorName())
                    .reviewRound(review.getReviewRound())
                    .reviewLevel(review.getReviewLevel())
                    .timestamp(LocalDateTime.now())
                    .build();
            String rk = reviewRoutingKey("submitted");
            CorrelationData correlationData = new CorrelationData(
                    "review-SUBMITTED-" + documentId + "-" + UUID.randomUUID().toString().substring(0, 8));
            rabbitTemplate.convertAndSend(REVIEW_EXCHANGE, rk, event, correlationData);
            log.info("Review-submitted event published: documentId={}, title={}, authorId={}, exchange={}, routingKey={}",
                    documentId, document.getTitle(), document.getAuthorId(), REVIEW_EXCHANGE, rk);
        } catch (Exception e) {
            log.warn("Failed to publish review-submitted event (does not affect the review flow): documentId={}, error={}",
                    documentId, e.getMessage());
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean approveReview(DocumentReviewDTO dto) {
        log.info("Approve review: reviewId={}", dto.getReviewId());

        if (dto.getReviewId() == null) {
            throw new BusinessException("Review record ID must not be null");
        }

        // Check whether the review record exists
        DocumentReview review = documentReviewMapper.selectById(dto.getReviewId());
        if (review == null) {
            throw new BusinessException("Review record does not exist");
        }

        if (review.getReviewResult() != null) {
            throw new BusinessException("This record has already been reviewed");
        }

        // Get the current reviewer information from the context
        Long reviewerId;
        String reviewerName;
        try {
            reviewerId = UserContext.getCurrentUserId();
            reviewerName = UserContext.getCurrentUserName();
        } catch (Exception e) {
            reviewerId = null;
            reviewerName = "Reviewer";
        }

        // Update the review record
        review.setReviewerId(reviewerId);
        review.setReviewerName(reviewerName);
        review.setReviewResult(1); // 1-approved
        review.setReviewComment(dto.getReviewComment());
        review.setReviewedAt(LocalDateTime.now());
        documentReviewMapper.updateById(review);

        // Update the document status to published, and set the publish time
        Document document = documentMapper.selectById(review.getDocumentId());
        if (document != null) {
            document.setStatus(DocumentStatus.PUBLISHED.getCode());
            document.setPublishTime(LocalDateTime.now());
            documentMapper.updateById(document);

            // Trigger RAG/KAG/ES indexing (indexes are built only after review approval)
            triggerRagReindex(document.getId());
            triggerKAGBuild(document.getId());
            triggerSearchIndex(document, null);

            // Publish a review-approved event -> RabbitMQ, notifying the document author
            try {
                ReviewEventDTO event = ReviewEventDTO.builder()
                        .eventType("APPROVED")
                        .documentId(document.getId())
                        .documentTitle(document.getTitle())
                        .authorId(document.getAuthorId())
                        .authorName(document.getAuthorName())
                        .reviewerId(reviewerId)
                        .reviewerName(reviewerName)
                        .reviewRound(review.getReviewRound())
                        .reviewLevel(review.getReviewLevel() != null ? review.getReviewLevel() : 1)
                        .timestamp(LocalDateTime.now())
                        .build();
                CorrelationData correlationData = new CorrelationData(
                        "review-APPROVED-" + document.getId() + "-" + UUID.randomUUID().toString().substring(0, 8));
                rabbitTemplate.convertAndSend(REVIEW_EXCHANGE, reviewRoutingKey("approved"), event, correlationData);
                log.info("Review-approved event published: documentId={}, eventType=APPROVED", document.getId());
            } catch (Exception e) {
                log.warn("Failed to publish review-approved event (does not affect the review flow): documentId={}, error={}",
                        document.getId(), e.getMessage());
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean rejectReview(DocumentReviewDTO dto) {
        log.info("Reject review: reviewId={}", dto.getReviewId());

        if (dto.getReviewId() == null) {
            throw new BusinessException("Review record ID must not be null");
        }

        if (!StringUtils.hasText(dto.getReviewComment())) {
            throw new BusinessException("Rejection comment must not be blank");
        }

        // Check whether the review record exists
        DocumentReview review = documentReviewMapper.selectById(dto.getReviewId());
        if (review == null) {
            throw new BusinessException("Review record does not exist");
        }

        if (review.getReviewResult() != null) {
            throw new BusinessException("This record has already been reviewed");
        }

        // Get the current reviewer information from the context
        Long reviewerId;
        String reviewerName;
        try {
            reviewerId = UserContext.getCurrentUserId();
            reviewerName = UserContext.getCurrentUserName();
        } catch (Exception e) {
            reviewerId = null;
            reviewerName = "Reviewer";
        }

        // Update the review record
        review.setReviewerId(reviewerId);
        review.setReviewerName(reviewerName);
        review.setReviewResult(2); // 2-rejected
        review.setReviewComment(dto.getReviewComment());
        review.setReviewedAt(LocalDateTime.now());
        documentReviewMapper.updateById(review);

        // Revert the document status to draft after rejection
        Document document = documentMapper.selectById(review.getDocumentId());
        if (document != null) {
            document.setStatus(DocumentStatus.DRAFT.getCode());
            documentMapper.updateById(document);

            // Publish a review-rejected event -> RabbitMQ, notifying the document author
            try {
                ReviewEventDTO event = ReviewEventDTO.builder()
                        .eventType("REJECTED")
                        .documentId(document.getId())
                        .documentTitle(document.getTitle())
                        .authorId(document.getAuthorId())
                        .authorName(document.getAuthorName())
                        .reviewerId(reviewerId)
                        .reviewerName(reviewerName)
                        .reviewRound(review.getReviewRound())
                        .reviewLevel(review.getReviewLevel() != null ? review.getReviewLevel() : 1)
                        .reviewComment(dto.getReviewComment())
                        .timestamp(LocalDateTime.now())
                        .build();
                CorrelationData correlationData = new CorrelationData(
                        "review-REJECTED-" + document.getId() + "-" + UUID.randomUUID().toString().substring(0, 8));
                rabbitTemplate.convertAndSend(REVIEW_EXCHANGE, reviewRoutingKey("rejected"), event, correlationData);
                log.info("Review-rejected event published: documentId={}, eventType=REJECTED", document.getId());
            } catch (Exception e) {
                log.warn("Failed to publish review-rejected event (does not affect the review flow): documentId={}, error={}",
                        document.getId(), e.getMessage());
            }
        }

        return true;
    }

    /**
     * Auto-repair: creates a review record for documents whose status is PENDING_REVIEW
     * but which are missing a pending review record
     * <p>This can happen due to direct database manipulation, data migration, etc.</p>
     */
    private void syncOrphanedPendingDocuments() {
        try {
            List<Long> orphanedIds = jdbcTemplate.queryForList(
                    "SELECT d.id FROM kb_document d " +
                    "WHERE d.status = ? AND d.deleted = 0 " +
                    "AND NOT EXISTS (SELECT 1 FROM tb_document_review r WHERE r.document_id = d.id AND r.review_result IS NULL)",
                    Long.class,
                    DocumentStatus.PENDING_REVIEW.getCode()
            );

            if (!orphanedIds.isEmpty()) {
                log.info("Found {} orphaned pending-review documents, auto-creating review records...", orphanedIds.size());
                for (Long docId : orphanedIds) {
                    try {
                        Document document = documentMapper.selectById(docId);
                        if (document == null) continue;

                        Integer currentRound = jdbcTemplate.queryForObject(
                                "SELECT COALESCE(MAX(review_round), 0) FROM tb_document_review WHERE document_id = ?",
                                Integer.class, docId
                        );

                        DocumentReview review = new DocumentReview();
                        review.setId(SnowflakeIdGenerator.getInstance().nextId());
                        review.setDocumentId(docId);
                        review.setReviewerId(null);
                        review.setReviewerName(null);
                        review.setReviewResult(null);
                        review.setReviewComment(null);
                        review.setBeforeStatus(document.getStatus());
                        review.setReviewedAt(null);
                        review.setReviewRound((currentRound != null ? currentRound : 0) + 1);
                        review.setReviewLevel(1);
                        review.setCreatedAt(LocalDateTime.now());
                        documentReviewMapper.insert(review);
                    } catch (Exception e) {
                        log.warn("Failed to create review record: documentId={}, error={}", docId, e.getMessage());
                    }
                }
                log.info("Finished creating records for orphaned pending-review documents, {} processed in total", orphanedIds.size());
            }
        } catch (Exception e) {
            log.warn("Failed to check for orphaned pending-review documents (does not affect the query flow): {}", e.getMessage());
        }
    }

    @Override
    public PageResult<DocumentReviewVO> getPendingReviews(ReviewQueryDTO dto) {
        // Auto-repair: creates a review record for documents whose status is PENDING_REVIEW
        // but which are missing a pending review record
        syncOrphanedPendingDocuments();

        // Build the query conditions
        LambdaQueryWrapper<DocumentReview> wrapper = new LambdaQueryWrapper<>();

        // Status filter: 0=pending(null), 1=approved, 2=rejected, null=all
        if (dto.getStatus() == null) {
            // No filter
        } else if (dto.getStatus() == 0) {
            wrapper.isNull(DocumentReview::getReviewResult);
        } else {
            wrapper.eq(DocumentReview::getReviewResult, dto.getStatus());
        }

        if (dto.getReviewerId() != null) {
            wrapper.eq(DocumentReview::getReviewerId, dto.getReviewerId());
        }

        if (dto.getAuthorId() != null) {
            wrapper.exists(
                    "SELECT 1 FROM kb_document d WHERE d.id = tb_document_review.document_id AND d.author_id = {0}",
                    dto.getAuthorId()
            );
        }

        // Keyword search
        if (StringUtils.hasText(dto.getKeyword())) {
            // Subquery on document title
            wrapper.exists(
                    "SELECT 1 FROM kb_document d WHERE d.id = tb_document_review.document_id AND d.title LIKE CONCAT('%', {0}, '%')",
                    dto.getKeyword()
            );
        }

        // Sort
        wrapper.orderByDesc(DocumentReview::getCreatedAt);

        // Paginated query
        Page<DocumentReview> page = new Page<>(dto.getCurrent(), dto.getSize());
        IPage<DocumentReview> reviewPage = documentReviewMapper.selectPage(page, wrapper);

        // Convert to VO
        IPage<DocumentReviewVO> voPage = reviewPage.convert(review -> {
            return buildReviewVO(review);
        });

        return PageResult.<DocumentReviewVO>builder()
                .records(voPage.getRecords())
                .total(voPage.getTotal())
                .current(voPage.getCurrent())
                .size(voPage.getSize())
                .build();
    }

    @Override
    public DocumentReviewVO getCurrentReviewTask(Long documentId) {
        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        syncOrphanedPendingDocuments();
        DocumentReview review = documentReviewMapper.selectLatestByDocumentId(documentId);
        if (review == null) {
            throw new BusinessException("Review record not found");
        }
        return buildReviewVO(review);
    }

    @Override
    public List<DocumentReviewVO> getDocumentReviewHistory(Long documentId) {
        if (documentId == null) {
            throw new BusinessException("Document ID must not be null");
        }

        List<DocumentReview> reviews = documentReviewMapper.selectList(
                new LambdaQueryWrapper<DocumentReview>()
                        .eq(DocumentReview::getDocumentId, documentId)
                        .orderByDesc(DocumentReview::getReviewRound)
        );

        return reviews.stream()
                .map(this::buildReviewVO)
                .collect(Collectors.toList());
    }

    @Override
    public Long getPendingCount() {
        LambdaQueryWrapper<DocumentReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(DocumentReview::getReviewResult);
        return documentReviewMapper.selectCount(wrapper);
    }

    @Override
    public Map<String, Long> getReviewStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("pending", documentReviewMapper.selectCount(
                new LambdaQueryWrapper<DocumentReview>().isNull(DocumentReview::getReviewResult)));
        stats.put("approved", documentReviewMapper.selectCount(
                new LambdaQueryWrapper<DocumentReview>().eq(DocumentReview::getReviewResult, 1)));
        stats.put("rejected", documentReviewMapper.selectCount(
                new LambdaQueryWrapper<DocumentReview>().eq(DocumentReview::getReviewResult, 2)));
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchReview(List<Long> taskIds, String status, String comment) {
        if (taskIds == null || taskIds.isEmpty()) {
            throw new BusinessException("Review task ID list must not be empty");
        }

        DocumentReviewDTO dto = new DocumentReviewDTO();
        dto.setReviewComment(comment);

        for (Long taskId : taskIds) {
            dto.setReviewId(taskId);
            if ("approved".equalsIgnoreCase(status)) {
                approveReview(dto);
            } else if ("rejected".equalsIgnoreCase(status)) {
                rejectReview(dto);
            } else {
                throw new BusinessException("Invalid review result: " + status);
            }
        }
    }

    private DocumentReviewVO buildReviewVO(DocumentReview review) {
        Document document = documentMapper.selectById(review.getDocumentId());
        String documentTitle = document != null ? document.getTitle() : "";
        String authorName = document != null ? document.getAuthorName() : "";
        Long authorId = document != null ? document.getAuthorId() : null;

        Long categoryId = document != null ? document.getCategoryId() : null;
        String categoryName = "";
        if (categoryId != null) {
            try {
                categoryName = jdbcTemplate.queryForObject(
                        "SELECT category_name FROM kb_category WHERE id = ?",
                        String.class,
                        categoryId
                );
            } catch (Exception ignored) {
                // Returns an empty string if the category was deleted or does not exist, without affecting the review page display
            }
        }

        return DocumentReviewVO.builder()
                .id(review.getId())
                .documentId(review.getDocumentId())
                .documentTitle(documentTitle)
                .authorId(authorId)
                .authorName(authorName)
                .reviewerId(review.getReviewerId())
                .reviewerName(review.getReviewerName())
                .reviewResult(review.getReviewResult())
                .reviewComment(review.getReviewComment())
                .beforeStatus(review.getBeforeStatus())
                .reviewedAt(review.getReviewedAt())
                .reviewRound(review.getReviewRound())
                .createdAt(review.getCreatedAt())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .build();
    }

    // ==================== Async index-triggering methods ====================

    /**
     * Asynchronously triggers RAG index rebuild
     */
    private void triggerRagReindex(Long docId) {
        CompletableFuture.runAsync(() -> {
            try {
                ragFeignClient.reindexDocument(docId);
                log.info("Triggered RAG index rebuild: documentId={}", docId);
            } catch (Exception e) {
                log.warn("Failed to trigger RAG index rebuild (RAG service unavailable, does not affect document operations): documentId={}, error={}",
                        docId, e.getMessage());
            }
        }, asyncTaskExecutor);
    }

    /**
     * Asynchronously triggers KAG graph construction
     */
    private void triggerKAGBuild(Long docId) {
        CompletableFuture.runAsync(() -> {
            try {
                kagFeignClient.buildGraph(docId);
                log.info("Triggered KAG graph construction: documentId={}", docId);
            } catch (Exception e) {
                log.warn("Failed to trigger KAG graph construction (KAG service unavailable, does not affect document operations): documentId={}, error={}",
                        docId, e.getMessage());
            }
        }, asyncTaskExecutor);
    }

    /**
     * Asynchronously synchronizes to the ES search index
     */
    private void triggerSearchIndex(Document document, String content) {
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> docData = buildSearchIndexData(document, content);
                searchIndexFeignClient.indexDocument(docData);
                log.info("Synchronized to ES search index: documentId={}, title={}", document.getId(), document.getTitle());
            } catch (Exception e) {
                log.warn("Failed to synchronize ES search index (search service unavailable, does not affect document operations): documentId={}, error={}",
                        document.getId(), e.getMessage());
            }
        }, asyncTaskExecutor);
    }

    /**
     * Builds the ES search index data
     *
     * @param document document entity
     * @param content  document content (may be null)
     * @return index data Map
     */
    private Map<String, Object> buildSearchIndexData(Document document, String content) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", document.getId());
        data.put("title", document.getTitle());
        data.put("summary", document.getSummary());
        if (StrUtil.isNotBlank(content)) {
            data.put("content", content.length() > 1000 ? content.substring(0, 1000) : content);
        } else if (StrUtil.isNotBlank(document.getContent())) {
            String c = document.getContent();
            data.put("content", c.length() > 1000 ? c.substring(0, 1000) : c);
        }
        data.put("categoryId", document.getCategoryId());
        data.put("tags", document.getTags());
        data.put("status", document.getStatus());
        data.put("isPublic", document.getIsPublic());
        data.put("viewCount", document.getViewCount());
        data.put("likeCount", document.getLikeCount());
        data.put("commentCount", document.getCommentCount());
        data.put("authorId", document.getAuthorId());
        data.put("author", Map.of("id", document.getAuthorId() != null ? document.getAuthorId() : 0,
                "name", document.getAuthorName() != null ? document.getAuthorName() : ""));
        if (document.getPublishTime() != null) {
            data.put("publishTime", document.getPublishTime().toString());
        }
        data.put("createdAt", document.getCreatedAt() != null ? document.getCreatedAt().toString() : "");
        return data;
    }

    // ==================== Direct publish method ====================

    /**
     * Checks system configuration: whether document review is required
     */
    private boolean checkRequireApproval() {
        String value = systemConfigCache.getConfig("system.requireApproval");
        return !"false".equals(value);
    }

    /**
     * Directly publishes a document (no review required)
     * <p>Called when the system.requireApproval configuration is set to false</p>
     */
    private Boolean directPublishDocument(Document document) {
        // Only draft, published, or pending-review documents can be published directly
        Integer status = document.getStatus();
        if (!status.equals(DocumentStatus.DRAFT.getCode())
                && !status.equals(DocumentStatus.PUBLISHED.getCode())
                && !status.equals(DocumentStatus.PENDING_REVIEW.getCode())) {
            throw new BusinessException("The current document status does not allow publishing");
        }

        // Set directly to published
        document.setStatus(DocumentStatus.PUBLISHED.getCode());
        document.setPublishTime(LocalDateTime.now());
        documentMapper.updateById(document);

        // Trigger RAG/KAG/ES indexing
        triggerRagReindex(document.getId());
        triggerKAGBuild(document.getId());
        triggerSearchIndex(document, null);

        log.info("Document published directly successfully: documentId={}", document.getId());
        return true;
    }
}
