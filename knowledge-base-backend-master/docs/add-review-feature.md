# Adding the Review Feature

## I. Overview

The document review feature is an important mechanism for content quality control in the knowledge base system. Through a review workflow, it ensures the quality and compliance of published content. This article describes in detail how to implement a complete document review feature in the kb-document service.

### 1.1 Feature Positioning

The review system, as a core feature for content quality control, provides the following core capabilities:

| Feature Module | Description | Use Case |
|---------|------|---------|
| Submit for review | Submit a draft document for review | Requesting publication after finishing editing |
| Approve | The document is automatically published once approved | Content meets requirements and may be published |
| Reject | Reject the document and return it to draft status | Content does not meet requirements and needs revision |
| Pending review list | View documents awaiting review | Reviewer's workbench |
| Review history | View all review records for a document | Tracing the review process |

### 1.2 Review Workflow Design

**Standard review workflow:**

```
Draft (0) → Submit for review → Pending review (3) → Approved → Published (1)
                                   ↓
                              Rejected
                                   ↓
                               Draft (0)
```

**Review status descriptions:**

| Status Code | Status Name | Description |
|-------|---------|------|
| 0 | Draft | Document is being edited, not yet submitted for review |
| 1 | Published | Approved and published externally |
| 2 | Unpublished | Taken down after having been published |
| 3 | Pending review | Submitted for review, awaiting review |

**Review result descriptions:**

| Result Code | Result Name | Document Status Change |
|-------|---------|-------------|
| 1 | Approved | 3 → 1 (pending review → published) |
| 2 | Rejected | 3 → 0 (pending review → draft) |

### 1.3 Review Round Mechanism

The system supports multiple rounds of review, recorded via the `reviewRound` field:

1. **First review:** reviewRound = 1
2. **Resubmission after rejection:** reviewRound = 2
3. **Submission after being rejected again:** reviewRound = 3
4. **And so on...**

**Round calculation:**
```sql
SELECT COALESCE(MAX(review_round), 0) + 1
FROM tb_document_review
WHERE document_id = ?
```

### 1.4 Data Model

```
Document
    ├── Review record 1 (DocumentReview) - round 1 review
    ├── Review record 2 (DocumentReview) - round 2 review (resubmitted after rejection)
    └── Review record 3 (DocumentReview) - round 3 review
```

---

## II. Database Design

### 2.1 Document Review Table Structure

