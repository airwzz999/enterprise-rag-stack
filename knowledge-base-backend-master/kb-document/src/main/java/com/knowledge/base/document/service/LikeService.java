package com.knowledge.base.document.service;

/**
 * Like service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface LikeService {

    /**
     * Likes a target
     *
     * @param targetId   target ID (document ID or comment ID)
     * @param userId     user ID
     * @param targetType target type: 1-document, 2-comment
     * @throws com.knowledge.base.common.exception.BusinessException thrown when already liked
     */
    void like(Long targetId, Long userId, Integer targetType);

    /**
     * Unlikes a target
     *
     * @param targetId   target ID (document ID or comment ID)
     * @param userId     user ID
     * @param targetType target type: 1-document, 2-comment
     * @throws com.knowledge.base.common.exception.BusinessException thrown when not yet liked
     */
    void unlike(Long targetId, Long userId, Integer targetType);

    /**
     * Checks whether a target is already liked
     *
     * @param targetId   target ID (document ID or comment ID)
     * @param userId     user ID
     * @param targetType target type: 1-document, 2-comment
     * @return true-liked, false-not liked
     */
    boolean isLiked(Long targetId, Long userId, Integer targetType);
}
