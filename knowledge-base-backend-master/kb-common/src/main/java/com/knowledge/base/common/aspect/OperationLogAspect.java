package com.knowledge.base.common.aspect;

import com.alibaba.fastjson2.JSON;
import com.knowledge.base.common.annotation.OperationLog;
import com.knowledge.base.common.config.InstanceIdentifier;
import com.knowledge.base.common.event.OperationLogEventDTO;
import com.knowledge.base.common.utils.UserContextUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Operation log AOP aspect
 *
 * <p>Intercepts methods annotated with @OperationLog, records operation logs, and publishes them
 * asynchronously via RabbitMQ, which the kb-foundation service consumes and writes to the kb_operation_log table</p>
 *
 * <p>Only active in a Servlet web environment; automatically disabled in WebFlux (e.g. the Gateway)</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class OperationLogAspect {

    private static final String OPERATION_LOG_EXCHANGE = "kb.operationlog.exchange";

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private HttpServletRequest request;

    @Resource
    private InstanceIdentifier instanceIdentifier;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        OperationLogEventDTO event = buildBaseEvent(joinPoint, operationLog);

        try {
            Object result = joinPoint.proceed();
            long executeTime = System.currentTimeMillis() - startTime;

            event.setExecuteTime((int) executeTime);
            event.setStatus(1);

            publishEvent(event);
            log.debug("Operation log recorded successfully: module={}, operation={}, executeTime={}ms",
                    event.getModule(), event.getOperationType(), executeTime);

            return result;
        } catch (Throwable e) {
            long executeTime = System.currentTimeMillis() - startTime;

            event.setExecuteTime((int) executeTime);
            event.setStatus(0);
            event.setErrorMsg(truncate(e.getMessage(), 500));

            publishEvent(event);
            log.warn("Operation log recorded (failure): module={}, operation={}, executeTime={}ms, error={}",
                    event.getModule(), event.getOperationType(), executeTime, e.getMessage());

            throw e;
        }
    }

    /**
     * Build the base event object
     */
    private OperationLogEventDTO buildBaseEvent(ProceedingJoinPoint joinPoint, OperationLog operationLog) {
        // Get method info
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String requestMethod = request.getMethod();
        String requestUrl = request.getRequestURI();

        // Serialize request parameters (filtering out non-serializable types such as file streams, to avoid OOM)
        String requestParams = null;
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                requestParams = JSON.toJSONString(filterArgs(args));
            }
        } catch (Exception e) {
            requestParams = "[serialization failed]";
        }

        // Get user info
        Long userId = UserContextUtil.getUserId();
        String username = UserContextUtil.getUsername();

        // Get client info
        String ipAddress = getClientIp();
        String userAgent = request.getHeader("User-Agent");

        return OperationLogEventDTO.builder()
                .module(operationLog.module())
                .operationType(operationLog.operation())
                .operationDesc(operationLog.description())
                .requestMethod(requestMethod)
                .requestUrl(requestUrl)
                .requestParams(truncate(requestParams, 1000))
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(truncate(userAgent, 500))
                .build();
    }

    /**
     * Publish the event to RabbitMQ asynchronously
     * <p>Serializes to JSON directly and sends via send(), explicitly setting the ContentType to application/json,
     * bypassing RabbitTemplate's default Java serialization (SimpleMessageConverter),
     * to avoid serialization/deserialization exceptions caused by inconsistent MessageConverter configuration across services.</p>
     */
    private void publishEvent(OperationLogEventDTO event) {
        try {
            String json = JSON.toJSONString(event);
            byte[] body = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            String routingKey = "operationlog." + instanceIdentifier.getId() + ".record";
            rabbitTemplate.send(OPERATION_LOG_EXCHANGE, routingKey,
                    new org.springframework.amqp.core.Message(body,
                            new org.springframework.amqp.core.MessageProperties() {{
                                setContentType("application/json");
                                setHeader("__TypeId__", "com.knowledge.base.common.event.OperationLogEventDTO");
                            }}));
        } catch (Exception e) {
            log.error("Failed to publish operation log event: module={}, operation={}, error={}",
                    event.getModule(), event.getOperationType(), e.getMessage());
        }
    }

    /**
     * Get the real client IP
     */
    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Take the first IP when passing through multiple proxies
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Filter method arguments, replacing non-serializable types with lightweight descriptors to avoid OOM from large files
     */
    private Object[] filterArgs(Object[] args) {
        Object[] filtered = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            filtered[i] = filterArg(args[i]);
        }
        return filtered;
    }

    private Object filterArg(Object arg) {
        if (arg == null) return null;
        if (arg instanceof MultipartFile f) {
            return Map.of(
                    "type", "MultipartFile",
                    "originalFilename", f.getOriginalFilename() != null ? f.getOriginalFilename() : "unknown",
                    "size", f.getSize()
            );
        }
        if (arg instanceof HttpServletRequest) return "[HttpServletRequest]";
        if (arg instanceof HttpServletResponse) return "[HttpServletResponse]";
        if (arg instanceof InputStream) return "[InputStream]";
        if (arg instanceof OutputStream) return "[OutputStream]";
        return arg;
    }

    /**
     * Truncate a string to the specified length
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
