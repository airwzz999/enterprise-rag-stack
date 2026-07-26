package com.knowledge.base.document.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batch export request DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchExportRequest {

    /** Document ID list (uses String to avoid JavaScript precision loss) */
    @NotEmpty(message = "Document ID list must not be empty")
    private List<String> documentIds;

    /** Export format: pdf / markdown */
    @NotNull(message = "Export format must not be null")
    private String format;
}
