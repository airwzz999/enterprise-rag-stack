package com.knowledge.base.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Auto-save history query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Auto-save history query parameters")
public class AutoSaveHistoryQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Document ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long documentId;

    @Schema(description = "Current page", example = "1")
    private Long current = 1L;

    @Schema(description = "Page size", example = "20")
    private Long size = 20L;
}
