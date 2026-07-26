package com.knowledge.base.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI writing request parameters
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI writing request parameters")
public class WritingRequestDTO {

    /**
     * Writing topic/title
     */
    @Schema(description = "Writing topic/title", example = "How to write a good technical proposal")
    @NotBlank(message = "Writing topic must not be blank")
    private String topic;

    /**
     * Writing requirements/additional notes
     */
    @Schema(description = "Writing requirements/additional notes", example = "A technical proposal for technical managers, including background, goals, solution design, and implementation plan")
    private String requirements;

    /**
     * Content type: article, report, documentation, email, announcement
     */
    @Schema(description = "Content type", example = "article",
            allowableValues = {"article", "report", "documentation", "email", "announcement"})
    private String contentType;

    /**
     * Writing style: formal, casual, technical, creative, academic
     */
    @Schema(description = "Writing style", example = "formal",
            allowableValues = {"formal", "casual", "technical", "creative", "academic"})
    private String style;

    /**
     * Tone: neutral, enthusiastic, serious, friendly, authoritative
     */
    @Schema(description = "Tone", example = "neutral",
            allowableValues = {"neutral", "enthusiastic", "serious", "friendly", "authoritative"})
    private String tone;

    /**
     * Desired word count
     */
    @Schema(description = "Desired word count", example = "1000")
    private Integer length;

    /**
     * Reference/existing content (used for expand, optimize, and continue scenarios)
     */
    @Schema(description = "Reference/existing content (used for expand, optimize, and continue scenarios)")
    private String existingContent;

    /**
     * Action type: generate, expand, optimize, continue
     */
    @Schema(description = "Action type", example = "generate",
            allowableValues = {"generate", "expand", "optimize", "continue"})
    @NotBlank(message = "Action type must not be blank")
    private String actionType;

    /**
     * Template ID (passed in when using a preset template)
     */
    @Schema(description = "Template ID (passed in when using a preset template)", example = "tech-solution")
    private String templateId;

    /**
     * Model name to use; defaults to the configured default model
     */
    @Schema(description = "Model name to use; defaults to the configured default model", example = "qwen")
    private String model;
}
