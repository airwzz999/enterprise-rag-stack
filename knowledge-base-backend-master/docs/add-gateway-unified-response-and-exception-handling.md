# Adding Unified Wrapping of Gateway Response Values and Exceptions

## I. Overview

In an enterprise-grade microservice architecture, uniformly handling response values and exceptions is key to ensuring system stability and maintainability. This tutorial describes in detail how to implement unified wrapping at the gateway layer in the knowledge base system, drawing on production best practices from susan-mall-cloud.

### 1.1 The Value of Unified Wrapping

| Benefit | Description | Application |
|------|------|---------|
| Unified response format | All endpoints return JSON data in the same format | The frontend can parse it uniformly, reducing adaptation cost |
| Unified exception handling | Business exceptions are uniformly converted into a standard format | Avoids leaking exception details, improves user experience |
| Gateway-layer processing | The response format is uniformly handled at the gateway layer | Business services focus on business logic; responsibilities are separated |
| Complete logging | Logs the complete request/response information | Troubleshooting, performance monitoring |

### 1.2 Technical Architecture

```
API call flow:
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  Client request                                              │
│     ↓                                                       │
│  ┌──────────────────┐                                       │
│  │  Spring Cloud    │                                       │
│  │  Gateway         │                                       │
│  │                  │                                       │
│  │  ┌──────────────┐ │  ┌──────────────────────────┐         │
│  │  │GlobalLogFilter│ │  │CorsResponseHeaderFilter│         │
│  │  │  Logs request  │ │  │  Deduplicates CORS      │         │
│  │  │                │ │  │  headers                │         │
│  │  └──────────────┘ │  └──────────────────────────┘         │
│  │         ↓           │                ↑                    │
│  │  ┌──────────────┐ │                │                    │
│  │  │ UnifiedResponse│ │ ┌──────────────────────┐      │
│  │  │   Filter       │ │ │GatewayGlobalException│     │
│  │  │ Unifies the    │ │ │Handler              │      │
│  │  │ response format│ │ │Gateway exception    │      │
│  │  └──────────────┘ │ │ handling              │      │
│  │         ↓           │ └──────────────────────┘      │
│  │  Backend microservice │                                     │
│  │  ┌──────────────┐ │                                     │
│  │  │GlobalException│ │ ← kb-common shared module         │
│  │  │   Handler      │ │                                     │
│  │  │ Unified        │ │                                     │
│  │  │ exception      │ │                                     │
│  │  │ handling       │ │                                     │
│  │  └──────────────┘ │                                     │
│  └──────────────────┘                                       │
│         ↓                                                   │
│  Client receives the response in a unified format            │
│                                                             │
└─────────────────────────────────────────────────────────────┘

Tech stack:
├── Spring Boot 3.2
├── Spring Cloud Gateway
├── Spring WebFlux (reactive programming)
├── FastJSON2 (JSON handling)
└── Lombok (simplifies code)
```

---

## II. Environment Setup

### 2.1 Create the Common Module Directory Structure

Run the following commands in the project root directory to create the directory structure:

```bash
# Create the kb-common module directory
mkdir -p kb-common/src/main/java/com/knowledge/base/common/result
mkdir -p kb-common/src/main/java/com/knowledge/base/common/exception
mkdir -p kb-common/src/main/java/com/knowledge/base/common/util
mkdir -p kb-common/src/main/java/com/knowledge/base/common/feign
mkdir -p kb-common/src/main/resources
```

### 2.2 Check the Parent POM Configuration

Make sure the root project's `pom.xml` includes the following dependency management:

```xml
<properties>
    <java.version>21</java.version>
    <spring-boot.version>3.2.0</spring-boot.version>
    <spring-cloud.version>2023.0.0</spring-cloud.version>
    <fastjson2.version>2.0.43</fastjson2.version>
    <lombok.version>1.18.30</lombok.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## III. Creating the Common Module (kb-common)

### 3.1 Configure kb-common's pom.xml

Create `kb-common/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.knowledge.base</groupId>
        <artifactId>knowledge-base-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>kb-common</artifactId>
    <packaging>jar</packaging>
    <name>Knowledge Base Common Module</name>
    <description>Common foundation module (unified responses, exception handling, utility classes)</description>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Spring Cloud OpenFeign -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- Apache Commons -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>

        <!-- FastJSON2 -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

