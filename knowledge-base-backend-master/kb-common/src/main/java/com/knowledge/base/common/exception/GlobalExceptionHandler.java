package com.knowledge.base.common.exception;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Global exception handler
 *
 * <p>Referenced susan-mall-cloud's GlobalExceptionHandler implementation</p>
 * <p>Main responsibilities:</p>
 * <ul>
 *   <li>Uniformly handle exceptions across all business services</li>
 *   <li>Distinguish between internal service calls and external API calls</li>
 *   <li>Internal calls return a ResponseEntity (preserving the HTTP status code)</li>
 *   <li>External calls return the unified Result format (HTTP 200 + a business error code)</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Unified exception handling entry point
     */
    @ExceptionHandler(Throwable.class)
    public Object handleException(Throwable e) {
        String requestInfo = getRequestInfo();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        // Check whether this is an internal service call
        if (Objects.nonNull(requestAttributes)) {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
            HttpServletRequest request = servletRequestAttributes.getRequest();
            if (StringUtils.isNotEmpty(request.getHeader("INNER-REQUEST"))) {
                return handleInternalException(e, requestInfo);
            }
        }

        // External API call
        return handleExternalException(e, requestInfo);
    }

    /**
     * Handle exceptions from internal service calls
     * <p>Internal service calls return a ResponseEntity, preserving the HTTP status code</p>
     */
    private Object handleInternalException(Throwable e, String requestInfo) {
        if (e instanceof BusinessException) {
            BusinessException businessException = (BusinessException) e;
            log.error("Business exception in internal call: {} code={} msg={}", requestInfo, businessException.getCode(), businessException.getMessage(), e);
            return ResponseEntity.status(businessException.getCode()).body(businessException.getMessage());
        }
        log.error("Internal call exception: {}", requestInfo, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    /**
     * Handle exceptions from external API calls
     * <p>External API calls return the unified Result format, with the HTTP status code always 200</p>
     */
    private Object handleExternalException(Throwable e, String requestInfo) {
        if (e instanceof BusinessException) {
            BusinessException businessException = (BusinessException) e;
            log.error("Business exception: {} code={} msg={}", requestInfo, businessException.getCode(), businessException.getMessage(), e);
            return Result.error(businessException.getCode(), businessException.getMessage());
        } else if (e instanceof AccessDeniedException) {
            log.warn("Permission exception: {} msg={}", requestInfo, e.getMessage(), e);
            return Result.error(HttpStatus.FORBIDDEN.value(), "Access denied, please contact your system administrator");
        } else if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            log.error("Parameter validation exception: {} {}", requestInfo, errorMsg);
            return Result.error(ResultCode.PARAM_ERROR.getCode(), errorMsg);
        } else if (e instanceof BindException) {
            BindException ex = (BindException) e;
            String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            log.error("Parameter binding exception: {} {}", requestInfo, errorMsg);
            return Result.error(ResultCode.PARAM_ERROR.getCode(), errorMsg);
        } else if (e instanceof IllegalArgumentException) {
            log.error("Illegal argument exception: {} {}", requestInfo, e.getMessage(), e);
            return Result.error(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
        } else if (e instanceof IllegalStateException && e.getMessage() != null
                && (e.getMessage().contains("not logged in") || e.getMessage().contains("login information is incomplete"))) {
            log.warn("User authentication exception: {} msg={}", requestInfo, e.getMessage());
            return Result.error(ResultCode.UNAUTHORIZED.getCode(), e.getMessage());
        }

        log.error("System exception: {} msg={}", requestInfo, e.getMessage(), e);
        return Result.error(ResultCode.ERROR);
    }

    /**
     * Get request info
     */
    private static String getRequestInfo() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes) {
            HttpServletRequest req = ((ServletRequestAttributes) attrs).getRequest();
            String method = req.getMethod();
            String uri = req.getRequestURI();
            String query = req.getQueryString();
            String ip = req.getRemoteAddr();
            String fullUri = query == null ? uri : uri + "?" + query;
            return "method=" + method + " uri=" + fullUri + " ip=" + ip;
        }
        return "";
    }
}
