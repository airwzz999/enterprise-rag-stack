package com.knowledge.base.statistics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.statistics.entity.UserStatistics;
import com.knowledge.base.statistics.model.DailyCount;
import com.knowledge.base.statistics.model.IdCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User statistics Mapper
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface UserStatisticsMapper extends BaseMapper<UserStatistics> {

    /**
     * Counts total users
     */
    Long countAll();

    /**
     * Counts users by status
     */
    Long countByStatus(@Param("status") Integer status);

    /**
     * Counts active users (those with operation records within a specified period)
     */
    Long countActiveUsers(@Param("since") LocalDateTime since);

    /**
     * Counts users registered within a specified date range
     */
    Long countByDateRange(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    /**
     * Counts daily new user registrations
     */
    List<DailyCount> countDailyUsers(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Finds the most active users (by operation count)
     */
    List<IdCount> selectMostActiveUsers(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       @Param("limit") Integer limit);

    /**
     * Counts a user's operations
     */
    Long countUserOperations(@Param("userId") Long userId,
                             @Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);

    /**
     * Counts documents published by a user
     */
    Long countUserDocuments(@Param("userId") Long userId);

    /**
     * Counts a user's comments
     */
    Long countUserComments(@Param("userId") Long userId);

    /**
     * Counts a user's view history records
     */
    Long countUserViews(@Param("userId") Long userId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);
}
