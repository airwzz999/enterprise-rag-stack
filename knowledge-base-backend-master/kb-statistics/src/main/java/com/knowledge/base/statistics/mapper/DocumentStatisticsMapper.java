package com.knowledge.base.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.statistics.entity.DocumentStatistics;
import com.knowledge.base.statistics.model.DailyCount;
import com.knowledge.base.statistics.model.IdCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Document statistics Mapper
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface DocumentStatisticsMapper extends BaseMapper<DocumentStatistics> {

    /**
     * Counts total documents
     */
    Long countAll();

    /**
     * Counts documents by status
     */
    Long countByStatus(@Param("status") Integer status);

    /**
     * Counts documents by author ID
     */
    Long countByAuthorId(@Param("authorId") Long authorId);

    /**
     * Counts documents by category ID
     */
    Long countByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * Counts documents within a specified date range
     */
    Long countByDateRange(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    /**
     * Counts daily document creation totals
     */
    List<DailyCount> countDailyDocuments(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Finds the most viewed documents (by view count)
     */
    List<DocumentStatistics> selectMostViewedDocuments(@Param("limit") Integer limit);

    /**
     * Finds the most liked documents (by like count)
     */
    List<DocumentStatistics> selectMostLikedDocuments(@Param("limit") Integer limit);

    /**
     * Finds the most favorited documents
     */
    List<DocumentStatistics> selectMostFavoritedDocuments(@Param("limit") Integer limit);

    /**
     * Finds the most active authors
     */
    List<IdCount> selectTopAuthors(@Param("limit") Integer limit);

    /**
     * Counts documents per category
     */
    List<IdCount> countByCategory();

    /**
     * Sums total view count
     */
    Long sumViewCount();

    /**
     * Sums total like count
     */
    Long sumLikeCount();

    /**
     * Sums total favorite count
     */
    Long sumFavoriteCount();
}