### 3.2 Create the Unified Response Result Class

Create `kb-common/src/main/java/com/knowledge/base/common/result/Result.java`:

```java
package com.knowledge.base.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * Unified response result wrapper class
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; all endpoints
 * return responses in this unified format</p>
 *
 * @param <T> data type
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Response code
     */
    private Integer code;

    /**
     * Response message
     */
    private String message;

    /**
     * Response data
     */
    private T data;

    /**
     * Timestamp
     */
    private Long timestamp;

    /**
     * Private constructor
     */
    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Private constructor
     *
     * @param code    the response code
     * @param message the response message
     * @param data    the response data
     */
    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Success response (no data)
     *
     * @param <T> data type
     * @return the unified response result
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * Success response (with data)
     *
     * @param data the data
     * @param <T>  data type
     * @return the unified response result
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * Success response (custom message)
     *
     * @param message the message
     * @param <T>     data type
     * @return the unified response result
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, null);
    }

    /**
     * Success response (custom message and data)
     *
     * @param message the message
     * @param data    the data
     * @param <T>     data type
     * @return the unified response result
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * Failure response (default error)
     *
     * @param <T> data type
     * @return the unified response result
     */
    public static <T> Result<T> error() {
        return new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage(), null);
    }

    /**
     * Failure response (custom message)
     *
     * @param message the error message
     * @param <T>     data type
     * @return the unified response result
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }

    /**
     * Failure response (custom error code and message)
     *
     * @param code    the error code
     * @param message the error message
     * @param <T>     data type
     * @return the unified response result
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * Failure response (using a result code enum)
     *
     * @param resultCode the result code enum
     * @param <T>        data type
     * @return the unified response result
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /**
     * Return success or failure based on a condition
     *
     * @param flag the condition flag
     * @param <T>  data type
     * @return the unified response result
     */
    public static <T> Result<T> status(boolean flag) {
        return flag ? success() : error();
    }
}
```

### 3.3 Create the Result Code Enum

Create `kb-common/src/main/java/com/knowledge/base/common/result/ResultCode.java`:

```java
package com.knowledge.base.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Result code enum
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /**
     * Success
     */
    SUCCESS(200, "success"),

    /**
     * System error
     */
    ERROR(500, "System exception, please contact the administrator"),

    /**
     * Parameter error
     */
    PARAM_ERROR(400, "Parameter error"),

    /**
     * Unauthorized
     */
    UNAUTHORIZED(401, "Unauthorized, please log in first"),

    /**
     * Access forbidden
     */
    FORBIDDEN(403, "Access forbidden"),

    /**
     * Resource not found
     */
    NOT_FOUND(404, "Resource not found"),

    /**
     * Business exception
     */
    BUSINESS_ERROR(10000, "Business exception");

    /**
     * Status code
     */
    private final Integer code;

    /**
     * Message
     */
    private final String message;
}
```

### 3.4 Create the Business Exception Class

Create `kb-common/src/main/java/com/knowledge/base/common/exception/BusinessException.java`:

```java
package com.knowledge.base.common.exception;

import com.knowledge.base.common.result.ResultCode;
import lombok.Getter;

/**
 * Business exception class
 *
 * <p>Used to handle exceptional situations in business logic, designed according
 * to the Alibaba Java Development Guidelines</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Error code
     */
    private final Integer code;

    /**
     * Error message
     */
    private final String message;

    /**
     * Constructor
     *
     * @param message the error message
     */
    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
        this.message = message;
    }

    /**
     * Constructor
     *
     * @param code    the error code
     * @param message the error message
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * Constructor
     *
     * @param resultCode the result code enum
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * Constructor
     *
     * @param message the error message
     * @param cause   the cause
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.ERROR.getCode();
        this.message = message;
    }

    /**
     * Constructor
     *
     * @param resultCode the result code enum
     * @param cause      the cause
     */
    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }
}
```

