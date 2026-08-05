package com.knowledge.base.statistics.controller;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.statistics.service.StatisticsService;
import com.knowledge.base.statistics.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Statistics analysis controller
 *
 * <p>Designed following the Alibaba Java Development Guidelines, providing data statistics and analysis APIs</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("")
@Tag(name = "Statistics Analysis", description = "Data statistics and analysis API")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    /**
     * Gets the data overview
     *
     * @return the data overview information
     */
    @GetMapping("/overview")
    @Operation(summary = "Data overview", description = "Get system data overview information")
    public Result<OverviewVO> getOverview() {
        log.info("Get data overview request");

        OverviewVO overview = statisticsService.getOverview();
        return Result.success(overview);
    }

    /**
     * Gets the document trend
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @param type      the trend type (create, view, like, favorite)
     * @return the trend data
     */
    @GetMapping("/trend/document")
    @Operation(summary = "Document trend", description = "Get document trend data")
    public Result<List<TrendVO>> getDocumentTrend(
        @Parameter(description = "Start date", required = true)
        @RequestParam LocalDate startDate,
        @Parameter(description = "End date", required = true)
        @RequestParam LocalDate endDate,
        @Parameter(description = "Trend type", required = true)
        @RequestParam String type) {
        log.info("Get document trend request: startDate={}, endDate={}, type={}", startDate, endDate, type);

        List<TrendVO> trends = statisticsService.getDocumentTrend(startDate, endDate, type);
        return Result.success(trends);
    }

    /**
     * Gets user activity
     *
     * @param startDate the start date
     * @param endDate   the end date
     * @return the activity data
     */
    @GetMapping("/activity/user")
    @Operation(summary = "User activity", description = "Get user activity statistics")
    public Result<List<UserActivityVO>> getUserActivity(
        @Parameter(description = "Start date", required = true)
        @RequestParam LocalDate startDate,
        @Parameter(description = "End date", required = true)
        @RequestParam LocalDate endDate) {
        log.info("Get user activity request: startDate={}, endDate={}", startDate, endDate);

        List<UserActivityVO> activities = statisticsService.getUserActivity(startDate, endDate);
        return Result.success(activities);
    }

    /**
     * Gets the category distribution
     *
     * @return the category distribution data
     */
    @GetMapping("/distribution/category")
    @Operation(summary = "Category distribution", description = "Get document category distribution statistics")
    public Result<List<CategoryDistributionVO>> getCategoryDistribution() {
        log.info("Get category distribution request");

        List<CategoryDistributionVO> distributions = statisticsService.getCategoryDistribution();
        return Result.success(distributions);
    }

    /**
     * Gets popular documents
     *
     * @param type the statistics type (view, like, favorite)
     * @param size the count
     * @return the list of popular documents
     */
    @GetMapping("/hot/document")
    @Operation(summary = "Popular documents", description = "Get the popular document ranking")
    public Result<List<HotDocumentVO>> getHotDocuments(
        @Parameter(description = "Statistics type", required = true)
        @RequestParam String type,
        @Parameter(description = "Count")
        @RequestParam(defaultValue = "10") Integer size) {
        log.info("Get popular documents request: type={}, size={}", type, size);

        List<HotDocumentVO> documents = statisticsService.getHotDocuments(type, size);
        return Result.success(documents);
    }

    /**
     * Gets the latest documents
     *
     * @param size the count
     * @return the latest document list
     */
    @GetMapping("/latest/documents")
    @Operation(summary = "Latest documents", description = "Get the list of most recently published documents")
    public Result<List<HotDocumentVO>> getLatestDocuments(
        @Parameter(description = "Count")
        @RequestParam(defaultValue = "6") Integer size) {
        log.info("Get latest documents request: size={}", size);

        List<HotDocumentVO> documents = statisticsService.getLatestDocuments(size);
        return Result.success(documents);
    }

    /**
     * Gets active users
     *
     * @param type the statistics type (create, comment, view)
     * @param size the count
     * @return the list of active users
     */
    @GetMapping("/active/user")
    @Operation(summary = "Active users", description = "Get the active user ranking")
    public Result<List<ActiveUserVO>> getActiveUsers(
        @Parameter(description = "Statistics type", required = true)
        @RequestParam String type,
        @Parameter(description = "Count")
        @RequestParam(defaultValue = "10") Integer size) {
        log.info("Get active users request: type={}, size={}", type, size);

        List<ActiveUserVO> users = statisticsService.getActiveUsers(type, size);
        return Result.success(users);
    }

    /**
     * Gets the dashboard data
     *
     * @return the dashboard data
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard data", description = "Get comprehensive dashboard data")
    public Result<DashboardVO> getDashboardData() {
        log.info("Get dashboard data request");

        DashboardVO dashboard = statisticsService.getDashboardData();
        return Result.success(dashboard);
    }

    /**
     * Gets the admin overview
     *
     * @return the admin overview data
     */
    @GetMapping("/admin-overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Admin overview", description = "Get key metrics for the admin dashboard")
    public Result<AdminOverviewVO> getAdminOverview() {
        log.info("Get admin overview request");

        AdminOverviewVO overview = statisticsService.getAdminOverview();
        return Result.success(overview);
    }
}
