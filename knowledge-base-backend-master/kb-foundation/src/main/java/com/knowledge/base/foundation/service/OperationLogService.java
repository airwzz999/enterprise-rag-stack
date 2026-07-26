package com.knowledge.base.foundation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.foundation.entity.OperationLog;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Operation log Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface OperationLogService {

    /**
     * Paginated query of operation logs
     *
     * @param current       current page
     * @param size          page size
     * @param module        module (optional)
     * @param operationType operation type (optional)
     * @param username      username (optional)
     * @param startTime     start time (optional)
     * @param endTime       end time (optional)
     * @return paginated result
     */
    IPage<OperationLog> pageLogs(Long current, Long size, String module, String operationType, String username, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Get an operation log by ID
     *
     * @param id log ID
     * @return log information
     */
    OperationLog getLogById(Long id);

    /**
     * Get operation log statistics
     *
     * @param startTime start time
     * @param endTime   end time
     * @return statistics data
     */
    Map<String, Object> getStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Delete operation logs older than the specified date
     *
     * @param beforeDate cutoff date
     * @return number of deleted rows
     */
    Integer deleteLogsBeforeDate(LocalDateTime beforeDate);
}
