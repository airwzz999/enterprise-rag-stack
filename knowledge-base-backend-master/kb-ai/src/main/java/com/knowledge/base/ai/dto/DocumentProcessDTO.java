package com.knowledge.base.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Document processing DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document processing parameters")
public class DocumentProcessDTO {

    /**
     * Document content
     */
    @Schema(description = "Document content")
    @NotBlank(message = "Document content must not be blank")
    private String content;

    /**
     * Document title
     */
    @Schema(description = "Document title")
    private String title;

    /**
     * Processing type (summary/outline/expansion/optimization/example)
     */
    @Schema(description = "Processing type")
    @NotBlank(message = "Processing type must not be blank")
    private String processType;

    /**
     * Processing parameters
     */
    @Schema(description = "Processing parameters")
    private ProcessParams processParams;

    /**
     * Processing parameters
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Processing parameters")
    public static class ProcessParams {

        /**
         * Summary length
         */
        @Schema(description = "Summary length")
        private Integer summaryLength;

        /**
         * Outline level
         */
        @Schema(description = "Outline level")
        private Integer outlineLevel;

        /**
         * Expansion type
         */
        @Schema(description = "Expansion type")
        private String expansionType;

        /**
         * Optimization target
         */
        @Schema(description = "Optimization target")
        private String optimizationTarget;

        /**
         * Example type
         */
        @Schema(description = "Example type")
        private String exampleType;
    }
}
