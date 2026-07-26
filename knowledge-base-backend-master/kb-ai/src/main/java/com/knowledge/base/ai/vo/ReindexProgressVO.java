package com.knowledge.base.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Reindex progress VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reindex progress")
public class ReindexProgressVO {

    @Schema(description = "Task ID")
    private String taskId;

    @Schema(description = "Task status: RUNNING | COMPLETED | FAILED | NOT_FOUND")
    private String status;

    @Schema(description = "Total number of documents")
    private int totalDocuments;

    @Schema(description = "Number completed")
    private int completedDocuments;

    @Schema(description = "Number failed")
    private int failedDocuments;

    @Schema(description = "Start time")
    private LocalDateTime startTime;

    @Schema(description = "Completion time")
    private LocalDateTime endTime;
}
