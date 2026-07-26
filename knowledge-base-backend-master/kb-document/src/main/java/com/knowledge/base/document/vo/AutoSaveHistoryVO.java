package com.knowledge.base.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Auto-save history VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Auto-save history snapshot")
public class AutoSaveHistoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Snapshot ID (MongoDB _id)")
    private String id;

    @Schema(description = "Document ID")
    private Long documentId;

    @Schema(description = "Document title at snapshot time")
    private String title;

    @Schema(description = "Content preview (first 200 characters, returned by the list endpoint)")
    private String contentPreview;

    @Schema(description = "Full Markdown content (returned only by the detail endpoint)")
    private String content;

    @Schema(description = "Content length (character count)")
    private Integer contentLength;

    @Schema(description = "Snapshot save time")
    private LocalDateTime savedAt;
}
