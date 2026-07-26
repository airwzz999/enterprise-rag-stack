package com.knowledge.base.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Base class for pagination query parameters
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Pagination query parameters")
public class PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Current page number
     */
    @Schema(description = "Current page number", example = "1")
    private Long current = 1L;

    /**
     * Page size
     */
    @Schema(description = "Page size", example = "10")
    private Long size = 10L;

    /**
     * Sort field
     */
    @Schema(description = "Sort field")
    private String sortField;

    /**
     * Sort order (asc/desc)
     */
    @Schema(description = "Sort order", example = "desc")
    private String sortOrder;

    /**
     * Get the offset
     */
    public long getOffset() {
        return (current - 1) * size;
    }
}
