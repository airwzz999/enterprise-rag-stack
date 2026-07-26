package com.knowledge.base.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.document.dto.CommentCreateDTO;
import com.knowledge.base.document.dto.CommentQueryDTO;
import com.knowledge.base.document.entity.Comment;
import com.knowledge.base.document.vo.CommentVO;

import java.util.List;

/**
 * Comment Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface CommentService extends IService<Comment> {

    /**
     * Creates a comment
     *
     * @param dto creation DTO
     * @return comment ID
     */
    Long createComment(CommentCreateDTO dto);

    /**
     * Deletes a comment
     *
     * @param commentId comment ID
     * @return whether successful
     */
    Boolean deleteComment(Long commentId);

    /**
     * Likes a comment
     *
     * @param commentId comment ID
     * @return whether successful
     */
    Boolean likeComment(Long commentId);

    /**
     * Unlikes a comment
     *
     * @param commentId comment ID
     * @return whether successful
     */
    Boolean unlikeComment(Long commentId);

    /**
     * Paginated query of document comments
     *
     * @param documentId document ID
     * @param dto query DTO
     * @return paginated result
     */
    PageResult<CommentVO> pageDocumentComments(Long documentId, CommentQueryDTO dto);

    /**
     * Gets the reply list for a comment
     *
     * @param parentCommentId parent comment ID
     * @return reply list
     */
    List<CommentVO> getCommentReplies(Long parentCommentId);
}
