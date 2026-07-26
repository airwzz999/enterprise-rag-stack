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
import com.knowledge.base.document.event.StatisticsEventPublisher;
import com.knowledge.base.document.mapper.CommentMapper;
import com.knowledge.base.document.service.CommentService;
import com.knowledge.base.document.client.UserServiceClient;
import com.knowledge.base.document.utils.UserContext;
import com.knowledge.base.document.vo.CommentVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Comment Service implementation class
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, implements comment related business logic</p>
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

    @Resource
    private UserServiceClient userServiceClient;

    @Resource
    private StatisticsEventPublisher statisticsEventPublisher;

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
                throw new BusinessException("Parent comment does not belong to this document");
            }
            rootId = parentComment.getRootId() != null ? parentComment.getRootId() : parentComment.getId();

            // Update the parent comment's reply count
            jdbcTemplate.update(
                    "UPDATE tb_comment SET reply_count = reply_count + 1 WHERE id = ?",
                    dto.getParentId()
            );
        }

        // Get the current user information from the context
        Long userId = UserContext.getCurrentUserId();
        String userName = UserContext.getCurrentUserName();
        String userAvatar = UserContext.getCurrentUserAvatar();
        // If there is no avatar in the JWT, fetch the latest avatar from the user service
        if (userAvatar == null || userAvatar.isEmpty()) {
            userAvatar = userServiceClient.getUserAvatar(userId);
        }

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

        // Publish a comment statistics event to RabbitMQ
        String documentTitle;
        try {
            documentTitle = jdbcTemplate.queryForObject(
                    "SELECT title FROM kb_document WHERE id = ?", String.class, dto.getDocumentId());
        } catch (Exception e) {
            documentTitle = null;
        }
        statisticsEventPublisher.publishCommentEvent(userId, userName, dto.getDocumentId(), documentTitle);

        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteComment(Long commentId) {
        log.info("Delete comment: commentId={}", commentId);

        if (commentId == null) {
            throw new BusinessException("Comment ID must not be null");
        }

        // Check whether the comment exists
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("Comment does not exist");
        }

        // Check permission; only the comment author can delete it
        Long currentUserId = UserContext.getCurrentUserId();
        if (!currentUserId.equals(comment.getCommenterId())) {
            throw new BusinessException("You can only delete your own comments");
        }

        // Check whether there are child comments
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
            throw new BusinessException("Comment ID must not be null");
        }

        // Check whether the comment exists
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("Comment does not exist");
        }

        // Get the current user ID from the context
        Long userId = getCurrentUserIdSafely();

        // Check whether it has already been liked
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_like WHERE target_id = ? AND user_id = ? AND target_type = 2",
                Integer.class,
                commentId, userId
        );

        if (count != null && count > 0) {
            throw new BusinessException("Already liked");
        }

        // Add the like record
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
            throw new BusinessException("Comment ID must not be null");
        }

        // Check whether the comment exists
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("Comment does not exist");
        }

        // Get the current user ID from the context
        Long userId = getCurrentUserIdSafely();

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
        // Build the query conditions - query only root comments
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getDocumentId, documentId)
                .eq(Comment::getParentId, 0)
                .eq(Comment::getStatus, 1);

        // Sort
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

        // Get the current user ID
        Long userId = getCurrentUserIdSafely();

        // Convert to VO and load child comments
        List<Comment> records = commentPage.getRecords();
        List<CommentVO> voRecords = new ArrayList<>();
        Set<Long> allCommentIds = new HashSet<>();

        for (Comment comment : records) {
            CommentVO vo = convertToVO(comment);
            allCommentIds.add(comment.getId());
            List<CommentVO> replies = getCommentReplies(comment.getId());
            for (CommentVO reply : replies) {
                allCommentIds.add(reply.getId());
            }
            vo.setReplies(replies);
            voRecords.add(vo);
        }

        // Batch-query whether the current user has liked these comments
        Set<Long> likedIds = queryLikedCommentIds(userId, allCommentIds);

        // Set isLiked
        for (CommentVO vo : voRecords) {
            vo.setIsLiked(likedIds.contains(vo.getId()));
            if (vo.getReplies() != null) {
                for (CommentVO reply : vo.getReplies()) {
                    reply.setIsLiked(likedIds.contains(reply.getId()));
                }
            }
        }

        IPage<CommentVO> voPage = new Page<>(commentPage.getCurrent(), commentPage.getSize(), commentPage.getTotal());
        voPage.setRecords(voRecords);

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

        Long userId = getCurrentUserIdSafely();
        Set<Long> likedIds = queryLikedCommentIds(userId,
                comments.stream().map(Comment::getId).collect(Collectors.toSet()));

        return comments.stream()
                .map(comment -> {
                    CommentVO vo = convertToVO(comment);
                    vo.setIsLiked(likedIds.contains(comment.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * Safely gets the current user ID (returns null if not logged in)
     */
    private Long getCurrentUserIdSafely() {
        try {
            return UserContext.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Batch-queries comment like status
     *
     * @param userId     current user ID (null means not logged in)
     * @param commentIds comment ID set
     * @return set of liked comment IDs
     */
    private Set<Long> queryLikedCommentIds(Long userId, Set<Long> commentIds) {
        if (userId == null || commentIds.isEmpty()) {
            return Collections.emptySet();
        }

        // Build the IN query
        String placeholders = commentIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.addAll(commentIds);

        List<Long> likedIds = jdbcTemplate.queryForList(
                "SELECT target_id FROM tb_like WHERE user_id = ? AND target_type = 2 AND target_id IN (" + placeholders + ")",
                Long.class,
                params.toArray()
        );

        return new HashSet<>(likedIds);
    }

    /**
     * Converts to VO
     *
     * @param comment comment entity
     * @return comment VO
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