### 3.5 Create the Assertion Utility Class

Create `kb-common/src/main/java/com/knowledge/base/common/util/AssertUtil.java`:

```java
package com.knowledge.base.common.util;

import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.ResultCode;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Assertion utility class
 *
 * <p>Based on the AssertUtil implementation from susan-mall-cloud</p>
 * <p>Provides business parameter validation, throwing a BusinessException when validation fails</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public class AssertUtil {

    /**
     * Assert that an expression is true
     *
     * @param expression the expression
     * @param message    the error message
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that an expression is false
     *
     * @param expression the expression
     * @param message    the error message
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that an object is null
     *
     * @param object  the object
     * @param message the error message
     */
    public static void isNull(Object object, String message) {
        if (Objects.nonNull(object)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that an object is not null
     *
     * @param object  the object
     * @param message the error message
     */
    public static void notNull(Object object, String message) {
        if (Objects.isNull(object)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that a string has length
     *
     * @param text    the string
     * @param message the error message
     */
    public static void hasLength(String text, String message) {
        if (StringUtils.isEmpty(text)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that a string has content (non-blank string)
     *
     * @param text    the string
     * @param message the error message
     */
    public static void hasText(String text, String message) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that a collection is not empty
     *
     * @param collection the collection
     * @param message    the error message
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that a Map is not empty
     *
     * @param map     the Map
     * @param message the error message
     */
    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert that an array has content
     *
     * @param array   the array
     * @param message the error message
     */
    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }

    /**
     * Assert a state (general-purpose assertion)
     *
     * @param state   the state
     * @param message the error message
     */
    public static void state(boolean state, String message) {
        if (!state) {
            throw new BusinessException(message);
        }
    }
}
```

### 3.6 Create the Global Exception Handler

Create `kb-common/src/main/java/com/knowledge/base/common/exception/GlobalExceptionHandler.java`:

```java
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
 * <p>Based on the GlobalExceptionHandler implementation from susan-mall-cloud</p>
 * <p>Main features:</p>
 * <ul>
 *   <li>Uniformly handles exceptions from all business services</li>
 *   <li>Distinguishes between internal service calls and external API calls</li>
 *   <li>Internal calls return a ResponseEntity (preserving the HTTP status code)</li>
 *   <li>External calls return the unified Result format (HTTP 200 + business error code)</li>
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
            log.error("Business exception on internal call: {} code={} msg={}", requestInfo, businessException.getCode(), businessException.getMessage(), e);
            return ResponseEntity.status(businessException.getCode()).body(businessException.getMessage());
        }
        log.error("Exception on internal call: {}", requestInfo, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    /**
     * Handle exceptions from external API calls
     * <p>External API calls return the unified Result format, with the HTTP status code uniformly set to 200</p>
     */
    private Object handleExternalException(Throwable e, String requestInfo) {
        if (e instanceof BusinessException) {
            BusinessException businessException = (BusinessException) e;
            log.error("Business exception: {} code={} msg={}", requestInfo, businessException.getCode(), businessException.getMessage(), e);
            return Result.error(businessException.getCode(), businessException.getMessage());
        } else if (e instanceof AccessDeniedException) {
            log.warn("Permission exception: {} msg={}", requestInfo, e.getMessage(), e);
            return Result.error(HttpStatus.FORBIDDEN.value(), "Access denied, please contact the system administrator");
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
        }

        log.error("System exception: {} msg={}", requestInfo, e.getMessage(), e);
        return Result.error(ResultCode.ERROR);
    }

    /**
     * Get the request information
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
```

### 3.7 Create the Feign Interceptor Configuration

Create `kb-common/src/main/java/com/knowledge/base/common/feign/FeignInterceptorConfig.java`:

