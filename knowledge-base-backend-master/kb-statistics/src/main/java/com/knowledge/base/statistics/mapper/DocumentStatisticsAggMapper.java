package com.knowledge.base.statistics.mapper;

import com.knowledge.base.statistics.model.DailyCount;
import com.knowledge.base.statistics.model.IdCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Document statistics pre-aggregated table Mapper
 *
 * <p>Queries the kb_document_statistics pre-aggregated table, for scenarios such as trends and rankings that need aggregate computation</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface DocumentStatisticsAggMapper {

    /**
     * Aggregates document view counts by date
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @return [{"date": "2024-01-01", "count": 120}, ...]
     */
    List<DailyCount> countDailyViews(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    /**
     * Queries the most viewed documents within a specified date range
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @param limit     the number of results to return
     * @return the document ranking list
     */
    List<Map<String, Object>> selectTopDocumentsByViews(@Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate,
                                                         @Param("limit") int limit);
}
