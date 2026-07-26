package com.knowledge.base.document.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Batch image URL conversion response
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Batch image URL conversion response")
public class BatchConvertResponse {

    @Schema(description = "Mapping of successfully converted URLs (original URL -> new URL)")
    private Map<String, String> urlMappings;

    @Schema(description = "Mapping of URLs that failed to convert (original URL -> error message)")
    private Map<String, String> errorMappings;

    @Schema(description = "Success count")
    private Integer successCount;

    @Schema(description = "Failure count")
    private Integer failureCount;
}