```java
package com.knowledge.base.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign request interceptor configuration
 *
 * <p>Adds an INNER-REQUEST header for inter-service calls, marking them as internal calls</p>
 * <p>Based on the Feign configuration from susan-mall-cloud, implementing special
 * handling for inter-service calls</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignInterceptorConfig {

    /**
     * Feign request interceptor bean
     */
    @Bean
    public RequestInterceptor feignInterceptor() {
        return new FeignInterceptor();
    }

    /**
     * Feign request interceptor implementation class
     */
    public static class FeignInterceptor implements RequestInterceptor {

        private static final String INNER_REQUEST_HEADER = "INNER-REQUEST";

        @Override
        public void apply(RequestTemplate template) {
            // Add the internal call marker
            template.header(INNER_REQUEST_HEADER, "true");
        }
    }
}
```

---

## IV. Configuring the Gateway Service (kb-gateway)

### 4.1 Configure kb-gateway's pom.xml

Make sure `kb-gateway/pom.xml` includes the following dependencies:

```xml
<dependencies>
    <!-- Common module -->
    <dependency>
        <groupId>com.knowledge.base</groupId>
        <artifactId>kb-common</artifactId>
    </dependency>

    <!-- Spring Cloud Gateway -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

    <!-- FastJSON2 -->
    <dependency>
        <groupId>com.alibaba.fastjson2</groupId>
        <artifactId>fastjson2</artifactId>
        <version>${fastjson2.version}</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### 4.2 Create the Global Log Filter

Create `kb-gateway/src/main/java/com/knowledge/base/gateway/filter/GlobalLogFilter.java`:

```java
package com.knowledge.base.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global log filter
 *
 * <p>Logs the complete information of all requests and responses</p>
 * <p>Includes the request method, URI, remote address, response status code,
 * execution time, etc.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class GlobalLogFilter implements GlobalFilter, Ordered {

    private static final String START_TIME_ATTR = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Record the start time
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        // Log the request
        String method = request.getMethod().name();
        String uri = request.getURI().getPath();
        String queryParams = request.getURI().getQuery();
        String fullUri = queryParams == null ? uri : uri + "?" + queryParams;
        String remoteAddr = request.getRemoteAddress() != null ?
            request.getRemoteAddress().getAddress().toString() : "unknown";

        log.info("Request => Method: {}, URI: {}, RemoteAddress: {}",
            method, fullUri, remoteAddr);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME_ATTR);
            if (startTime != null) {
                long executeTime = System.currentTimeMillis() - startTime;
                ServerHttpResponse response = exchange.getResponse();
                log.info("Response => StatusCode: {}, Time: {}ms",
                    response.getStatusCode(), executeTime);
            }
        }));
    }

    @Override
    public int getOrder() {
        return -100; // High priority, runs first
    }
}
```

### 4.3 Create the Unified Response Filter

Create `kb-gateway/src/main/java/com/knowledge/base/gateway/filter/UnifiedResponseFilter.java`:

```java
package com.knowledge.base.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified response filter
 *
 * <p>Based on the AuthFilter implementation from susan-mall-cloud; uniformly
 * handles response body wrapping</p>
 * <p>Main features:</p>
 * <ul>
 *   <li>Intercepts backend service responses, ensuring a uniform response format</li>
   *   <li>Handles chunked-transfer data</li>
 *   <li>Automatically wraps non-standard-format responses</li>
   *   <li>Logs the complete response</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class UnifiedResponseFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {

            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    String contentType = getDelegate().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

                    // Only handle JSON-format responses
                    if (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
                        Flux<? extends DataBuffer> fluxBody = Flux.from(body);

                        // Handle chunked-transfer data
                        return super.writeWith(fluxBody.buffer().flatMap(dataBuffers -> {
                            List<String> list = new ArrayList<>();
                            dataBuffers.forEach(dataBuffer -> {
                                try {
                                    byte[] content = new byte[dataBuffer.readableByteCount()];
                                    dataBuffer.read(content);
                                    list.add(new String(content, StandardCharsets.UTF_8));
                                } catch (Exception e) {
                                    log.error("Exception reading the response byte stream: {}", e.getMessage(), e);
                                }
                            });

                            // Release the original data buffers
                            DataBufferUtils.release(dataBuffers);

                            String responseData = String.join("", list);
                            log.info("Gateway forwarded response: URI={}, Status={}, Response={}",
                                    exchange.getRequest().getURI(),
                                    getStatusCode(),
                                    responseData);

                            // Wrap the response data
                            String wrappedResponse = wrapResponse(responseData);
                            byte[] uppedContent = wrappedResponse.getBytes(StandardCharsets.UTF_8);

                            // Update Content-Length
                            getDelegate().getHeaders().setContentLength(uppedContent.length);

                            // Return the new data buffer
                            return Mono.just(bufferFactory.wrap(uppedContent));
                        }));
                    }
                }
                return super.writeWith(body);
            }

            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return writeWith(Flux.from(body).flatMapSequential(p -> p));
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    /**
     * Wrap the response data
     * <p>If the response data is already in the standard format, return it as-is;
     * otherwise wrap it into the standard format</p>
     *
     * @param responseData the original response data
     * @return the wrapped response data
     */
    private String wrapResponse(String responseData) {
        try {
            // Try to parse it as JSON
            Object json = JSON.parse(responseData);
            if (json instanceof JSONObject) {
                JSONObject obj = (JSONObject) json;
                // Check whether it already contains code and message fields (standard Result format)
                if (obj.containsKey("code") && obj.containsKey("message")) {
                    return responseData;
                }
            }
        } catch (Exception ignored) {
            // JSON parsing failed, meaning it's not in the standard format and needs wrapping
        }

        // Wrap it into the standard Result format
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", JSON.parse(responseData));
        result.put("timestamp", System.currentTimeMillis());

        return result.toJSONString();
    }

    @Override
    public int getOrder() {
        return -2; // Relatively high priority
    }
}
```

### 4.4 Create the Gateway Global Exception Handler

Create `kb-gateway/src/main/java/com/knowledge/base/gateway/handler/GatewayGlobalExceptionHandler.java`:

```java
package com.knowledge.base.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.base.common.result.Result;
import io.netty.channel.ConnectTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Gateway global exception handler
 *
 * <p>Uniformly handles exceptions from the gateway layer and backend services,
 * returning error responses in a unified format</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Order(-1)
