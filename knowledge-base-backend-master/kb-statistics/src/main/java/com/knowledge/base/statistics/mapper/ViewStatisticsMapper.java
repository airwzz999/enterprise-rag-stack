package com.knowledge.base.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.statistics.entity.ViewStatistics;
import com.knowledge.base.statistics.model.DailyCount;
import com.knowledge.base.statistics.model.IdCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * View statistics Mapper
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface ViewStatisticsMapper extends BaseMapper<ViewStatistics> {

    /**
     * Counts total view history records
     */
    Long countAll();

    /**
     * Counts views by user ID
     */
    Long countByUserId(@Param("userId") Long userId);

    /**
     * Counts views by document ID
     */
    Long countByDocumentId(@Param("documentId") Long documentId);

    /**
     * Counts views within a specified date range
     */
    Long countByDateRange(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    /**
     * Counts a user's views within a specified date range
     */
    Long countByUserIdAndDateRange(@Param("userId") Long userId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Counts daily view totals
     */
    List<DailyCount> countDailyViews(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Counts a document's views within a specified date range
     */
    Long countByDocumentIdAndDateRange(@Param("documentId") Long documentId,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Finds the most viewed documents (by view count)
     */
    List<IdCount> selectMostViewedDocuments(@Param("limit") Integer limit);

    /**
     * Finds the most active viewing users
     */
    List<IdCount> selectMostActiveViewers(@Param("limit") Integer limit);

    /**
     * Queries the list of document IDs a user has viewed
     */
    List<Long> selectViewedDocumentIdsByUserId(@Param("userId") Long userId,
                                                @Param("limit") Integer limit);

    /**
     * Checks whether a user has viewed a specified document
     */
    Boolean hasViewedDocument(@Param("userId") Long userId,
                              @Param("documentId") Long documentId);

    /**
     * Queries a user's recent view history
     */
    List<ViewStatistics> selectRecentViewsByUserId(@Param("userId") Long userId,
                                                    @Param("limit") Integer limit);

    /**
     * Counts active users (distinct users with view records in the last 30 days)
     */
    Long countActiveUsers(@Param("sinceDate") LocalDateTime sinceDate);
}
