package com.knowledge.base.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.statistics.entity.CommentStatistics;
import com.knowledge.base.statistics.model.DailyCount;
import com.knowledge.base.statistics.model.IdCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Comment statistics Mapper
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface CommentStatisticsMapper extends BaseMapper<CommentStatistics> {

    /**
     * Counts comments by user ID
     */
    Long countByUserId(@Param("userId") Long userId);

    /**
     * Counts comments by document ID
     */
    Long countByDocumentId(@Param("documentId") Long documentId);

    /**
     * Counts comments within a specified date range
     */
    Long countByDateRange(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    /**
     * Counts daily comment totals
     */
    List<DailyCount> countDailyComments(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Counts a user's comments within a specified date range
     */
    Long countByUserIdAndDateRange(@Param("userId") Long userId,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Finds the most active commenting users (user_id, comment_count)
     */
    List<IdCount> selectTopCommenters(@Param("limit") Integer limit);

    /**
     * Finds the most commented documents (document_id, comment_count)
     */
    List<IdCount> selectMostCommentedDocuments(@Param("limit") Integer limit);

    /**
     * Computes the average daily comment count
     */
    Double getAverageDailyComments(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
}