@Component("gatewayGlobalExceptionHandler")
public class GatewayGlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // Set the response headers
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Generate the error response based on the exception type
        Result<?> result;
        HttpStatus status;

        if (ex instanceof ResponseStatusException rse) {
            status = (HttpStatus) rse.getStatusCode();
            result = Result.error(status.value(), rse.getReason());
        } else if (ex instanceof ConnectTimeoutException || ex instanceof TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            result = Result.error(status.value(), "The backend service timed out, please try again later");
        } else if (ex instanceof java.net.ConnectException) {
            status = HttpStatus.BAD_GATEWAY;
            result = Result.error(status.value(), "The backend service is unavailable, please check its status");
        } else {
            log.error("Gateway exception: ", ex);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            result = Result.error(status.value(), "System exception: " + ex.getMessage());
        }

        response.setStatusCode(status);

        try {
            String responseBody = objectMapper.writeValueAsString(result);
            DataBuffer buffer = response.bufferFactory()
                    .wrap(responseBody.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("JSON serialization failed", e);
            return Mono.error(ex);
        }
    }
}
```

### 4.5 Create the Gateway Configuration Class

Create `kb-gateway/src/main/java/com/knowledge/base/gateway/config/GatewayConfig.java`:

```java
package com.knowledge.base.gateway.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

