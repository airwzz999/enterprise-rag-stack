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

    @Schema(description = "Reply-to user (user ID)")
    private Long replyToUserId;

    @Schema(description = "Reply-to user (user name)")
    private String replyToUserName;

    @Schema(description = "Status")
    private Integer status;

    @Schema(description = "Like count")
    private Integer likeCount;

    @Schema(description = "Reply count")
    private Integer replyCount;

    @Schema(description = "Whether liked")
    private Boolean isLiked;

    @Schema(description = "Creation time")
    private LocalDateTime createdAt;

    @Schema(description = "Child comment list")
    private List<CommentVO> replies;
}
