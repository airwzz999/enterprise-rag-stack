package com.knowledge.base.file.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Batch conversion response
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchConvertResponse {

    /**
     * URL mappings (original URL -> new URL)
     */
    private Map<String, String> urlMappings;

    /**
     * Error mappings (original URL -> error message)
     */
    private Map<String, String> errorMappings;

    /**
     * Success count
     */
    private Integer successCount;

    /**
     * Failure count
     */
    private Integer failureCount;
}
