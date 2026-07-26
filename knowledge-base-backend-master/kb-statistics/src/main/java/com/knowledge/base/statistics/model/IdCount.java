package com.knowledge.base.statistics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ID + count - used for mapping MyBatis aggregate query results
 *
 * <p>Suitable for scenarios that group statistics by a dimension ID (e.g., grouping by user ID, document ID, or category ID)</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdCount {

    /** Dimension ID (e.g., user ID, document ID, category ID) */
    private Long id;

    /** Count */
    private Long count;
}
