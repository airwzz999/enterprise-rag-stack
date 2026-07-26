# Adding the Comment Feature

## I. Overview

The document comment feature is an important tool for the knowledge base system to enhance user interaction. Through the comment system, users can discuss, ask questions about, and give feedback on documents. This article describes in detail how to implement a complete document comment feature in the kb-document service.

### 1.1 Feature Positioning

The comment system, as a core feature for user interaction, provides the following core capabilities:

| Feature Module | Description | Use Case |
|---------|------|---------|
| Comment creation | Post a comment on a document or reply to another comment | User feedback, discussing questions |
| Comment deletion | Delete your own comment | Retracting inappropriate remarks |
| Like/unlike | Like a comment to show agreement | Content quality feedback |
| Comment list | Paginated view of all comments on a document | Browsing comments |
| Comment replies | Second-level replies to comments | In-depth discussion |
| Comment sorting | Supports sorting by time or like count | Displaying popular comments |

### 1.2 Comment System Design Points

**Hierarchical structure design:**
- Supports a two-level comment structure: root comment + child comment (reply)
- A maximum of two levels is supported, to avoid overly deep nesting hurting the experience
- Hierarchy is linked via the `parentId` and `rootId` fields

**Like system design:**
- A standalone `tb_like` table supports multiple target types
- `targetType` distinguishes between document likes and comment likes
- A unique index design prevents duplicate likes

**Counting strategy:**
- The comment table denormalizes `likeCount` and `replyCount`
- Atomic database operations ensure count accuracy
- Avoids frequent join queries that would hurt performance

### 1.3 Data Model

```
Document
    ├── Root comment 1 (Comment, parentId=0)
    │   ├── Reply 1 (Comment, parentId=root comment 1.id, rootId=root comment 1.id)
    │   └── Reply 2 (Comment, parentId=root comment 1.id, rootId=root comment 1.id)
    ├── Root comment 2 (Comment, parentId=0)
    └── ...
```

Like table design:
```
tb_like
    ├── Document like (targetType=1)
    └── Comment like (targetType=2)
```

---

## II. Database Design

### 2.1 Comment Table Structure

```sql
-- Comment table
DROP TABLE IF EXISTS `tb_comment`;
CREATE TABLE `tb_comment` (
    `id` BIGINT(20) PRIMARY KEY COMMENT 'Comment ID (Snowflake ID)',
    `document_id` BIGINT(20) NOT NULL COMMENT 'Document ID',
    `parent_id` BIGINT(20) COMMENT 'Parent comment ID',
    `root_id` BIGINT(20) COMMENT 'Root comment ID',

    `content` TEXT NOT NULL COMMENT 'Comment content',

    `commenter_id` BIGINT(20) NOT NULL COMMENT 'Commenter ID',
    `commenter_name` VARCHAR(50) COMMENT 'Commenter name',
    `commenter_avatar` VARCHAR(500) COMMENT 'Commenter avatar',

    `reply_to_user_id` BIGINT(20) COMMENT 'Who this replies to (user ID)',
    `reply_to_user_name` VARCHAR(50) COMMENT 'Who this replies to (user name)',

    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-hidden, 1-normal',

    `like_count` INT DEFAULT 0 COMMENT 'Like count',
    `reply_count` INT DEFAULT 0 COMMENT 'Reply count',

    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Delete flag',

    KEY `idx_document_id` (`document_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_root_id` (`root_id`),
    KEY `idx_commenter_id` (`commenter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Comment table';
