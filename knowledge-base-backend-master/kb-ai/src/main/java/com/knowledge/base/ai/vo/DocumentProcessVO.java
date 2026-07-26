package com.knowledge.base.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Document processing result VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document processing result")
public class DocumentProcessVO {

    /**
     * Processing type
     */
    @Schema(description = "Processing type")
    private String processType;

    /**
     * Processed content
     */
    @Schema(description = "Processed content")
    private String processedContent;

    /**
     * Whether the operation succeeded
     */
    @Schema(description = "Whether the operation succeeded")
    private Boolean success;

    /**
     * Message
     */
    @Schema(description = "Message")
    private String message;

    /**
     * Token count used
     */
    @Schema(description = "Token count used")
    private Integer tokens;

    /**
     * Original content
     */
    @Schema(description = "Original content")
    private String originalContent;
}