package com.knowledge.base.document.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Batch upload response
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Batch upload response")
public class BatchUploadResponse {

    @Schema(description = "Mapping of file name to URL")
    private Map<String, String> fileUrls;

    @Schema(description = "Success count")
    private Integer successCount;

    @Schema(description = "Failure count")
    private Integer failureCount;
}