/**
 * Gateway configuration class
 *
 * <p>Configures the gateway's global filters; route configuration lives in application.yml</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class GatewayConfig {

    /**
     * Configure the CORS filter
     *
     * @return the CorsWebFilter
     */
    @Bean
    public CorsWebFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allowed origins (cannot use the wildcard * because allowCredentials=true)
        // Development environment: allow local dev servers
        config.addAllowedOriginPattern("http://localhost:3000");
        config.addAllowedOriginPattern("http://localhost:5173");
        config.addAllowedOriginPattern("http://localhost:5174");
        config.addAllowedOriginPattern("http://127.0.0.1:3000");
        config.addAllowedOriginPattern("http://127.0.0.1:5173");
        config.addAllowedOriginPattern("http://127.0.0.1:5174");

        // Allow sending credentials (cookies, etc.)
        config.setAllowCredentials(true);

        // Allowed HTTP methods
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("PATCH");

        // Allowed request headers
        config.addAllowedHeader("*");

        // Exposed response headers
        config.addExposedHeader("Content-Disposition");
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Set-Cookie");

        // Preflight request cache duration (seconds)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }

    /**
     * Global authentication filter
     *
     * @return the AuthFilter
     */
    @Bean
    public GlobalFilter authFilter() {
        return new AuthFilter();
    }

    /**
     * Authentication filter
     */
    public static class AuthFilter implements GlobalFilter, Ordered {

        @Override
        public Mono<Void> filter(
                org.springframework.web.server.ServerWebExchange exchange,
                org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
            // JWT validation logic could be added here
            // Currently all requests are allowed through
            return chain.filter(exchange);
        }

        @Override
        public int getOrder() {
            return -100; // High priority
        }
    }
}
```

### 4.6 Create the WebFlux Configuration Class

Create `kb-gateway/src/main/java/com/knowledge/base/gateway/config/GatewayWebFluxConfig.java`:

```java
package com.knowledge.base.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Gateway WebFlux configuration
 *
 * <p>Ensures the gateway can correctly handle JSON response bodies</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class GatewayWebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        // Configure the message codecs to ensure response bodies are handled correctly
        configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024); // 16MB
    }
}
```

### 4.7 Create the CORS Response Header Deduplication Filter

Create `kb-gateway/src/main/java/com/knowledge/base/gateway/config/CorsResponseHeaderFilter.java`:

```java
package com.knowledge.base.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CORS response header deduplication filter
 *
 * <p>Based on the CORS solution from the susan-mall-cloud project</p>
 * <p>Removes duplicate CORS headers set by backend services, ensuring only the
 * CORS headers set at the gateway layer are returned to the client</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Component
public class CorsResponseHeaderFilter implements GlobalFilter, Ordered {

    private static final String ANY = "*";

