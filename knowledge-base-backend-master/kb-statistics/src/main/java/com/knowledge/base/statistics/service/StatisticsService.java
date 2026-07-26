package com.knowledge.base.statistics.service;

import com.knowledge.base.statistics.vo.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Statistics analysis service interface
 *
 * <p>Provides business logic related to data statistics</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface StatisticsService {

    /**
     * Gets the data overview
     *
     * @return the data overview information
     */
    OverviewVO getOverview();

    /**
     * Gets the document trend
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @param type      the trend type (create, view, like, favorite)
     * @return the list of trend data
     */
    List<TrendVO> getDocumentTrend(LocalDate startDate, LocalDate endDate, String type);

    /**
     * Gets user activity
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @return the list of activity data
     */
    List<UserActivityVO> getUserActivity(LocalDate startDate, LocalDate endDate);

    /**
     * Gets the category distribution
     *
     * @return the list of category distribution data
     */
    List<CategoryDistributionVO> getCategoryDistribution();

    /**
     * Gets popular documents
     *
     * @param type the statistics type (view, like, favorite)
     * @param size the count
     * @return the list of popular documents
     */
    List<HotDocumentVO> getHotDocuments(String type, Integer size);

    /**
     * Gets active users
     *
     * @param type the statistics type (create, comment, view)
     * @param size the count
     * @return the list of active users
     */
    List<ActiveUserVO> getActiveUsers(String type, Integer size);

    /**
     * Gets the dashboard data
     *
     * @return the comprehensive dashboard data
     */
    DashboardVO getDashboardData();

    /**
     * Gets the latest documents
     *
     * @param size the count
     * @return the latest document list
     */
    List<HotDocumentVO> getLatestDocuments(Integer size);

    /**
     * Gets the admin overview
     *
     * @return the admin overview data
     */
    AdminOverviewVO getAdminOverview();
}
