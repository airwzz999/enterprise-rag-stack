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
@RequestMapping("/comments")
@RequiredArgsConstructor
@Tag(name = "Comment Management", description = "Comment management related endpoints")
public class CommentController {

    private final CommentService commentService;

    /**
     * Creates a comment
     */
    @PostMapping
    @Operation(summary = "Create comment", description = "Creates a comment on a document")
    @OperationLog(module = "Comment Management", operation = "Create Comment", description = "Creates a comment on a document")
    public Result<Long> createComment(@Valid @RequestBody CommentCreateDTO dto) {
        Long commentId = commentService.createComment(dto);
        return Result.success(commentId);
    }

    /**
     * Deletes a comment
     */
    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete comment", description = "Deletes the specified comment")
    @OperationLog(module = "Comment Management", operation = "Delete Comment", description = "Deletes a comment")
    public Result<Boolean> deleteComment(@PathVariable Long commentId) {
        Boolean result = commentService.deleteComment(commentId);
        return Result.success(result);
    }

    /**
     * Likes a comment
     */
    @PostMapping("/{commentId}/like")
    @Operation(summary = "Like comment", description = "Likes the specified comment")
    @OperationLog(module = "Comment Management", operation = "Like Comment", description = "Likes a comment")
    public Result<Boolean> likeComment(@PathVariable Long commentId) {
        Boolean result = commentService.likeComment(commentId);
        return Result.success(result);
    }

    /**
     * Unlikes a comment
     */
    @DeleteMapping("/{commentId}/like")
    @Operation(summary = "Unlike comment", description = "Unlikes a comment")
    @OperationLog(module = "Comment Management", operation = "Unlike Comment", description = "Unlikes a comment")
    public Result<Boolean> unlikeComment(@PathVariable Long commentId) {
        Boolean result = commentService.unlikeComment(commentId);
        return Result.success(result);
    }

    /**
     * Paginated query of document comments
     */
    @PostMapping("/document/{documentId}")
    @Operation(summary = "Paginated query of document comments", description = "Paginated query of the document's comment list")
    public Result<PageResult<CommentVO>> pageDocumentComments(
            @PathVariable Long documentId,
            @RequestBody CommentQueryDTO dto) {
        PageResult<CommentVO> pageResult = commentService.pageDocumentComments(documentId, dto);
        return Result.success(pageResult);
    }

    /**
     * Gets the reply list for a comment
     */
    @GetMapping("/{parentCommentId}/replies")
    @Operation(summary = "Get comment replies", description = "Gets the reply list for a comment")
    public Result<List<CommentVO>> getCommentReplies(@PathVariable Long parentCommentId) {
        List<CommentVO> replies = commentService.getCommentReplies(parentCommentId);
        return Result.success(replies);
    }
}
