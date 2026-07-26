package com.knowledge.base.statistics.mapper;

import com.knowledge.base.statistics.model.IdCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * User statistics pre-aggregated table Mapper
 *
 * <p>Queries the kb_user_statistics pre-aggregated table, for scenarios such as active-user rankings</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface UserStatisticsAggMapper {

    /**
     * Queries the most active users within a specified date range (by view count)
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @param limit     the number of results to return
     * @return the active user ranking list
     */
    List<IdCount> selectTopActiveUsers(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate,
                                      @Param("limit") int limit);
}
