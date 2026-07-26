package com.knowledge.base.statistics.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Daily statistics count - used for mapping MyBatis aggregate query results
 *
 * <p>Replaces {@code Map<String, Object>}, providing a type-safe carrier for daily aggregate data</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyCount {

    /** Date (yyyy-MM-dd) */
    private String date;

    /** Count */
    private Long count;
}
