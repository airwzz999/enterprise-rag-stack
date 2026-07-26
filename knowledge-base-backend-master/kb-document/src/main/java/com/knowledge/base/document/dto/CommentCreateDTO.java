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

    @NotNull(message = "Document ID must not be null")
    @Schema(description = "Document ID")
    private Long documentId;

    @Schema(description = "Parent comment ID")
    private Long parentId;

    @NotBlank(message = "Comment content must not be blank")
    @Schema(description = "Comment content")
    private String content;

    @Schema(description = "Reply-to user (user ID)")
    private Long replyToUserId;
}