```

### 2.2 Like Table Structure

```sql
-- Like table
DROP TABLE IF EXISTS `tb_like`;
CREATE TABLE `tb_like` (
    `id` BIGINT(20) PRIMARY KEY COMMENT 'Like ID',
    `target_id` BIGINT(20) NOT NULL COMMENT 'Target ID (document or comment)',
    `target_type` TINYINT NOT NULL COMMENT 'Target type: 1-document, 2-comment',
    `user_id` BIGINT(20) NOT NULL COMMENT 'User ID',

    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',

    UNIQUE KEY `uk_target_user_type` (`target_id`, `user_id`, `target_type`),
    KEY `idx_target_id` (`target_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Like table';
```

### 2.3 Field Descriptions

**Comment table (tb_comment) field descriptions:**

| Field | Type | Description | Required | Default |
|------|------|------|------|--------|
| id | BIGINT | Comment ID (Snowflake ID) | Yes | Auto-generated |
| document_id | BIGINT | Document ID | Yes | - |
| parent_id | BIGINT | Parent comment ID (0 means root comment) | No | 0 |
| root_id | BIGINT | Root comment ID | No | - |
| content | TEXT | Comment content | Yes | - |
| commenter_id | BIGINT | Commenter ID | Yes | - |
| commenter_name | VARCHAR(50) | Commenter name (denormalized) | No | - |
| commenter_avatar | VARCHAR(500) | Commenter avatar (denormalized) | No | - |
| reply_to_user_id | BIGINT | User ID this replies to | No | - |
| reply_to_user_name | VARCHAR(50) | User name this replies to (denormalized) | No | - |
| status | TINYINT | Status (0-hidden, 1-normal) | Yes | 1 |
| like_count | INT | Like count (denormalized) | Yes | 0 |
| reply_count | INT | Reply count (denormalized) | Yes | 0 |
| created_at | DATETIME | Creation time | Yes | CURRENT_TIMESTAMP |
| updated_at | DATETIME | Update time | Yes | Automatically updated |
| deleted | TINYINT | Delete flag | Yes | 0 |

**Like table (tb_like) field descriptions:**

| Field | Type | Description | Required | Default |
|------|------|------|------|--------|
| id | BIGINT | Like ID | Yes | Auto-generated |
| target_id | BIGINT | Target ID (document ID or comment ID) | Yes | - |
| target_type | TINYINT | Target type (1-document, 2-comment) | Yes | - |
| user_id | BIGINT | User ID | Yes | - |
| created_at | DATETIME | Creation time | Yes | CURRENT_TIMESTAMP |

### 2.4 Index Design

**Comment table indexes:**
- **Primary key index**: `id` - uniquely identifies the primary key
- **Regular index**: `idx_document_id` - speeds up querying comments by document
- **Regular index**: `idx_parent_id` - speeds up querying child comments
- **Regular index**: `idx_root_id` - speeds up querying the comment tree
- **Regular index**: `idx_commenter_id` - speeds up querying comments by user

**Like table indexes:**
- **Primary key index**: `id` - uniquely identifies the primary key
- **Unique index**: `uk_target_user_type` - prevents duplicate likes
- **Regular index**: `idx_target_id` - speeds up querying likes by target
- **Regular index**: `idx_user_id` - speeds up querying likes by user

---

## III. Entity Class Design

### 3.1 Create the Comment Entity Class

Create `kb-document/src/main/java/com/knowledge/base/document/entity/Comment.java`:

```java
package com.knowledge.base.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Comment entity
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_comment")
@Schema(description = "Comment entity")
public class Comment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Comment ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "Comment ID")
    private Long id;

    /**
     * Document ID
     */
    @Schema(description = "Document ID")
    private Long documentId;

    /**
     * Parent comment ID
     */
    @Schema(description = "Parent comment ID")
    private Long parentId;

    /**
     * Root comment ID
     */
    @Schema(description = "Root comment ID")
    private Long rootId;

    /**
     * Comment content
     */
    @Schema(description = "Comment content")
    private String content;

    /**
     * Commenter ID
     */
    @Schema(description = "Commenter ID")
    private Long commenterId;

    /**
     * Commenter name
     */
    @Schema(description = "Commenter name")
    private String commenterName;

    /**
     * Commenter avatar
     */
    @Schema(description = "Commenter avatar")
    private String commenterAvatar;

    /**
     * Who this replies to (user ID)
     */
    @Schema(description = "Who this replies to")
    private Long replyToUserId;

    /**
     * Who this replies to (user name)
     */
    @Schema(description = "Who this replies to")
    private String replyToUserName;

    /**
     * Status: 0-hidden, 1-normal
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Like count
     */
    @Schema(description = "Like count")
    private Integer likeCount;

    /**
     * Reply count
     */
    @Schema(description = "Reply count")
    private Integer replyCount;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    @Schema(description = "Update time")
    private LocalDateTime updatedAt;

    /**
     * Delete flag
     */
    @TableLogic
    @Schema(description = "Delete flag")
    private Integer deleted;
}
```

**Design points:**
- Extends `BaseEntity` to get common fields
- Uses `@TableId(type = IdType.ASSIGN_ID)` to configure the Snowflake ID generation strategy
- Uses `@TableLogic` to configure logical deletion
- Denormalizes user information (name, avatar) to avoid join queries
- Denormalizes count information (like count, reply count) to improve query performance

---

## IV. DTO Design

### 4.1 Create the Comment DTO

Create `kb-document/src/main/java/com/knowledge/base/document/dto/CommentCreateDTO.java`:

```java
package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Comment creation DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Comment creation request")
public class CommentCreateDTO {

    @NotNull(message = "Document ID must not be empty")
    @Schema(description = "Document ID")
    private Long documentId;

    @Schema(description = "Parent comment ID")
    private Long parentId;

    @NotBlank(message = "Comment content must not be empty")
    @Schema(description = "Comment content")
    private String content;

    @Schema(description = "Who this replies to (user ID)")
    private Long replyToUserId;
}
```

**Design points:**
- Uses `@NotNull` and `@NotBlank` for parameter validation
- `parentId` is optional; omitting it means a root comment is being created
- `replyToUserId` is optional, used to identify the reply target when replying

### 4.2 Comment Query DTO

Create `kb-document/src/main/java/com/knowledge/base/document/dto/CommentQueryDTO.java`:

```java
package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Comment query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Comment query request")
public class CommentQueryDTO {

    @Schema(description = "Current page")
    private Long current = 1L;

    @Schema(description = "Page size")
    private Long size = 10L;

    @Schema(description = "Sort field: like_count-like count, created_at-creation time")
    private String sortBy = "created_at";

    @Schema(description = "Sort direction: asc-ascending, desc-descending")
    private String sortOrder = "desc";
}
```

**Design points:**
- Supports pagination (current, size)
- Supports flexible sorting (sortBy, sortOrder)
- Sorted by creation time descending by default

---

## V. VO Design

### 5.1 Create the Comment VO

Create `kb-document/src/main/java/com/knowledge/base/document/vo/CommentVO.java`:

```java
package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Comment VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comment information")
public class CommentVO {

    @Schema(description = "Comment ID")
    private Long id;

    @Schema(description = "Document ID")
    private Long documentId;

    @Schema(description = "Parent comment ID")
    private Long parentId;

    @Schema(description = "Root comment ID")
    private Long rootId;

    @Schema(description = "Comment content")
    private String content;

    @Schema(description = "Commenter ID")
    private Long commenterId;

    @Schema(description = "Commenter name")
    private String commenterName;

    @Schema(description = "Commenter avatar")
    private String commenterAvatar;

    @Schema(description = "Who this replies to (user ID)")
    private Long replyToUserId;

    @Schema(description = "Who this replies to (user name)")
    private String replyToUserName;

    @Schema(description = "Status")
    private Integer status;

    @Schema(description = "Like count")
    private Integer likeCount;

    @Schema(description = "Reply count")
    private Integer replyCount;

    @Schema(description = "Whether already liked")
    private Boolean isLiked;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    @Schema(description = "List of child comments")
    private List<CommentVO> replies;
}
```

**Design points:**
- Uses the `@Builder` pattern to support flexible construction
- The `isLiked` field indicates whether the current user has already liked it
- The `replies` field supports displaying a tree structure
- Includes complete user information for frontend display

---

## VI. Mapper Layer Design

### 6.1 Create the CommentMapper Interface

Create `kb-document/src/main/java/com/knowledge/base/document/mapper/CommentMapper.java`:

```java
package com.knowledge.base.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.document.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * Comment Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

}
```

**Design points:**
- Extends `BaseMapper<Comment>` to get the basic CRUD methods
- Marked as a MyBatis Mapper interface with the `@Mapper` annotation
- Defines several query methods that support comment queries and statistics

### 6.2 Create CommentMapper.xml

Create `kb-document/src/main/resources/mapper/CommentMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.knowledge.base.document.mapper.CommentMapper">

    <!-- Common query result mapping -->
    <resultMap id="BaseResultMap" type="com.knowledge.base.document.entity.Comment">
        <id column="id" property="id" />
        <result column="document_id" property="documentId" />
        <result column="parent_id" property="parentId" />
        <result column="root_id" property="rootId" />
        <result column="content" property="content" />
        <result column="commenter_id" property="commenterId" />
        <result column="commenter_name" property="commenterName" />
        <result column="commenter_avatar" property="commenterAvatar" />
        <result column="reply_to_user_id" property="replyToUserId" />
        <result column="reply_to_user_name" property="replyToUserName" />
        <result column="status" property="status" />
        <result column="like_count" property="likeCount" />
        <result column="reply_count" property="replyCount" />
        <result column="created_at" property="createdAt" />
        <result column="updated_at" property="updatedAt" />
        <result column="deleted" property="deleted" />
        <result column="version" property="version" />
        <result column="create_time" property="createTime" />
        <result column="update_time" property="updateTime" />
        <result column="create_by" property="createBy" />
        <result column="update_by" property="updateBy" />
    </resultMap>

    <!-- Common query result columns -->
    <sql id="Base_Column_List">
        id, document_id, parent_id, root_id, content, commenter_id, commenter_name, commenter_avatar,
        reply_to_user_id, reply_to_user_name, status, like_count, reply_count, created_at, updated_at,
        deleted, version, create_time, update_time, create_by, update_by
    </sql>

    <!-- Query the comment list by document ID -->
    <select id="selectByDocumentId" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List" />
        FROM tb_comment
        WHERE document_id = #{documentId}
        AND parent_id IS NULL
        AND status = 1
        AND deleted = 0
        ORDER BY created_at DESC
    </select>

    <!-- Query the reply list by parent comment ID -->
    <select id="selectByParentId" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List" />
        FROM tb_comment
        WHERE parent_id = #{parentId}
        AND status = 1
        AND deleted = 0
        ORDER BY created_at ASC
    </select>

    <!-- Query the comment list by commenter ID -->
    <select id="selectByCommenterId" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List" />
        FROM tb_comment
        WHERE commenter_id = #{commenterId}
        AND deleted = 0
        ORDER BY created_at DESC
    </select>

    <!-- Increment the like count -->
    <update id="incrementLikeCount">
        UPDATE tb_comment
        SET like_count = like_count + 1
        WHERE id = #{commentId}
        AND deleted = 0
    </update>

    <!-- Decrement the like count -->
    <update id="decrementLikeCount">
        UPDATE tb_comment
        SET like_count = GREATEST(like_count - 1, 0)
        WHERE id = #{commentId}
        AND deleted = 0
    </update>

    <!-- Increment the reply count -->
    <update id="incrementReplyCount">
        UPDATE tb_comment
        SET reply_count = reply_count + 1
        WHERE id = #{commentId}
        AND deleted = 0
    </update>

    <!-- Update the comment status -->
    <update id="updateStatus">
        UPDATE tb_comment
        SET status = #{status}
        WHERE id = #{commentId}
        AND deleted = 0
    </update>

    <!-- Count the number of comments on a document -->
    <select id="countByDocumentId" resultType="java.lang.Long">
        SELECT COUNT(*)
        FROM tb_comment
        WHERE document_id = #{documentId}
        AND status = 1
        AND deleted = 0
    </select>

</mapper>
```

**Mapper method notes:**

| Method | Description | Use Case |
|------|------|---------|
| selectByDocumentId | Query the root comment list for a document | Displaying the comment list |
| selectByParentId | Query the reply list for a parent comment | Displaying child comments |
| selectByCommenterId | Query all comments by a user | User comment history |
| incrementLikeCount | Increment the like count | Liking |
| decrementLikeCount | Decrement the like count | Unliking |
| incrementReplyCount | Increment the reply count | New reply |
| updateStatus | Update the comment status | Comment moderation/hiding |
| countByDocumentId | Count comments on a document | Comment count statistics |

---

## VII. Service Layer Design

### 7.1 Create the CommentService Interface

Create `kb-document/src/main/java/com/knowledge/base/document/service/CommentService.java`:

```java
package com.knowledge.base.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.document.dto.CommentCreateDTO;
import com.knowledge.base.document.dto.CommentQueryDTO;
import com.knowledge.base.document.entity.Comment;
import com.knowledge.base.document.vo.CommentVO;

/**
 * Comment Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface CommentService extends IService<Comment> {

    /**
     * Create a comment
     *
     * @param dto the creation DTO
     * @return the comment ID
     */
    Long createComment(CommentCreateDTO dto);

    /**
     * Delete a comment
     *
     * @param commentId the comment ID
     * @return whether it succeeded
     */
    Boolean deleteComment(Long commentId);

    /**
     * Like a comment
     *
     * @param commentId the comment ID
     * @return whether it succeeded
     */
    Boolean likeComment(Long commentId);

    /**
     * Unlike a comment
     *
     * @param commentId the comment ID
     * @return whether it succeeded
     */
    Boolean unlikeComment(Long commentId);

    /**
     * Paginated query of document comments
     *
     * @param documentId the document ID
     * @param dto the query DTO
     * @return the paginated result
     */
    PageResult<CommentVO> pageDocumentComments(Long documentId, CommentQueryDTO dto);

    /**
     * Get the reply list for a comment
     *
     * @param parentCommentId the parent comment ID
     * @return the reply list
     */
    java.util.List<CommentVO> getCommentReplies(Long parentCommentId);
}
```

### 7.2 Create the CommentServiceImpl Implementation Class

Create `kb-document/src/main/java/com/knowledge/base/document/service/impl/CommentServiceImpl.java`:

```java
package com.knowledge.base.document.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.document.dto.CommentCreateDTO;
import com.knowledge.base.document.dto.CommentQueryDTO;
import com.knowledge.base.document.entity.Comment;
import com.knowledge.base.document.mapper.CommentMapper;
import com.knowledge.base.document.service.CommentService;
import com.knowledge.base.document.vo.CommentVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comment Service implementation class
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; implements the
 * business logic related to comments</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(CommentCreateDTO dto) {
        log.info("Create comment: documentId={}, parentId={}", dto.getDocumentId(), dto.getParentId());

        // Check whether the parent comment exists
        Long rootId = null;
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            Comment parentComment = commentMapper.selectById(dto.getParentId());
            if (parentComment == null) {
                throw new BusinessException("Parent comment does not exist");
            }
            if (!parentComment.getDocumentId().equals(dto.getDocumentId())) {
                throw new BusinessException("The parent comment does not belong to this document");
            }
            rootId = parentComment.getRootId() != null ? parentComment.getRootId() : parentComment.getId();

            // Update the reply count of the parent comment
            jdbcTemplate.update(
                    "UPDATE tb_comment SET reply_count = reply_count + 1 WHERE id = ?",
                    dto.getParentId()
            );
        }

        // TODO: get the current user information from the context
        Long userId = 1L;
        String userName = "Test User";
        String userAvatar = "/avatar/default.png";

        // Build the comment entity
        Comment comment = new Comment();
        comment.setId(SnowflakeIdGenerator.getInstance().nextId());
        comment.setDocumentId(dto.getDocumentId());
        comment.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        comment.setRootId(rootId);
        comment.setContent(dto.getContent());
        comment.setCommenterId(userId);
        comment.setCommenterName(userName);
        comment.setCommenterAvatar(userAvatar);
        comment.setReplyToUserId(dto.getReplyToUserId());
        comment.setStatus(1);
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        comment.setDeleted(0);

        // Save the comment
        int count = commentMapper.insert(comment);
        if (count <= 0) {
            throw new BusinessException("Failed to create comment");
        }

        // Update the document's comment count
        jdbcTemplate.update(
                "UPDATE kb_document SET comment_count = comment_count + 1 WHERE id = ?",
                dto.getDocumentId()
        );

        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteComment(Long commentId) {
        log.info("Delete comment: commentId={}", commentId);

        if (commentId == null) {
            throw new BusinessException("Comment ID must not be empty");
        }

        // Check whether the comment exists
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("Comment does not exist");
        }

        // TODO: check permissions; only the comment author or an admin can delete it

        // Check whether it has child comments
        Long childCount = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getParentId, commentId)
        );
        if (childCount > 0) {
            throw new BusinessException("This comment has replies and cannot be deleted");
        }

        // Delete the comment
        int count = commentMapper.deleteById(commentId);

        // Update the parent comment's reply count
        if (comment.getParentId() != null && comment.getParentId() > 0) {
            jdbcTemplate.update(
                    "UPDATE tb_comment SET reply_count = reply_count - 1 WHERE id = ?",
                    comment.getParentId()
            );
        }

        // Update the document's comment count
        if (count > 0) {
            jdbcTemplate.update(
                    "UPDATE kb_document SET comment_count = comment_count - 1 WHERE id = ?",
                    comment.getDocumentId()
            );
        }

        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean likeComment(Long commentId) {
        log.info("Like comment: commentId={}", commentId);

        if (commentId == null) {
            throw new BusinessException("Comment ID must not be empty");
        }

        // Check whether the comment exists
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("Comment does not exist");
        }

        // TODO: get the current user ID from the context
        Long userId = 1L;

        // Check whether it has already been liked
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_like WHERE target_id = ? AND user_id = ? AND target_type = 2",
                Integer.class,
                commentId, userId
        );

        if (count != null && count > 0) {
            throw new BusinessException("Already liked");
        }

        // Insert the like record
        jdbcTemplate.update(
                "INSERT INTO tb_like (id, target_id, user_id, target_type, created_at) VALUES (?, ?, ?, 2, NOW())",
                SnowflakeIdGenerator.getInstance().nextId(), commentId, userId
        );

        // Update the comment's like count
        jdbcTemplate.update(
                "UPDATE tb_comment SET like_count = like_count + 1 WHERE id = ?",
                commentId
        );

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unlikeComment(Long commentId) {
        log.info("Unlike comment: commentId={}", commentId);

        if (commentId == null) {
            throw new BusinessException("Comment ID must not be empty");
        }

        // Check whether the comment exists
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("Comment does not exist");
        }

        // TODO: get the current user ID from the context
        Long userId = 1L;

        // Delete the like record
        int count = jdbcTemplate.update(
                "DELETE FROM tb_like WHERE target_id = ? AND user_id = ? AND target_type = 2",
                commentId, userId
        );

        // Update the comment's like count
        if (count > 0) {
            jdbcTemplate.update(
                    "UPDATE tb_comment SET like_count = like_count - 1 WHERE id = ?",
                    commentId
            );
        }

        return count > 0;
    }

    @Override
    public PageResult<CommentVO> pageDocumentComments(Long documentId, CommentQueryDTO dto) {
        // Build the query conditions - only query root comments
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getDocumentId, documentId)
                .eq(Comment::getParentId, 0)
                .eq(Comment::getStatus, 1);

        // Sorting
        if (StringUtils.hasText(dto.getSortBy())) {
            if ("like_count".equals(dto.getSortBy())) {
                wrapper.orderByDesc(Comment::getLikeCount);
            } else {
                boolean isAsc = "asc".equals(dto.getSortOrder());
                if (isAsc) {
                    wrapper.orderByAsc(Comment::getCreatedAt);
                } else {
                    wrapper.orderByDesc(Comment::getCreatedAt);
                }
            }
        } else {
            wrapper.orderByDesc(Comment::getCreatedAt);
        }

        // Paginated query
        Page<Comment> page = new Page<>(dto.getCurrent(), dto.getSize());
        IPage<Comment> commentPage = commentMapper.selectPage(page, wrapper);

        // Convert to VO and load child comments
        IPage<CommentVO> voPage = commentPage.convert(comment -> {
            CommentVO vo = convertToVO(comment);
            // TODO: set whether it has been liked
            vo.setIsLiked(false);
            // Load child comments
            vo.setReplies(getCommentReplies(comment.getId()));
            return vo;
        });

        return PageResult.<CommentVO>builder()
                .records(voPage.getRecords())
                .total(voPage.getTotal())
                .current(voPage.getCurrent())
                .size(voPage.getSize())
                .build();
    }

    @Override
    public List<CommentVO> getCommentReplies(Long parentCommentId) {
        if (parentCommentId == null || parentCommentId <= 0) {
            return new ArrayList<>();
        }

        List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getParentId, parentCommentId)
                        .eq(Comment::getStatus, 1)
                        .orderByAsc(Comment::getCreatedAt)
        );

        return comments.stream()
                .map(comment -> {
                    CommentVO vo = convertToVO(comment);
                    // TODO: set whether it has been liked
                    vo.setIsLiked(false);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Convert to VO
     *
     * @param comment the comment entity
     * @return the comment VO
     */
    private CommentVO convertToVO(Comment comment) {
        return CommentVO.builder()
                .id(comment.getId())
                .documentId(comment.getDocumentId())
                .parentId(comment.getParentId())
                .rootId(comment.getRootId())
                .content(comment.getContent())
                .commenterId(comment.getCommenterId())
                .commenterName(comment.getCommenterName())
                .commenterAvatar(comment.getCommenterAvatar())
                .replyToUserId(comment.getReplyToUserId())
                .replyToUserName(comment.getReplyToUserName())
                .status(comment.getStatus())
                .likeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0)
                .replyCount(comment.getReplyCount() != null ? comment.getReplyCount() : 0)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
```

**Core implementation notes:**

1. **Comment creation logic:**
   - Verifies the existence and ownership of the parent comment
   - Automatically computes `rootId` (the parent comment's rootId, or the parent comment's own ID)
   - Uses an atomic database operation to update the reply count
   - Synchronously updates the document's total comment count

2. **Comment deletion logic:**
   - Checks whether it has child comments; if so, deletion is disallowed
   - Cascades an update to the parent comment's reply count
   - Cascades an update to the document's total comment count

3. **Like logic:**
   - First checks whether it has already been liked, to prevent duplicate likes
   - Inserts the like record
   - Uses an atomic operation to update the like count

4. **Query logic:**
   - Only queries root comments (parentId=0)
   - Supports sorting by like count or creation time
   - Automatically loads child comments to form a tree structure

---

## VIII. Controller Layer Design

### 8.1 Create CommentController

Create `kb-document/src/main/java/com/knowledge/base/document/controller/CommentController.java`:

```java
package com.knowledge.base.document.controller;

import com.knowledge.base.common.annotation.OperationLog;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.document.dto.CommentCreateDTO;
import com.knowledge.base.document.dto.CommentQueryDTO;
import com.knowledge.base.document.service.CommentService;
import com.knowledge.base.document.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Comment management Controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "Comment Management", description = "Comment management endpoints")
public class CommentController {

    private final CommentService commentService;

    /**
     * Create a comment
     */
    @PostMapping
    @Operation(summary = "Create comment", description = "Create a document comment")
    @OperationLog(module = "Comment Management", operation = "Create Comment", description = "Create a document comment")
    public Result<Long> createComment(@Valid @RequestBody CommentCreateDTO dto) {
        Long commentId = commentService.createComment(dto);
        return Result.success(commentId);
    }

    /**
     * Delete a comment
     */
    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete comment", description = "Delete the specified comment")
    @OperationLog(module = "Comment Management", operation = "Delete Comment", description = "Delete a comment")
    public Result<Boolean> deleteComment(@PathVariable Long commentId) {
        Boolean result = commentService.deleteComment(commentId);
        return Result.success(result);
    }

    /**
     * Like a comment
     */
    @PostMapping("/{commentId}/like")
    @Operation(summary = "Like comment", description = "Like the specified comment")
    @OperationLog(module = "Comment Management", operation = "Like Comment", description = "Like a comment")
    public Result<Boolean> likeComment(@PathVariable Long commentId) {
        Boolean result = commentService.likeComment(commentId);
        return Result.success(result);
    }

    /**
     * Unlike a comment
     */
    @DeleteMapping("/{commentId}/like")
    @Operation(summary = "Unlike comment", description = "Unlike a comment")
    @OperationLog(module = "Comment Management", operation = "Unlike Comment", description = "Unlike a comment")
    public Result<Boolean> unlikeComment(@PathVariable Long commentId) {
        Boolean result = commentService.unlikeComment(commentId);
        return Result.success(result);
    }

    /**
     * Paginated query of document comments
     */
    @PostMapping("/document/{documentId}")
    @Operation(summary = "Paginated query of document comments", description = "Paginated query of the document comment list")
    public Result<PageResult<CommentVO>> pageDocumentComments(
            @PathVariable Long documentId,
            @RequestBody CommentQueryDTO dto) {
        PageResult<CommentVO> pageResult = commentService.pageDocumentComments(documentId, dto);
        return Result.success(pageResult);
    }

    /**
     * Get the reply list for a comment
     */
    @GetMapping("/{parentCommentId}/replies")
    @Operation(summary = "Get comment replies", description = "Get the reply list for a comment")
    public Result<List<CommentVO>> getCommentReplies(@PathVariable Long parentCommentId) {
        List<CommentVO> replies = commentService.getCommentReplies(parentCommentId);
        return Result.success(replies);
    }
}
```

**Endpoint design notes:**

1. **Create a comment** - `POST /api/comments`
   - Supports creating both root comments and reply comments
   - Distinguished via `parentId`

2. **Delete a comment** - `DELETE /api/comments/{commentId}`
   - Checks whether it has child comments before deleting

3. **Like a comment** - `POST /api/comments/{commentId}/like`
   - Prevents duplicate likes

4. **Unlike** - `DELETE /api/comments/{commentId}/like`
   - Deletes the like record and updates the count

5. **Paginated query** - `POST /api/comments/document/{documentId}`
   - Only returns root comments
   - Child comments are returned via the `replies` field

6. **Get replies** - `GET /api/comments/{parentCommentId}/replies`
   - Returns all child comments of the specified comment

---

## IX. Testing and Verification

### 9.1 Start the Application

```bash
# Start the kb-document service
cd kb-document
mvn spring-boot:run
```

### 9.2 Test Creating a Comment

**Create a root comment:**
```bash
curl -X POST 'http://localhost:8083/api/comments' \
  -H 'Content-Type: application/json' \
  -d '{
    "documentId": 1,
    "content": "This document is really well written!"
  }'
```

**Create a reply comment:**
```bash
curl -X POST 'http://localhost:8083/api/comments' \
  -H 'Content-Type: application/json' \
  -d '{
    "documentId": 1,
    "parentId": 1,
    "content": "It really is helpful!",
    "replyToUserId": 1
  }'
```

### 9.3 Test Querying Comments

**Paginated query of document comments:**
```bash
curl -X POST 'http://localhost:8083/api/comments/document/1' \
  -H 'Content-Type: application/json' \
  -d '{
    "current": 1,
    "size": 10,
    "sortBy": "like_count",
    "sortOrder": "desc"
  }'
```

**Get comment replies:**
```bash
curl -X GET 'http://localhost:8083/api/comments/1/replies'
```

### 9.4 Test the Like Feature

**Like a comment:**
```bash
curl -X POST 'http://localhost:8083/api/comments/1/like'
```

**Unlike:**
```bash
curl -X DELETE 'http://localhost:8083/api/comments/1/like'
```

### 9.5 Test Deleting a Comment

```bash
curl -X DELETE 'http://localhost:8083/api/comments/1'
```

### 9.6 Verify the Knife4j Docs

Visit: `http://localhost:8083/doc.html`

View the API docs and try out the comment management endpoints online.

---

## X. Advanced Features

### 10.1 Comment Tree Structure Handling

The current implementation uses a two-level structure (root comment + child comment), implemented as follows:

1. **Data model design:**
   - `parentId=0` indicates a root comment
   - A child comment's `rootId` points to the root comment ID

2. **Query logic:**
   - First query all root comments
   - Then batch-query the child comments of each root comment
   - Assemble them into a tree structure in memory

3. **Advantages:**
   - Avoids performance problems from infinite nesting
   - Simpler frontend display
   - High query efficiency

### 10.2 Counter Update Strategy

To ensure count accuracy, the following strategy is used:

1. **Atomic database operations:**
   ```sql
   UPDATE tb_comment SET like_count = like_count + 1 WHERE id = ?
   ```

2. **Transactions guarantee consistency:**
   - The like record insertion and count update happen in the same transaction
   - Automatically rolled back on failure

3. **Avoiding concurrency issues:**
   - Uses database locking mechanisms
   - Prevents inaccurate counts

### 10.3 Duplicate-Like Prevention Design

1. **Unique index constraint:**
   ```sql
   UNIQUE KEY `uk_target_user_type` (`target_id`, `user_id`, `target_type`)
   ```

2. **Application-layer checking:**
   - Query for existence before inserting
   - Throws a friendly error message

### 10.4 Content Safety Filtering Recommendations

For production, it's recommended to add:

1. **Sensitive word filtering:**
   - Integrate a content safety detection service
   - Filter out spam and advertisements

2. **Comment moderation:**
   - New comments first enter a pending-review state
   - Displayed publicly only after approval

3. **Rate limiting:**
   - Limit how frequently a user can comment
   - Prevent spam flooding attacks

---

## XI. Performance Optimization Recommendations

### 11.1 Query Optimization

1. **Index optimization:**
   - Ensure the `idx_document_id` index exists
   - Consider creating composite indexes for compound queries

2. **Pagination optimization:**
   - Use cursor-based pagination for large amounts of data
   - Avoid deep pagination

3. **Caching strategy:**
   - Cache popular comment lists
   - Use Redis to cache counts

### 11.2 Concurrency Optimization

1. **Like operations:**
   - Process asynchronously via a message queue
   - Reduce database load

2. **Count updates:**
   - Batch updates instead of single-row updates
   - Periodically sync to the database

### 11.3 Storage Optimization

1. **Hot/cold data separation:**
   - Store recent comments in a hot table
   - Archive historical comments

2. **Content compression:**
   - Compress long text content for storage
   - Save storage space

---

## XII. Frequently Asked Questions

### Q1: How do you prevent like-farming (fake likes)?

**A:** Use multi-dimensional protection:
- IP restriction: limit the number of likes per IP within a short time
- CAPTCHA: require verification for frequent operations
- Account behavior analysis: identify abnormal account behavior
- Anomaly detection: monitor for sudden spikes in like frequency

### Q2: Does the comment feature support editing?

**A:** Not currently. To support it:
1. Add `updated_at` and `edit_count` fields to the comment table
2. Add an edit endpoint and permission validation
3. Record edit history for auditing
4. Show an "edited" indicator on the frontend

### Q3: How do you handle @-mentioning users in comments?

**A:** Implementation steps:
1. The frontend parses the @ symbol and pops up a user picker
2. Record `reply_to_user_id` and `reply_to_user_name` when saving
3. Notify the mentioned user
4. Highlight the @-mention on the frontend

### Q4: Do child comments support unlimited nesting levels?

**A:** Currently only two levels are supported, because:
- Avoids overly deep nesting hurting performance
- Provides a better user experience
- Two levels are sufficient for most scenarios

To support multiple levels:
1. Modify the query logic to support recursion
2. Use a closure table or materialized path pattern
3. Consider the performance impact

### Q5: How do you implement comment search?

**A:** Implementation options:
1. **Simple implementation:**
   ```sql
   SELECT * FROM tb_comment WHERE content LIKE '%keyword%'
   ```

2. **Efficient implementation:**
   - Integrate Elasticsearch
   - Full-text index support
   - Highlighted keyword display
   - Relevance-based ranking

---

## XIII. Summary

This article described in detail the complete implementation of the comment-related features in the kb-document service, including:

### Core Features
- ✅ Comment creation (supports root comments and replies)
- ✅ Comment deletion (with child comment checking)
- ✅ Like/unlike (duplicate prevention)
- ✅ Paginated query (with sorting support)
- ✅ Comment tree structure display

### Technical Highlights
1. **Two-level comment structure**: root comment + child comment, avoiding infinite nesting
2. **Denormalized design**: user information and counts are denormalized to improve query performance
3. **Atomic operations**: database atomic operations ensure count accuracy
4. **Unified like table**: supports likes on multiple target types
5. **Transaction management**: ensures data consistency

### Future Extensions
- Integrate content safety moderation
- Add a comment editing feature
- Support @-mentioning users and notifications
- Integrate Elasticsearch full-text search
- Comment reporting and moderation features

Through this tutorial, readers can master the complete design and implementation of an enterprise-grade comment system, providing a reference for real-world project development.
