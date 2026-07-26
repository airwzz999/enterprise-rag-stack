package com.knowledge.base.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI writing result VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI writing result")
public class WritingResultVO {

    /**
     * Generated content
     */
    @Schema(description = "Generated content")
    private String content;

    /**
     * Token count used
     */
    @Schema(description = "Token count used", example = "856")
    private Integer tokens;

    /**
     * Word count of the generated content
     */
    @Schema(description = "Word count of the generated content", example = "1024")
    private Integer wordCount;

    /**
     * Model name used
     */
    @Schema(description = "Model name used", example = "qwen")
    private String model;
}
