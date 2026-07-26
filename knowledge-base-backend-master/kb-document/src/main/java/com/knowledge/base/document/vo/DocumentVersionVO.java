package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Document version VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Document version information")
public class DocumentVersionVO {

    /**
     * Version ID
     */
    @Schema(description = "Version ID")
    private Long id;

    /**
     * Document ID
     */
    @Schema(description = "Document ID")
    private Long documentId;

    /**
     * Version number
     */
    @Schema(description = "Version number")
    private Integer version;

    /**
     * Document title
     */
    @Schema(description = "Document title")
    private String title;

    /**
     * Version change description
     */
    @Schema(description = "Version change description")
    private String changeDescription;

    /**
     * Change size (bytes)
     */
    @Schema(description = "Change size")
    private Long changeSize;

    /**
     * Operator ID
     */
    @Schema(description = "Operator ID")
    private Long operatorId;

    /**
     * Operator name
     */
    @Schema(description = "Operator name")
    private String operatorName;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createdAt;
}