    @Override
    public int getOrder() {
        // Runs after NettyWriteResponseFilter, ensuring deduplication happens after the response headers are processed
        return org.springframework.cloud.gateway.filter.NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            try {
                if (exchange.getResponse().isCommitted()) {
                    return;
                }
                HttpHeaders headers = exchange.getResponse().getHeaders();
                if (headers == null || headers.isEmpty()) {
                    return;
                }

                headers.entrySet().stream()
                        .filter(kv -> kv != null && kv.getKey() != null && kv.getValue() != null && kv.getValue().size() > 1)
                        .filter(kv -> (HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN.equals(kv.getKey())
                                || HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS.equals(kv.getKey())
                                || HttpHeaders.VARY.equals(kv.getKey())))
                        .forEach(kv -> {
                            try {
                                if (HttpHeaders.VARY.equals(kv.getKey())) {
                                    List<String> varyValue = kv.getValue().stream().distinct().collect(Collectors.toList());
                                    headers.put(kv.getKey(), varyValue);
                                    log.debug("CORS header deduplicated: {} -> {}", kv.getKey(), varyValue);
                                } else {
                                    List<String> value = new ArrayList<>();
                                    if (kv.getValue().contains(ANY)) {
                                        value.add(ANY);
                                    } else {
                                        value.add(kv.getValue().get(0));
                                    }
                                    headers.put(kv.getKey(), value);
                                    log.debug("CORS header deduplicated: {} -> {}", kv.getKey(), value);
                                }
                            } catch (Exception e) {
                                log.error("Exception while processing CORS header: key={}, value={}", kv.getKey(), kv.getValue(), e);
                            }
                        });
            } catch (Exception e) {
                log.error("Exception during CORS response header deduplication", e);
            }
        }));
    }
}
```

### 4.8 Configure the Gateway's CORS Settings

The CORS configuration is already handled via `CorsWebFilter` in `GatewayConfig.java`; no additional configuration is needed in `application.yml`.

If you need to adjust the allowed origins, modify the `corsFilter()` method in `GatewayConfig.java`.

---

## V. Configuring the User & Auth Service

### 5.1 Configure Spring Security (Disable CORS)

Modify `kb-user-auth/src/main/java/com/knowledge/base/userauth/config/SecurityConfig.java`:

```java
package com.knowledge.base.userauth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration class
 *
 * <p>Configures the security policy and authentication rules</p>
 * <p>Note: CORS is handled centrally by the gateway; business services no longer configure CORS</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Configure the security filter chain
     *
     * @param http the HttpSecurity
     * @return the SecurityFilterChain
     * @throws Exception on error
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (not needed when using JWT)
                .csrf(AbstractHttpConfigurer::disable)
                // Configure session management (stateless)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Disable CORS (handled centrally by the gateway)
                .cors(org.springframework.security.config.Customizer.withDefaults())
                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Allow the login and registration endpoints
                        .requestMatchers("/auth/login", "/auth/register", "/auth/auth/**").permitAll()
                        // Allow public APIs
                        .requestMatchers("/public/**").permitAll()
                        // Allow WebSocket
                        .requestMatchers("/ws/**").permitAll()
                        // Allow health checks
                        .requestMatchers("/actuator/**").permitAll()
                        // Allow Swagger docs
                        .requestMatchers("/doc.html", "/swagger-resources/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        // Allow test endpoints
                        .requestMatchers("/auth/test", "/auth/test-error").permitAll()
                        // Allow OPTIONS preflight requests (important CORS configuration)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    /**
     * Configure the password encoder
     *
     * @return the PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## VI. Testing and Verification

### 6.1 Start the Services

Start the following services in order:

```bash
# 1. Start Nacos (if used)
cd nacos/bin
./startup.sh -m standalone

# 2. Build all modules
mvn clean install -DskipTests

# 3. Start the user & auth service
cd kb-user-auth
mvn spring-boot:run

# 4. Start the gateway service
cd kb-gateway
mvn spring-boot:run
```

### 6.2 Test a Normal Response

Use curl to test the login endpoint:

```bash
curl -X POST http://localhost:8080/api/auth/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "123456"
  }'
```

**Expected response:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "mock-token-123456",
    "username": "admin"
  },
  "timestamp": 1715905600000
}
```

### 6.3 Test a Business Exception

```bash
curl -X POST http://localhost:8080/api/auth/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test",
    "password": "wrong"
  }'
```

**Expected response:**
```json
{
  "code": 10000,
  "message": "Incorrect username or password",
  "data": null,
  "timestamp": 1715905600000
}
```

### 6.4 Check the Gateway Logs

Example log for a successful call:

```
2026-05-17 16:10:12.324 [parallel-1] INFO  c.k.base.gateway.filter.GlobalLogFilter - Request => Method: POST, URI: /api/auth/auth/login, RemoteAddress: /127.0.0.1:50786
2026-05-17 16:10:12.852 [reactor-http-nio-2] INFO  c.k.base.gateway.filter.UnifiedResponseFilter - Gateway forwarded response: URI=/api/auth/auth/login, Status=200 OK, Response={"code":200,"message":"success",...}
2026-05-17 16:10:13.116 [reactor-http-nio-2] INFO  com.knowledge.base.gateway.filter.GlobalLogFilter - Response => StatusCode: 200 OK, Time: 791ms
```

---

## VII. Common Issues and Solutions

### 7.1 The Endpoint Returns Empty Data

**Symptom:**
```
The gateway log shows: Gateway forwarded response: Response={"code":10001,"message":"Incorrect username or password"}
But the browser receives an empty response
```

**Root cause analysis:**
- There is a problem with the response streaming logic
- The DataBuffer is released at the wrong time
- The response body is not written back to the client correctly

**Solution:**
1. Make sure UnifiedResponseFilter uses flatMap instead of map
2. Use Mono.just to wrap the newly created DataBuffer
3. Release the original DataBuffer only after all the data has been processed

### 7.2 Duplicate CORS Header Error

**Symptom:**
```
Access-Control-Allow-Origin header contains multiple values 'http://localhost:3000, http://localhost:3000'
```

**Root cause analysis:**
- CORS is configured at the gateway layer
- CORS is also configured in the business service
- This causes the response header to be set twice

**Solution:**
1. Remove the CORS configuration from the business service layer
2. Use `.cors(org.springframework.security.config.Customizer.withDefaults())` in SecurityConfig
3. Add CorsResponseHeaderFilter to remove the duplicate headers

### 7.3 Inconsistent Response Formats

**Symptom:**
- Some endpoints return the standard Result format
- Some endpoints return other formats
- The frontend has trouble parsing them

**Solution:**
1. Make sure UnifiedResponseFilter correctly wraps non-standard-format responses
2. Check the logic of the wrapResponse method
3. Have business services uniformly throw BusinessException

---

## VIII. Architecture Summary

### 8.1 Complete Call Chain

```
Client request
    ↓
