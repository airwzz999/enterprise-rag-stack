package com.knowledge.base.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Pagination result wrapper class
 *
 * @param <T> data type
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pagination result")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Current page number
     */
    @Schema(description = "Current page number")
    private Long current;

    /**
     * Page size
     */
    @Schema(description = "Page size")
    private Long size;

    /**
     * Total number of records
     */
    @Schema(description = "Total number of records")
    private Long total;

    /**
     * Total number of pages
     */
    @Schema(description = "Total number of pages")
    private Long pages;

    /**
     * Data list
     */
    @Schema(description = "Data list")
    private List<T> records;

    /**
     * Build a pagination result
     */
    public static <T> PageResult<T> of(Long current, Long size, Long total, List<T> records) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setCurrent(current);
        pageResult.setSize(size);
        pageResult.setTotal(total);
        pageResult.setRecords(records);
        pageResult.setPages((total + size - 1) / size);
        return pageResult;
    }

    /**
     * Empty pagination result
     */
    @SuppressWarnings("unchecked")
    public static <T> PageResult<T> empty() {
        return PageResult.<T>builder()
                .current(1L)
                .size(10L)
                .total(0L)
                .records(Collections.emptyList())
                .build();
    }

    /**
     * Check whether there is any data
     */
    public boolean hasData() {
        return records != null && !records.isEmpty();
    }
}
