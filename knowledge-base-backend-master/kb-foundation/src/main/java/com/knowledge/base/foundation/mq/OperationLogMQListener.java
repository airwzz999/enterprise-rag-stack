package com.knowledge.base.foundation.mq;

import com.knowledge.base.common.event.OperationLogEventDTO;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Operation log MQ listener
 *
 * <p>Consumes operation log events from all business services and writes them into
 * the kb_operation_log table</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class OperationLogMQListener {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * Consume an operation log event
     */
    @RabbitListener(queues = "#{@operationLogQueue.name}")
    public void handleOperationLogEvent(OperationLogEventDTO event) {
        try {
            log.debug("Received an operation log event: module={}, operationType={}, userId={}",
                    event.getModule(), event.getOperationType(), event.getUserId());

            long id = SnowflakeIdGenerator.getInstance().nextId();
            jdbcTemplate.update(
                    "INSERT INTO kb_operation_log " +
                            "(id, module, operation_type, operation_desc, request_method, request_url, " +
                            "request_params, user_id, username, ip_address, user_agent, " +
                            "execute_time, status, error_msg, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                    id,
                    event.getModule(),
                    event.getOperationType(),
                    event.getOperationDesc(),
                    event.getRequestMethod(),
                    event.getRequestUrl(),
                    event.getRequestParams(),
                    event.getUserId(),
                    event.getUsername(),
                    event.getIpAddress(),
                    event.getUserAgent(),
                    event.getExecuteTime(),
                    event.getStatus(),
                    event.getErrorMsg()
            );

            log.debug("Operation log written successfully: id={}, module={}, operationType={}", id, event.getModule(), event.getOperationType());
        } catch (Exception e) {
            log.error("Failed to process operation log event: module={}, operationType={}, userId={}, error={}",
                    event.getModule(), event.getOperationType(), event.getUserId(), e.getMessage(), e);
            // Do not rethrow, to avoid endless message retries
        }
    }
}
