package com.knowledge.base.foundation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.foundation.entity.OperationLog;
import com.knowledge.base.foundation.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Operation log Controller
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; provides
 * operation log management endpoints</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/logs")
@Tag(name = "Operation Log Management", description = "Operation log management endpoints")
public class OperationLogController {

    @Resource
    private OperationLogService operationLogService;

    /**
     * Paginated query of the log list
     *
     * @param current       current page
     * @param size          page size
     * @param module        module name
     * @param operationType operation type
     * @param username      username
     * @param startTime     start time
     * @param endTime       end time
     * @return paginated log information
     */
    @GetMapping
    @Operation(summary = "Paginated query of logs", description = "Paginated query of the operation log list")
    public Result<IPage<OperationLog>> pageLogs(
        @Parameter(description = "Current page") @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Long size,
        @Parameter(description = "Module name") @RequestParam(required = false) String module,
        @Parameter(description = "Operation type") @RequestParam(required = false) String operationType,
        @Parameter(description = "Username") @RequestParam(required = false) String username,
        @Parameter(description = "Start time") @RequestParam(required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @Parameter(description = "End time") @RequestParam(required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        log.info("Paginated log query request: current={}, size={}, module={}, operationType={}, username={}",
            current, size, module, operationType, username);

        IPage<OperationLog> page = operationLogService.pageLogs(current, size, module,
            operationType, username, startTime, endTime);
        return Result.success(page);
    }

    /**
     * Query log details by ID
     *
     * @param id log ID
     * @return log details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Query log details", description = "Query log details by log ID")
    public Result<OperationLog> getLogById(
        @Parameter(description = "Log ID", required = true)
        @PathVariable Long id) {
        log.info("Query log details request: id={}", id);

        OperationLog log = operationLogService.getLogById(id);
        return Result.success(log);
    }

    /**
     * Get log statistics
     *
     * @param startTime start time
     * @param endTime   end time
     * @return statistics information
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get log statistics", description = "Get operation log statistics")
    public Result<Map<String, Object>> getStatistics(
        @Parameter(description = "Start time") @RequestParam(required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @Parameter(description = "End time") @RequestParam(required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        log.info("Get log statistics request: startTime={}, endTime={}", startTime, endTime);

        Map<String, Object> statistics = operationLogService.getStatistics(startTime, endTime);
        return Result.success(statistics);
    }

    /**
     * Delete logs older than the specified date
     *
     * @param beforeDate the cutoff date
     * @return number of deleted rows
     */
    @DeleteMapping("/before-date")
    @Operation(summary = "Delete historical logs", description = "Delete operation logs older than the specified date")
    public Result<Integer> deleteLogsBeforeDate(
        @Parameter(description = "Cutoff date", required = true)
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beforeDate) {
        log.info("Delete historical logs request: beforeDate={}", beforeDate);

        Integer count = operationLogService.deleteLogsBeforeDate(beforeDate);
        return Result.success("Deleted successfully", count);
    }
}