```sql
-- Document review table
DROP TABLE IF EXISTS `tb_document_review`;
CREATE TABLE `tb_document_review` (
    `id` BIGINT(20) PRIMARY KEY COMMENT 'Review record ID (Snowflake ID)',
    `document_id` BIGINT(20) NOT NULL COMMENT 'Document ID',

    `reviewer_id` BIGINT(20) NOT NULL COMMENT 'Reviewer ID',
    `reviewer_name` VARCHAR(50) COMMENT 'Reviewer name',

    `review_result` TINYINT NOT NULL COMMENT 'Review result: 1-approved, 2-rejected',
    `review_comment` TEXT COMMENT 'Review comment',

    `before_status` TINYINT COMMENT 'Status before review',

    `reviewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Review time',

    `review_round` INT DEFAULT 1 COMMENT 'Review round',

    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',

    KEY `idx_document_id` (`document_id`),
    KEY `idx_reviewer_id` (`reviewer_id`),
    KEY `idx_reviewed_at` (`reviewed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Document review record table';
```

### 2.2 Field Descriptions

| Field | Type | Description | Required | Default |
|------|------|------|------|--------|
| id | BIGINT | Review record ID (Snowflake ID) | Yes | Auto-generated |
| document_id | BIGINT | Document ID | Yes | - |
| reviewer_id | BIGINT | Reviewer ID | Yes | - |
| reviewer_name | VARCHAR(50) | Reviewer name (denormalized) | No | - |
| review_result | TINYINT | Review result (1-approved, 2-rejected) | Yes | - |
| review_comment | TEXT | Review comment | No | - |
| before_status | TINYINT | Status before review (denormalized) | No | - |
| reviewed_at | DATETIME | Review time | Yes | CURRENT_TIMESTAMP |
| review_round | INT | Review round | Yes | 1 |
| created_at | DATETIME | Creation time | Yes | CURRENT_TIMESTAMP |

### 2.3 Index Design

- **Primary key index**: `id` - uniquely identifies the primary key
- **Regular index**: `idx_document_id` - speeds up querying review records by document
- **Regular index**: `idx_reviewer_id` - speeds up querying by reviewer
- **Regular index**: `idx_reviewed_at` - speeds up sorting by time

### 2.4 Document Status Field Description

The `status` field in the document table (kb_document) needs to support review-related statuses:

| Status Value | Status Name | Description |
|-------|---------|------|
| 0 | Draft | Document is being edited |
| 1 | Published | Approved and published |
| 2 | Unpublished | Taken down after having been published |
| 3 | Pending review | Submitted for review, awaiting review |

---

## III. Entity Class Design

### 3.1 Create the DocumentReview Entity Class

Create `kb-document/src/main/java/com/knowledge/base/document/entity/DocumentReview.java`:

```java
package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Document review record entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@TableName("tb_document_review")
@Schema(description = "Document review record entity")
public class DocumentReview {

    /**
     * Review record ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "Review record ID")
    private Long id;

    /**
     * Document ID
     */
    @Schema(description = "Document ID")
    private Long documentId;

    /**
     * Reviewer ID
     */
    @Schema(description = "Reviewer ID")
    private Long reviewerId;

    /**
     * Reviewer name
     */
    @Schema(description = "Reviewer name")
    private String reviewerName;

    /**
     * Review result: 1-approved, 2-rejected
     */
    @Schema(description = "Review result")
    private Integer reviewResult;

    /**
     * Review comment
     */
    @Schema(description = "Review comment")
    private String reviewComment;

    /**
     * Status before review
     */
    @Schema(description = "Status before review")
    private Integer beforeStatus;

    /**
     * Review time
     */
    @Schema(description = "Review time")
    private LocalDateTime reviewedAt;

    /**
     * Review round
     */
    @Schema(description = "Review round")
    private Integer reviewRound;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
```

**Design points:**
- Uses `@TableId(type = IdType.ASSIGN_ID)` to configure the Snowflake ID generation strategy
- Denormalizes the reviewer name to avoid join queries
- Records the status before review, to support tracing the status history
- Records the review round, to support multiple review rounds

---

## IV. DTO Design

### 4.1 Create the Document Review DTO

Create `kb-document/src/main/java/com/knowledge/base/document/dto/DocumentReviewDTO.java`:

```java
package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Document review DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document review parameters")
public class DocumentReviewDTO {

    /**
     * Review record ID
     */
    @Schema(description = "Review record ID")
    @NotNull(message = "Review record ID must not be empty")
    private Long reviewId;

    /**
     * Review result: 1-approved, 2-rejected
     */
    @Schema(description = "Review result: 1-approved, 2-rejected")
    @NotNull(message = "Review result must not be empty")
    private Integer reviewResult;

    /**
     * Review comment
     */
    @Schema(description = "Review comment")
    private String reviewComment;
}
```

**Design points:**
- `reviewId` specifies which record to review
- `reviewResult` distinguishes between approval and rejection
- `reviewComment` records the review comment; required when rejecting

### 4.2 Create the Review Query DTO

Create `kb-document/src/main/java/com/knowledge/base/document/dto/ReviewQueryDTO.java`:

```java
package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Review query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Review query request")
public class ReviewQueryDTO {

    @Schema(description = "Current page")
    private Long current = 1L;

    @Schema(description = "Page size")
    private Long size = 10L;

    @Schema(description = "Review status: 0-pending, 1-approved, 2-rejected")
    private Integer status;

    @Schema(description = "Reviewer ID")
    private Long reviewerId;

    @Schema(description = "Keyword search")
    private String keyword;
}
```

**Design points:**
- Supports paginated querying
- Supports filtering by reviewer
- Supports keyword search (searches the document title)

---

## V. VO Design

### 5.1 Create the Document Review VO

Create `kb-document/src/main/java/com/knowledge/base/document/vo/DocumentReviewVO.java`:

```java
package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Document review VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document review information")
public class DocumentReviewVO {

    @Schema(description = "Review record ID")
    private Long id;

    @Schema(description = "Document ID")
    private Long documentId;

    @Schema(description = "Document title")
    private String documentTitle;

    @Schema(description = "Reviewer ID")
    private Long reviewerId;

    @Schema(description = "Reviewer name")
    private String reviewerName;

    @Schema(description = "Review result: 1-approved, 2-rejected")
    private Integer reviewResult;

    @Schema(description = "Review comment")
    private String reviewComment;

    @Schema(description = "Status before review")
    private Integer beforeStatus;

    @Schema(description = "Review time")
    private LocalDateTime reviewedAt;

    @Schema(description = "Review round")
    private Integer reviewRound;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
```

**Design points:**
- Uses the `@Builder` pattern to support flexible construction
- The `documentTitle` field is denormalized to avoid join queries
- Includes complete review information for display

---

## VI. Mapper Layer Design

### 6.1 Create the DocumentReviewMapper Interface

Create `kb-document/src/main/java/com/knowledge/base/document/mapper/DocumentReviewMapper.java`:

```java
package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.entity.DocumentReview;
import org.apache.ibatis.annotations.Mapper;

/**
 * Document review Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface DocumentReviewMapper extends BaseMapper<DocumentReview> {

}
```

**Design points:**
- Extends `BaseMapper<DocumentReview>` to get the basic CRUD methods
- Marked as a MyBatis Mapper interface with the `@Mapper` annotation
- Defines several query methods that support review record queries and statistics

### 6.2 Create DocumentReviewMapper.xml

Create `kb-document/src/main/resources/mapper/DocumentReviewMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.knowledge.base.document.mapper.DocumentReviewMapper">

    <!-- Common query result mapping -->
    <resultMap id="BaseResultMap" type="com.knowledge.base.document.entity.DocumentReview">
        <id column="id" property="id" />
        <result column="document_id" property="documentId" />
        <result column="reviewer_id" property="reviewerId" />
        <result column="reviewer_name" property="reviewerName" />
        <result column="review_result" property="reviewResult" />
        <result column="review_comment" property="reviewComment" />
        <result column="before_status" property="beforeStatus" />
        <result column="reviewed_at" property="reviewedAt" />
        <result column="review_round" property="reviewRound" />
        <result column="created_at" property="createdAt" />
    </resultMap>

    <!-- Common query result columns -->
    <sql id="Base_Column_List">
        id, document_id, reviewer_id, reviewer_name, review_result, review_comment, before_status,
        reviewed_at, review_round, created_at
    </sql>

    <!-- Query the review record list by document ID -->
    <select id="selectByDocumentId" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List" />
        FROM tb_document_review
        WHERE document_id = #{documentId}
        ORDER BY review_round ASC, created_at DESC
    </select>

    <!-- Query the review record list by reviewer ID -->
    <select id="selectByReviewerId" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List" />
        FROM tb_document_review
        WHERE reviewer_id = #{reviewerId}
        ORDER BY created_at DESC
    </select>

    <!-- Query the review record by document ID and review round -->
    <select id="selectByDocumentIdAndRound" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List" />
        FROM tb_document_review
        WHERE document_id = #{documentId}
        AND review_round = #{reviewRound}
        ORDER BY created_at DESC
    </select>

    <!-- Query the latest review record for a document -->
    <select id="selectLatestByDocumentId" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List" />
        FROM tb_document_review
        WHERE document_id = #{documentId}
        ORDER BY review_round DESC, created_at DESC
        LIMIT 1
    </select>

    <!-- Count the review rounds for a document -->
    <select id="countRoundsByDocumentId" resultType="java.lang.Integer">
        SELECT COALESCE(MAX(review_round), 0)
        FROM tb_document_review
        WHERE document_id = #{documentId}
    </select>

    <!-- Count the number of reviews by a reviewer -->
    <select id="countByReviewerId" resultType="java.lang.Long">
        SELECT COUNT(*)
        FROM tb_document_review
        WHERE reviewer_id = #{reviewerId}
    </select>

    <!-- Query the list of documents pending review (related to the reviewer) -->
    <select id="selectPendingReviews" resultMap="BaseResultMap">
        SELECT DISTINCT dr.*
        FROM tb_document_review dr
        INNER JOIN kb_document d ON dr.document_id = d.id
        WHERE d.status = 3
        AND dr.reviewer_id = #{reviewerId}
        AND dr.review_result IS NULL
        ORDER BY dr.created_at ASC
    </select>

</mapper>
```

**Mapper method notes:**

| Method | Description | Use Case |
|------|------|---------|
| selectByDocumentId | Query all review records for a document | Displaying review history |
| selectByReviewerId | Query review records by reviewer | Reviewer workload statistics |
| selectByDocumentIdAndRound | Query the review record for a document's specified round | Viewing review details |
| selectLatestByDocumentId | Query the latest review record for a document | Current review status |
| countRoundsByDocumentId | Count the review rounds for a document | Computing the next round |
| countByReviewerId | Count the number of reviews by a reviewer | Reviewer workload statistics |
| selectPendingReviews | Query the list of documents pending review | Reviewer's workbench |

---

## VII. Service Layer Design

### 7.1 Create the DocumentReviewService Interface

Create `kb-document/src/main/java/com/knowledge/base/document/service/DocumentReviewService.java`:

```java
package com.knowledge.base.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.document.dto.DocumentReviewDTO;
import com.knowledge.base.document.dto.ReviewQueryDTO;
import com.knowledge.base.document.entity.DocumentReview;
import com.knowledge.base.document.vo.DocumentReviewVO;

/**
 * Document review Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface DocumentReviewService extends IService<DocumentReview> {

    /**
     * Submit for review
     *
     * @param documentId the document ID
     * @return whether it succeeded
     */
    Boolean submitForReview(Long documentId);

    /**
     * Approve a review
     *
     * @param dto the review DTO
     * @return whether it succeeded
     */
    Boolean approveReview(DocumentReviewDTO dto);

    /**
     * Reject a review
     *
     * @param dto the review DTO
     * @return whether it succeeded
     */
    Boolean rejectReview(DocumentReviewDTO dto);

    /**
     * Get the list of documents pending review
     *
     * @param dto the query DTO
     * @return the paginated result
     */
    PageResult<DocumentReviewVO> getPendingReviews(ReviewQueryDTO dto);

    /**
     * Get a document's review history
     *
     * @param documentId the document ID
     * @return the review history list
     */
    java.util.List<DocumentReviewVO> getDocumentReviewHistory(Long documentId);
}
```

### 7.2 Create the DocumentReviewServiceImpl Implementation Class

Create `kb-document/src/main/java/com/knowledge/base/document/service/impl/DocumentReviewServiceImpl.java`:

```java
package com.knowledge.base.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.document.dto.DocumentReviewDTO;
import com.knowledge.base.document.dto.ReviewQueryDTO;
import com.knowledge.base.document.entity.Document;
import com.knowledge.base.document.entity.DocumentReview;
import com.knowledge.base.document.mapper.DocumentMapper;
import com.knowledge.base.document.mapper.DocumentReviewMapper;
import com.knowledge.base.document.service.DocumentReviewService;
import com.knowledge.base.document.vo.DocumentReviewVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Document review Service implementation class
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; implements the
 * business logic related to document review</p>
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean submitForReview(Long documentId) {
        log.info("Submit document for review: documentId={}", documentId);

        if (documentId == null) {
            throw new BusinessException("Document ID must not be empty");
        }

        // Check whether the document exists
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException("Document does not exist");
        }

        // Check the document status
        if (document.getStatus() != 0) {
            throw new BusinessException("Only documents in draft status can be submitted for review");
        }

        // Get the current review round
        Integer currentRound = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(review_round), 0) FROM tb_document_review WHERE document_id = ?",
                Integer.class,
                documentId
        );

        // Create the review record
        DocumentReview review = new DocumentReview();
        review.setId(SnowflakeIdGenerator.getInstance().nextId());
        review.setDocumentId(documentId);
        review.setReviewerId(null); // to be assigned
        review.setReviewerName(null);
        review.setReviewResult(null);
        review.setReviewComment(null);
        review.setBeforeStatus(document.getStatus());
        review.setReviewedAt(null);
        review.setReviewRound((currentRound != null ? currentRound : 0) + 1);
        review.setCreatedAt(LocalDateTime.now());

        int count = documentReviewMapper.insert(review);
        if (count <= 0) {
            throw new BusinessException("Failed to submit for review");
        }

        // Update the document status to pending review
        document.setStatus(3); // 3-pending review
        documentMapper.updateById(document);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean approveReview(DocumentReviewDTO dto) {
        log.info("Approve review: reviewId={}", dto.getReviewId());

        if (dto.getReviewId() == null) {
            throw new BusinessException("Review record ID must not be empty");
        }

        // Check whether the review record exists
        DocumentReview review = documentReviewMapper.selectById(dto.getReviewId());
        if (review == null) {
            throw new BusinessException("Review record does not exist");
        }

        if (review.getReviewResult() != null) {
            throw new BusinessException("This record has already been reviewed");
        }

        // TODO: get the current reviewer information from the context
        Long reviewerId = 1L;
        String reviewerName = "Reviewer";

        // Update the review record
        review.setReviewerId(reviewerId);
        review.setReviewerName(reviewerName);
        review.setReviewResult(1); // 1-approved
        review.setReviewComment(dto.getReviewComment());
        review.setReviewedAt(LocalDateTime.now());
        documentReviewMapper.updateById(review);

        // Update the document status to published
        Document document = documentMapper.selectById(review.getDocumentId());
        if (document != null) {
            document.setStatus(1); // 1-published
            documentMapper.updateById(document);
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean rejectReview(DocumentReviewDTO dto) {
        log.info("Reject review: reviewId={}", dto.getReviewId());

        if (dto.getReviewId() == null) {
            throw new BusinessException("Review record ID must not be empty");
        }

        if (!StringUtils.hasText(dto.getReviewComment())) {
            throw new BusinessException("A rejection comment must not be empty");
        }

        // Check whether the review record exists
        DocumentReview review = documentReviewMapper.selectById(dto.getReviewId());
        if (review == null) {
            throw new BusinessException("Review record does not exist");
        }

        if (review.getReviewResult() != null) {
            throw new BusinessException("This record has already been reviewed");
        }

        // TODO: get the current reviewer information from the context
        Long reviewerId = 1L;
        String reviewerName = "Reviewer";

        // Update the review record
        review.setReviewerId(reviewerId);
        review.setReviewerName(reviewerName);
        review.setReviewResult(2); // 2-rejected
        review.setReviewComment(dto.getReviewComment());
        review.setReviewedAt(LocalDateTime.now());
        documentReviewMapper.updateById(review);

        // Update the document status to draft
        Document document = documentMapper.selectById(review.getDocumentId());
        if (document != null) {
            document.setStatus(0); // 0-draft
            documentMapper.updateById(document);
        }

        return true;
    }

    @Override
    public PageResult<DocumentReviewVO> getPendingReviews(ReviewQueryDTO dto) {
        // Build the query conditions
        LambdaQueryWrapper<DocumentReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(DocumentReview::getReviewResult); // unreviewed records

        if (dto.getReviewerId() != null) {
            wrapper.eq(DocumentReview::getReviewerId, dto.getReviewerId());
        }

        // Keyword search
        if (StringUtils.hasText(dto.getKeyword())) {
            // Subquery on the document title
            wrapper.exists(
                    "SELECT 1 FROM kb_document d WHERE d.id = tb_document_review.document_id AND d.title LIKE CONCAT('%', {0}, '%')",
                    dto.getKeyword()
            );
        }

        // Sorting
        wrapper.orderByDesc(DocumentReview::getCreatedAt);

        // Paginated query
        Page<DocumentReview> page = new Page<>(dto.getCurrent(), dto.getSize());
        IPage<DocumentReview> reviewPage = documentReviewMapper.selectPage(page, wrapper);

        // Convert to VO
        IPage<DocumentReviewVO> voPage = reviewPage.convert(review -> {
            // Get the document title
            Document document = documentMapper.selectById(review.getDocumentId());
            String documentTitle = document != null ? document.getTitle() : "";

            return DocumentReviewVO.builder()
                    .id(review.getId())
                    .documentId(review.getDocumentId())
                    .documentTitle(documentTitle)
                    .reviewerId(review.getReviewerId())
                    .reviewerName(review.getReviewerName())
                    .reviewResult(review.getReviewResult())
                    .reviewComment(review.getReviewComment())
                    .beforeStatus(review.getBeforeStatus())
                    .reviewedAt(review.getReviewedAt())
                    .reviewRound(review.getReviewRound())
                    .createdAt(review.getCreatedAt())
                    .build();
        });

        return PageResult.<DocumentReviewVO>builder()
                .records(voPage.getRecords())
                .total(voPage.getTotal())
                .current(voPage.getCurrent())
                .size(voPage.getSize())
                .build();
    }

    @Override
    public List<DocumentReviewVO> getDocumentReviewHistory(Long documentId) {
        if (documentId == null) {
            throw new BusinessException("Document ID must not be empty");
        }

        List<DocumentReview> reviews = documentReviewMapper.selectList(
                new LambdaQueryWrapper<DocumentReview>()
                        .eq(DocumentReview::getDocumentId, documentId)
                        .orderByDesc(DocumentReview::getReviewRound)
        );

        return reviews.stream()
                .map(review -> {
                    // Get the document title
                    Document document = documentMapper.selectById(review.getDocumentId());
                    String documentTitle = document != null ? document.getTitle() : "";

                    return DocumentReviewVO.builder()
                            .id(review.getId())
                            .documentId(review.getDocumentId())
                            .documentTitle(documentTitle)
                            .reviewerId(review.getReviewerId())
                            .reviewerName(review.getReviewerName())
                            .reviewResult(review.getReviewResult())
                            .reviewComment(review.getReviewComment())
                            .beforeStatus(review.getBeforeStatus())
                            .reviewedAt(review.getReviewedAt())
                            .reviewRound(review.getReviewRound())
                            .createdAt(review.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
```

**Core implementation notes:**

1. **Submit-for-review logic:**
   - Verifies the document status must be draft (0)
   - Automatically computes the review round (max round + 1)
   - Creates the review record and updates the document status to pending review (3)

2. **Approve logic:**
   - Verifies the review record has not already been reviewed
   - Updates the review record (result=1, reviewer information)
   - Updates the document status to published (1)

3. **Reject logic:**
   - Requires a non-empty rejection comment
   - Updates the review record (result=2, reviewer information)
   - Updates the document status to draft (0)

4. **Pending review list query:**
   - Queries records where `reviewResult IS NULL`
   - Supports keyword search on the document title
   - Joins with document information

---

## VIII. Controller Layer Design

### 8.1 Create DocumentReviewController

Create `kb-document/src/main/java/com/knowledge/base/document/controller/DocumentReviewController.java`:

```java
package com.knowledge.base.document.controller;

import com.knowledge.base.common.annotation.OperationLog;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.dto.DocumentReviewDTO;
import com.knowledge.base.document.dto.ReviewQueryDTO;
import com.knowledge.base.document.service.DocumentReviewService;
import com.knowledge.base.document.vo.DocumentReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Document review Controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/document-reviews")
@RequiredArgsConstructor
@Tag(name = "Document Review", description = "Document review endpoints")
public class DocumentReviewController {

    private final DocumentReviewService reviewService;

    /**
     * Submit for review
     */
    @PostMapping("/submit/{documentId}")
    @Operation(summary = "Submit document for review", description = "Submit a document for review")
    @OperationLog(module = "Document Review", operation = "Submit for Review", description = "Submit a document for review")
    public Result<Boolean> submitForReview(@PathVariable Long documentId) {
        Boolean result = reviewService.submitForReview(documentId);
        return Result.success(result);
    }

    /**
     * Approve a review
     */
    @PostMapping("/approve")
    @Operation(summary = "Approve review", description = "Approve a document's review")
    @OperationLog(module = "Document Review", operation = "Approve Review", description = "Approve a document's review")
    public Result<Boolean> approveReview(@Valid @RequestBody DocumentReviewDTO dto) {
        Boolean result = reviewService.approveReview(dto);
        return Result.success(result);
    }

    /**
     * Reject a review
     */
    @PostMapping("/reject")
    @Operation(summary = "Reject review", description = "Reject a document's review")
    @OperationLog(module = "Document Review", operation = "Reject Review", description = "Reject a document's review")
    public Result<Boolean> rejectReview(@Valid @RequestBody DocumentReviewDTO dto) {
        Boolean result = reviewService.rejectReview(dto);
        return Result.success(result);
    }

    /**
     * Get the list of documents pending review
     */
    @PostMapping("/pending")
    @Operation(summary = "Get pending reviews", description = "Get the list of documents pending review")
    public Result<PageResult<DocumentReviewVO>> getPendingReviews(@RequestBody ReviewQueryDTO dto) {
        PageResult<DocumentReviewVO> pageResult = reviewService.getPendingReviews(dto);
        return Result.success(pageResult);
    }

    /**
     * Get a document's review history
     */
    @GetMapping("/history/{documentId}")
    @Operation(summary = "Get review history", description = "Get the review history for a document")
    public Result<List<DocumentReviewVO>> getDocumentReviewHistory(@PathVariable Long documentId) {
        List<DocumentReviewVO> history = reviewService.getDocumentReviewHistory(documentId);
        return Result.success(history);
    }
}
```

**Endpoint design notes:**

1. **Submit for review** - `POST /api/document-reviews/submit/{documentId}`
   - Submits a draft document for review
   - Automatically computes the review round

2. **Approve** - `POST /api/document-reviews/approve`
   - The document is automatically published once approved
   - An optional review comment can be provided

3. **Reject** - `POST /api/document-reviews/reject`
   - The document returns to draft status after rejection
   - A rejection comment is required

4. **Pending review list** - `POST /api/document-reviews/pending`
   - Queries the list of documents pending review
   - Supports pagination and keyword search

5. **Review history** - `GET /api/document-reviews/history/{documentId}`
   - View all review records for a document
   - Sorted by review round descending

---

## IX. Testing and Verification

### 9.1 Start the Application

```bash
# Start the kb-document service
cd kb-document
mvn spring-boot:run
```

### 9.2 Test Submitting for Review

```bash
curl -X POST 'http://localhost:8083/api/document-reviews/submit/1' \
  -H 'Content-Type: application/json'
```

**Example response:**
```json
{
  "code": 200,
  "message": "Operation succeeded",
  "data": true
}
```

### 9.3 Test Getting the Pending Review List

```bash
curl -X POST 'http://localhost:8083/api/document-reviews/pending' \
  -H 'Content-Type: application/json' \
  -d '{
    "current": 1,
    "size": 10,
    "keyword": "technical"
  }'
```

**Example response:**
```json
{
  "code": 200,
  "message": "Operation succeeded",
  "data": {
    "records": [
      {
        "id": 1,
        "documentId": 1,
        "documentTitle": "Spring Boot Tutorial",
        "reviewerId": null,
        "reviewerName": null,
        "reviewResult": null,
        "reviewComment": null,
        "beforeStatus": 0,
        "reviewedAt": null,
        "reviewRound": 1,
        "createdAt": "2024-01-15T10:30:00"
      }
    ],
    "total": 1,
    "current": 1,
    "size": 10
  }
}
```

### 9.4 Test Approving a Review

```bash
curl -X POST 'http://localhost:8083/api/document-reviews/approve' \
  -H 'Content-Type: application/json' \
  -d '{
    "reviewId": 1,
    "reviewResult": 1,
    "reviewComment": "Content is complete, approved"
  }'
```

### 9.5 Test Rejecting a Review

```bash
curl -X POST 'http://localhost:8083/api/document-reviews/reject' \
  -H 'Content-Type: application/json' \
  -d '{
    "reviewId": 2,
    "reviewResult": 2,
    "reviewComment": "Needs more examples"
  }'
```

### 9.6 Test Querying Review History

```bash
curl -X GET 'http://localhost:8083/api/document-reviews/history/1'
```

**Example response:**
```json
{
  "code": 200,
  "message": "Operation succeeded",
  "data": [
    {
      "id": 1,
      "documentId": 1,
      "documentTitle": "Spring Boot Tutorial",
      "reviewerId": 1,
      "reviewerName": "Reviewer",
      "reviewResult": 2,
      "reviewComment": "Needs more examples",
      "beforeStatus": 0,
      "reviewedAt": "2024-01-15T11:00:00",
      "reviewRound": 1,
      "createdAt": "2024-01-15T10:30:00"
    },
    {
      "id": 2,
      "documentId": 1,
      "documentTitle": "Spring Boot Tutorial",
      "reviewerId": 1,
      "reviewerName": "Reviewer",
      "reviewResult": 1,
      "reviewComment": "Revised, approved",
      "beforeStatus": 0,
      "reviewedAt": "2024-01-15T14:00:00",
      "reviewRound": 2,
      "createdAt": "2024-01-15T13:00:00"
    }
  ]
}
```

### 9.7 Verify the Knife4j Docs

Visit: `http://localhost:8083/doc.html`

View the API docs and try out the document review endpoints online.

---

## X. Advanced Features

### 10.1 Multi-Level Review Workflow

The current implementation is single-level review. To support multi-level review (initial review, second review, final review):

**Implementation options:**

1. **Add a review level field:**
   ```sql
   ALTER TABLE tb_document_review ADD COLUMN `review_level` TINYINT DEFAULT 1 COMMENT 'Review level: 1-initial, 2-second, 3-final';
   ```

2. **Modify the approval logic:**
   ```java
   // After approval, determine whether the next level of review is needed
   if (review.getReviewLevel() < 3) {
       // Create the next-level review record
       createNextLevelReview(review);
   } else {
       // Final review approved; publish the document
       publishDocument(review.getDocumentId());
   }
   ```

3. **Configure the review levels:**
   ```yaml
   review:
     levels: 3
     level-1-name: "Initial Review"
     level-2-name: "Second Review"
     level-3-name: "Final Review"
   ```

### 10.2 Review Permission Control

Implement role-based review permissions:

**Permission table design:**
```sql
CREATE TABLE `tb_review_permission` (
    `id` BIGINT PRIMARY KEY,
    `role_id` BIGINT NOT NULL COMMENT 'Role ID',
    `review_level` TINYINT NOT NULL COMMENT 'Reviewable level',
    `category_id` BIGINT COMMENT 'Category ID (NULL means all categories)'
);
```

**Permission validation logic:**
```java
private void checkReviewPermission(Long userId, Long documentId) {
    // Get the user's roles
    List<Long> roleIds = getUserRoleIds(userId);

    // Get the document's category
    Document document = documentMapper.selectById(documentId);

    // Check whether review permission is granted
    boolean hasPermission = checkPermission(roleIds, document.getCategoryId());
    if (!hasPermission) {
        throw new BusinessException("No review permission");
    }
}
```

### 10.3 Review Notifications

Integrate a notification feature:

**Notification triggers:**
1. Notify the reviewer when a document is submitted for review
2. Notify the submitter when the review is approved
3. Notify the submitter when the review is rejected

**Implementation:**
```java
@Override
public Boolean submitForReview(Long documentId) {
    // ... submit-for-review logic ...

    // Send a notification
    notificationService.notifyReviewers(documentId, review.getId());

    return true;
}

@Override
public Boolean approveReview(DocumentReviewDTO dto) {
    // ... approval logic ...

    // Send a notification
    notificationService.notifySubmitter(review.getDocumentId(), "Review approved");

    return true;
}
```

### 10.4 Review Statistics Feature

Implement review data statistics:

**Statistical dimensions:**
- Number of pending reviews
- Number of reviews completed today
- Review approval rate
- Average review duration

**Implementation:**
```java
public ReviewStatisticsVO getReviewStatistics(Long reviewerId) {
    // Number of pending reviews
    Long pendingCount = getPendingCount(reviewerId);

    // Number of reviews completed today
    Long todayCount = getTodayReviewCount(reviewerId);

    // Review approval rate
    Double approveRate = getApproveRate(reviewerId);

    // Average review duration
    Double avgDuration = getAvgReviewDuration(reviewerId);

    return ReviewStatisticsVO.builder()
            .pendingCount(pendingCount)
            .todayCount(todayCount)
            .approveRate(approveRate)
            .avgDuration(avgDuration)
            .build();
}
```

---

## XI. Performance Optimization Recommendations

### 11.1 Query Optimization

1. **Cache the pending review list:**
   ```java
   @Cacheable(value = "pending_reviews", key = "#dto.current + '_' + #dto.size")
   public PageResult<DocumentReviewVO> getPendingReviews(ReviewQueryDTO dto) {
       // ...
   }
   ```

2. **Cache review history:**
   ```java
   @Cacheable(value = "review_history", key = "#documentId")
   public List<DocumentReviewVO> getDocumentReviewHistory(Long documentId) {
       // ...
   }
   ```

3. **Index optimization:**
   ```sql
   -- Composite index to optimize the query
   CREATE INDEX idx_review_result_created ON tb_document_review(review_result, created_at);
   ```

### 11.2 Concurrency Control

1. **Prevent duplicate submissions:**
   ```java
   // Use a distributed lock
   @Lock(key = "document:review:submit:#documentId")
   public Boolean submitForReview(Long documentId) {
       // Check whether a pending review record already exists
       // ...
   }
   ```

2. **Prevent duplicate reviews:**
   ```java
   // Use optimistic locking
   @Version
   private Integer version;

   // Check the version when updating
   documentReviewMapper.updateById(review);
   ```

---

## XII. Frequently Asked Questions

### Q1: How do you support a review withdrawal feature?

**A:** Implement a withdrawal feature:

1. **Add a withdrawal endpoint:**
   ```java
   @Transactional
   public Boolean withdrawReview(Long reviewId) {
       DocumentReview review = documentReviewMapper.selectById(reviewId);

       // Check the status (only pending-review records can be withdrawn)
       if (review.getReviewResult() != null) {
           throw new BusinessException("A record that has already been reviewed cannot be withdrawn");
       }

       // Delete the review record
       documentReviewMapper.deleteById(reviewId);

       // Restore the document status
       Document document = documentMapper.selectById(review.getDocumentId());
       document.setStatus(review.getBeforeStatus());
       documentMapper.updateById(document);

       return true;
   }
   ```

### Q2: How do you implement an expedited review feature?

**A:** Implement expedited review:

1. **Add an urgent flag field:**
   ```sql
   ALTER TABLE tb_document_review ADD COLUMN `is_urgent` TINYINT DEFAULT 0 COMMENT 'Whether urgent';
   ```

2. **Choose urgent when submitting:**
   ```java
   public Boolean submitForReview(Long documentId, Boolean isUrgent) {
       // ...
       review.setIsUrgent(isUrgent ? 1 : 0);
       // ...
   }
   ```

3. **Prioritize urgent items when querying:**
   ```java
   wrapper.orderByDesc(DocumentReview::getIsUrgent)
          .orderByAsc(DocumentReview::getCreatedAt);
   ```

### Q3: How do you handle review timeouts?

**A:** Implement timeout handling:

1. **Add a timeout configuration:**
   ```yaml
   review:
     timeout-hours: 48
   ```

2. **A scheduled task scans for timed-out records:**
   ```java
   @Scheduled(cron = "0 0 * * * ?")
   public void handleTimeoutReviews() {
       // Query timed-out pending review records
       List<DocumentReview> timeoutReviews = findTimeoutReviews();

       // Handle automatically (escalate or notify)
       for (DocumentReview review : timeoutReviews) {
           escalateReview(review);
       }
   }
   ```

### Q4: How do you implement review delegation?

**A:** Implement a review delegation feature:

1. **Add a delegation record table:**
   ```sql
   CREATE TABLE `tb_review_delegation` (
       `id` BIGINT PRIMARY KEY,
       `review_id` BIGINT NOT NULL COMMENT 'Review record ID',
       `from_reviewer_id` BIGINT NOT NULL COMMENT 'Delegating reviewer ID',
       `to_reviewer_id` BIGINT NOT NULL COMMENT 'Delegated reviewer ID',
       `reason` VARCHAR(500) COMMENT 'Delegation reason',
       `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
   );
   ```

2. **Delegation endpoint:**
   ```java
   @Transactional
   public Boolean delegateReview(Long reviewId, Long toReviewerId, String reason) {
       // Create the delegation record
       // Update the reviewer on the review record
       // Send a notification

       return true;
   }
   ```

### Q5: How do you support batch review?

**A:** Implement batch review:

1. **Batch review endpoint:**
   ```java
   @Transactional
   public Boolean batchApprove(List<Long> reviewIds, String comment) {
       for (Long reviewId : reviewIds) {
           approveReview(DocumentReviewDTO.builder()
                   .reviewId(reviewId)
                   .reviewResult(1)
                   .reviewComment(comment)
                   .build());
       }
       return true;
   }
   ```

2. **Controller endpoint:**
   ```java
   @PostMapping("/batch-approve")
   public Result<Boolean> batchApprove(@RequestBody BatchReviewDTO dto) {
       return Result.success(reviewService.batchApprove(dto.getReviewIds(), dto.getComment()));
   }
   ```

---

## XIII. Summary

This article described in detail the complete implementation of the review-related features in the kb-document service, including:

### Core Features
- ✅ Submit for review (draft → pending review)
- ✅ Approve (pending review → published)
- ✅ Reject (pending review → draft)
- ✅ Pending review list query
- ✅ Review history query

### Technical Highlights
1. **Review round mechanism**: supports multiple review rounds, automatically computing the round number
2. **State machine design**: a clear document status transition logic
3. **Transaction guarantee**: review records and document status are updated synchronously
4. **Historical traceability**: complete review history records
5. **Flexible querying**: supports pagination and keyword search

### Review Workflow Diagram

```
Edit document → Draft (0) → Submit for review → Pending review (3)
                                   ↓
                            Reviewer reviews
                                   ↓
                          ┌─────────┴─────────┐
                          ↓                   ↓
                     Approved (1)          Rejected (2)
                          ↓                   ↓
                       Published (1)        Draft (0)
                          ↑                   │
                          └───────────────────┘
                        Revise and resubmit
```

### Future Extensions
- Multi-level review workflow
- Review permission control
- Review notifications
- Review data statistics
- Review timeout handling
- Review delegation feature

Through this tutorial, readers can master the complete design and implementation of an enterprise-grade review system, providing a reference for real-world project development.