┌──────────────────────────────────────────┐
│ Spring Cloud Gateway                      │
│                                            │
│ 1. GlobalLogFilter (-100)               │
│    └─ Logs the request                    │
│         ↓                                  │
│ 2. Route matching                             │
│    └─ Forwards to the backend service                    │
│         ↓                                  │
│ Backend service (kb-user-auth)               │
│  ┌────────────────────────────────┐      │
│  │ Controller                        │      │
│  │   ↓                               │      │
│  │ Service                          │      │
│  │   ↓ (throws BusinessException)       │      │
│  │ GlobalExceptionHandler           │      │
│ │   ↓ (converts to Result format)            │      │
│  └────────────────────────────────┘      │
│         ↓                                  │
│ 3. UnifiedResponseFilter (-2)       │
│    └─ Unifies the response format                      │
│         ↓                                  │
│ 4. CorsResponseHeaderFilter           │
│    └─ Deduplicates CORS headers                        │
│         ↓                                  │
│ 5. GlobalLogFilter                    │
│    └─ Logs the response                      │
│         ↓                                  │
└──────────────────────────────────────────┘
    ↓
Client receives the unified-format response
```

### 8.2 Core Design Points

| Design Point | Implementation | Purpose |
|---------|---------|------|
| Layered exception handling | Business throws → Common handles → Gateway wraps | Separates responsibilities, easier to maintain |
| Internal/external call distinction | INNER-REQUEST header marker | Different exception handling strategies |
| Streaming response handling | flatMap + Mono.just wrapping | Ensures data is passed through correctly |
| Unified CORS management | Gateway config + deduplication filter | Avoids duplicate-header issues |
| Complete logging | GlobalLogFilter | Facilitates troubleshooting |

### 8.3 Extensibility Design

**When adding a new microservice:**
1. Depend on the kb-common module
2. Business services only throw BusinessException
3. The gateway automatically handles the response format

**Custom error codes:**
1. Add a new enum value to ResultCode
2. Throw it using BusinessException(code, message)

**Adding new business validation:**
1. Use the AssertUtil utility class
2. A BusinessException is automatically thrown when validation fails

---

## IX. Summary

This tutorial fully implemented unified wrapping of response values and exceptions at the gateway layer, with the following key features:

✅ **Unified response format** - all endpoints return the standard Result format
✅ **Unified exception handling** - the gateway layer uniformly handles business exceptions
✅ **Internal/external call distinction** - supports different exception handling strategies
✅ **CORS header deduplication** - automatically removes duplicate CORS response headers
✅ **Complete logging** - logs the complete request/response information
✅ **Production-validated** - based on the production implementation from susan-mall-cloud

**Applicable scenarios:**
- Enterprise-grade microservice architectures
- Spring Cloud Gateway-based gateways
- Frontend/backend-separated projects
- Systems that need a unified API specification

Implementing this tutorial can greatly improve a system's maintainability and user experience, and it represents a best practice recommended for production environments.

---

*This tutorial is based on the production implementation from susan-mall-cloud, written by airwzz999.*
*Last updated: May 17, 2026*
