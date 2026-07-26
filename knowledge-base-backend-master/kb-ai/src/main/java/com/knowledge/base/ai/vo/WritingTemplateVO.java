package com.knowledge.base.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Writing template VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Writing template")
public class WritingTemplateVO {

    /**
     * Unique template identifier
     */
    @Schema(description = "Unique template identifier", example = "tech-solution")
    private String id;

    /**
     * Template name
     */
    @Schema(description = "Template name", example = "Technical Proposal")
    private String name;

    /**
     * Template description
     */
    @Schema(description = "Template description", example = "For writing technical proposal documents, including background analysis, solution design, implementation plan, etc.")
    private String description;

    /**
     * Template category
     */
    @Schema(description = "Template category", example = "Technical Documentation")
    private String category;

    /**
     * Preset prompt/template content
     */
    @Schema(description = "Preset prompt/template content")
    private String prompt;

    /**
     * Suggested content type
     */
    @Schema(description = "Suggested content type", example = "article")
    private String suggestedContentType;

    /**
     * Suggested writing style
     */
    @Schema(description = "Suggested writing style", example = "technical")
    private String suggestedStyle;
}
