# Building the Project Skeleton

## I. Project Overview

This article will walk you through building the backend project skeleton for an enterprise knowledge base management system from scratch. This project uses the **Spring Boot 3.2 + MyBatis Plus + MySQL** tech stack, a **Maven multi-module** architecture, and follows microservice design principles.

### 1.1 Technology Choices

| Technology | Version | Description |
|------|------|------|
| JDK | 21 | Uses the latest long-term support version |
| Spring Boot | 3.2.0 | Core framework |
| Spring Cloud | 2023.0.0 | Microservice infrastructure |
| MyBatis Plus | 3.5.8 | ORM framework that simplifies database operations |
| MySQL | 8.0+ | Relational database |
| Redis | 7.2+ | Caching middleware |
| Knife4j | 4.3.0 | API documentation tool |
| Hutool | 5.8.24 | Java utility library |
| JWT | 0.12.3 | Token authentication |

### 1.2 Module Design

The project uses a multi-module architecture with 10 submodules in total:

```
knowledge-base-backend/
├── kb-common/           # Common module
├── kb-gateway/          # API Gateway
├── kb-user-auth/        # User & Auth Service
├── kb-document/         # Document Service
├── kb-search/           # Search Service
├── kb-file/            # File Service
├── kb-ai/              # AI Service
├── kb-graph/           # Knowledge Graph Service
├── kb-statistics/      # Statistics Service
└── kb-foundation/      # Foundation Service
```

---

## II. Environment Setup

### 2.1 Installing the Base Software

Make sure the following software is installed in your development environment:

```bash
# Check the Java version
java -version  # Should show 21 or higher

# Check the Maven version
mvn -v         # Should show 3.8+

# Check the MySQL version
mysql --version # Should show 8.0+
```

### 2.2 Recommended Development Tools

- **IDE**: IntelliJ IDEA 2023.2+
- **Database Tool**: Navicat / DBeaver
- **API Testing**: Postman / Apifox
- **Redis Tool**: RedisInsight

---

## III. Creating the Parent Project

### 3.1 Create the Maven Parent Project

First, create a Maven parent project to manage all the submodules:

```bash
# Create the project root directory
mkdir knowledge-base-backend
cd knowledge-base-backend
```

### 3.2 Configure the Parent pom.xml

Create a `pom.xml` file in the root directory:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.knowledge.base</groupId>
    <artifactId>knowledge-base-backend</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>Knowledge Base Backend</name>
    <description>Enterprise Knowledge Base System Backend</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Dependency version management -->
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        <mybatis-plus.version>3.5.8</mybatis-plus.version>
        <druid.version>1.2.20</druid.version>
        <hutool.version>5.8.24</hutool.version>
        <jwt.version>0.12.3</jwt.version>
        <knife4j.version>4.3.0</knife4j.version>
        <commons-lang3.version>3.14.0</commons-lang3.version>
        <commons-collections4.version>4.4</commons-collections4.version>
        <fastjson2.version>2.0.43</fastjson2.version>
    </properties>

    <modules>
        <module>kb-common</module>
        <module>kb-gateway</module>
        <module>kb-user-auth</module>
        <module>kb-document</module>
        <module>kb-search</module>
        <module>kb-file</module>
        <module>kb-ai</module>
        <module>kb-graph</module>
        <module>kb-statistics</module>
        <module>kb-foundation</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud dependencies -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- MyBatis Plus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-boot-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>

            <!-- Druid data source -->
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>druid-spring-boot-3-starter</artifactId>
                <version>${druid.version}</version>
            </dependency>

            <!-- Hutool utility library -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>

            <!-- JWT -->
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-impl</artifactId>
                <version>${jwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-jackson</artifactId>
                <version>${jwt.version}</version>
            </dependency>

            <!-- Knife4j API documentation -->
            <dependency>
                <groupId>com.github.xiaoymin</groupId>
                <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
                <version>${knife4j.version}</version>
            </dependency>

            <!-- Apache Commons -->
            <dependency>
                <groupId>org.apache.commons</groupId>
                <artifactId>commons-lang3</artifactId>
                <version>${commons-lang3.version}</version>
            </dependency>
            <dependency>
                <groupId>org.apache.commons</groupId>
                <artifactId>commons-collections4</artifactId>
                <version>${commons-collections4.version}</version>
            </dependency>

            <!-- FastJSON2 -->
            <dependency>
                <groupId>com.alibaba.fastjson2</groupId>
                <artifactId>fastjson2</artifactId>
                <version>${fastjson2.version}</version>
            </dependency>

            <!-- Internal module dependency -->
            <dependency>
                <groupId>com.knowledge.base</groupId>
                <artifactId>kb-common</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <configuration>
                        <excludes>
                            <exclude>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </exclude>
                        </excludes>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>

    <repositories>
        <repository>
            <id>aliyun</id>
            <url>https://maven.aliyun.com/repository/public</url>
        </repository>
    </repositories>
</project>
```

### 3.3 Configure .gitignore

Create a `.gitignore` file:

```gitignore
# Maven
target/
!.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

# IntelliJ IDEA
.idea/
*.iws
*.iml
*.ipr
out/

# Mac
.DS_Store

# Log files
*.log
logs/

# Application
application-local.yml
application-dev.yml
application-prod.yml

# Upload files
uploads/
data/
```

---

## IV. Creating the Common Module

The common module contains code shared by all submodules, such as utility classes, configuration classes, exception handling, etc.

### 4.1 Create the Module Directory

```bash
mkdir -p kb-common/src/main/java/com/knowledge/base/common
mkdir -p kb-common/src/main/resources
```

### 4.2 Create kb-common/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.knowledge.base</groupId>
        <artifactId>knowledge-base-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>kb-common</artifactId>
    <packaging>jar</packaging>
    <name>Knowledge Base Common Module</name>
    <description>Common module</description>

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

        <!-- Spring Boot Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Spring Boot AOP -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- MyBatis Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>

        <!-- Knife4j API documentation -->
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Hutool -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
        </dependency>

        <!-- Apache Commons -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-collections4</artifactId>
        </dependency>

        <!-- FastJSON2 -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 4.3 Create the Unified Response Structure

Create `kb-common/src/main/java/com/knowledge/base/common/result/Result.java`:

```java
package com.knowledge.base.common.result;

import lombok.Data;
import java.io.Serializable;

/**
 * Unified response result wrapper class
 *
 * @param <T> data type
 */
@Data
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

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Success response (no data)
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * Success response (with data)
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * Success response (custom message)
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * Failure response (error code)
     */
    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * Failure response (custom message)
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message, null);
    }

    /**
     * Failure response (custom code and message)
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
```

Create `kb-common/src/main/java/com/knowledge/base/common/result/ResultCode.java`:

```java
package com.knowledge.base.common.result;

import lombok.Getter;

/**
 * Response code enum
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "Operation succeeded"),
    ERROR(500, "Operation failed"),
    PARAM_ERROR(400, "Parameter error"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Access forbidden"),
    NOT_FOUND(404, "Resource not found"),
    METHOD_NOT_ALLOWED(405, "Request method not supported");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

Create `kb-common/src/main/java/com/knowledge/base/common/result/ErrorCode.java`:

```java
package com.knowledge.base.common.result;

import lombok.Getter;

/**
 * Business error code enum
 */
@Getter
public enum ErrorCode {

    // General error codes 1000-1999
    SYSTEM_ERROR(1000, "System error"),
    PARAM_ERROR(1001, "Parameter error"),
    DATA_NOT_FOUND(1002, "Data not found"),
    DATA_ALREADY_EXISTS(1003, "Data already exists"),
    OPERATION_FAILED(1004, "Operation failed"),

    // User error codes 2000-2999
    USER_NOT_EXIST(2000, "User does not exist"),
    USER_ALREADY_EXISTS(2001, "User already exists"),
    USER_PASSWORD_ERROR(2002, "Incorrect password"),
    USER_ACCOUNT_DISABLED(2003, "Account has been disabled"),
    USER_TOKEN_EXPIRED(2004, "Token has expired"),
    USER_TOKEN_INVALID(2005, "Invalid token"),

    // Document error codes 3000-3999
    DOC_NOT_FOUND(3000, "Document not found"),
    DOC_ALREADY_EXISTS(3001, "Document already exists"),
    DOC_TITLE_EMPTY(3002, "Document title must not be empty"),
    DOC_CONTENT_EMPTY(3003, "Document content must not be empty"),

    // Permission error codes 4000-4999
    PERMISSION_DENIED(4000, "Insufficient permissions"),
    ROLE_NOT_EXIST(4001, "Role does not exist"),
    ROLE_ALREADY_EXISTS(4002, "Role already exists");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

### 4.4 Create the Base Entity Class

Create `kb-common/src/main/java/com/knowledge/base/common/config/BaseEntity.java`:

```java
package com.knowledge.base.common.config;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base entity class containing common fields
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key ID
     */
    private Long id;

    /**
     * Creation time
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * Update time
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * Creator ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * Updater ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * Logical delete flag (0-not deleted, 1-deleted)
     */
    @TableLogic
    private Integer deleted;
}
```

### 4.5 Create the Custom Exception

Create `kb-common/src/main/java/com/knowledge/base/common/exception/BusinessException.java`:

```java
package com.knowledge.base.common.exception;

import com.knowledge.base.common.result.ErrorCode;
import lombok.Getter;

/**
 * Business exception
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }
}
```

Create `kb-common/src/main/java/com/knowledge/base/common/exception/GlobalExceptionHandler.java`:

```java
package com.knowledge.base.common.exception;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle business exceptions
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("Business exception: {}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * Handle parameter validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Parameter validation failed";
        log.error("Parameter validation exception: {}", message);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * Handle system exceptions
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("System exception: {}", e.getMessage(), e);
        return Result.error(ResultCode.ERROR.getCode(), "System error, please contact the administrator");
    }
}
```

### 4.6 Create MyBatis Plus Field Auto-Fill

Create `kb-common/src/main/java/com/knowledge/base/common/handler/MyMetaObjectHandler.java`:

```java
package com.knowledge.base.common.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.knowledge.base.common.utils.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus field auto-fill handler
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("Starting insert fill...");
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // Get the current user ID
        Long userId = UserContextUtil.getCurrentUserId();
        if (userId != null) {
            this.strictInsertFill(metaObject, "createBy", Long.class, userId);
            this.strictInsertFill(metaObject, "updateBy", Long.class, userId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("Starting update fill...");
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // Get the current user ID
        Long userId = UserContextUtil.getCurrentUserId();
        if (userId != null) {
            this.strictUpdateFill(metaObject, "updateBy", Long.class, userId);
        }
    }
}
```

---

## V. Creating the User & Auth Service Module

The user & auth service is responsible for user authentication, authorization, role and permission management, and related features.

### 5.1 Create the Module Directory

```bash
mkdir -p kb-user-auth/src/main/java/com/knowledge/base/userauth
mkdir -p kb-user-auth/src/main/resources
```

### 5.2 Create kb-user-auth/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.knowledge.base</groupId>
        <artifactId>knowledge-base-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>kb-user-auth</artifactId>
    <packaging>jar</packaging>
    <name>Knowledge Base User Auth Service</name>
    <description>User & Auth Service</description>

    <dependencies>
        <!-- Common module -->
        <dependency>
            <groupId>com.knowledge.base</groupId>
            <artifactId>kb-common</artifactId>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- MyBatis Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>

        <!-- MySQL Driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Druid -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-3-starter</artifactId>
        </dependency>

        <!-- Knife4j -->
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Spring Boot Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 5.3 Create the Application Bootstrap Class

Create `kb-user-auth/src/main/java/com/knowledge/base/userauth/UserAuthApplication.java`:

```java
package com.knowledge.base.userauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * User & Auth Service bootstrap class
 */
@SpringBootApplication(scanBasePackages = "com.knowledge.base")
@EnableDiscoveryClient
public class UserAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserAuthApplication.class, args);
        System.out.println("User & Auth Service started successfully!");
    }
}
```

### 5.4 Create the Configuration File

Create `kb-user-auth/src/main/resources/application.yml`:

```yaml
server:
  port: 8081
  servlet:
    context-path: /api/auth

spring:
  application:
    name: kb-user-auth
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/kb_user?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    druid:
      initial-size: 10
      max-active: 100
      min-idle: 10
      max-wait: 60000
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1 FROM DUAL
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false

# MyBatis Plus configuration
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.knowledge.base.*.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: ASSIGN_ID
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# Knife4j configuration
knife4j:
  enable: true
  setting:
    language: zh_cn
  production: false
```

### 5.5 Create the Entity Classes

Create `kb-user-auth/src/main/java/com/knowledge/base/userauth/entity/User.java`:

```java
package com.knowledge.base.userauth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User entity class
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    /**
     * Username
     */
    private String username;

    /**
     * Password
     */
    private String password;

    /**
     * Nickname
     */
    private String nickname;

    /**
     * Email
     */
    private String email;

    /**
     * Phone number
     */
    private String phone;

    /**
     * Avatar
     */
    private String avatar;

    /**
     * User status (0-normal, 1-disabled)
     */
    private Integer status;

    /**
     * User type (0-regular user, 1-admin)
     */
    private Integer userType;

    /**
     * Remarks
     */
    private String remark;
}
```

Create `kb-user-auth/src/main/java/com/knowledge/base/userauth/entity/Role.java`:

```java
package com.knowledge.base.userauth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Role entity class
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class Role extends BaseEntity {

    /**
     * Role name
     */
    private String roleName;

    /**
     * Role code
     */
    private String roleCode;

    /**
     * Sort order
     */
    private Integer sort;

    /**
     * Role status (0-normal, 1-disabled)
     */
    private Integer status;

    /**
     * Remarks
     */
    private String remark;
}
```

Create `kb-user-auth/src/main/java/com/knowledge/base/userauth/entity/Permission.java`:

```java
package com.knowledge.base.userauth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Permission entity class
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class Permission extends BaseEntity {

    /**
     * Parent permission ID
     */
    private Long parentId;

    /**
     * Permission name
     */
    private String permissionName;

    /**
     * Permission code
     */
    private String permissionCode;

    /**
     * Permission type (0-directory, 1-menu, 2-button)
     */
    private Integer permissionType;

    /**
     * Route path
     */
    private String path;

    /**
     * Component path
     */
    private String component;

    /**
     * Icon
     */
    private String icon;

    /**
     * Sort order
     */
    private Integer sort;

    /**
     * Permission status (0-normal, 1-disabled)
     */
    private Integer status;

    /**
     * Remarks
     */
    private String remark;
}
```

### 5.6 Create the Mapper Interfaces

Create `kb-user-auth/src/main/java/com/knowledge/base/userauth/mapper/UserMapper.java`:

```java
package com.knowledge.base.userauth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.userauth.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * User Mapper interface
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

Create the corresponding XML mapping file `kb-user-auth/src/main/resources/mapper/UserMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.knowledge.base.userauth.mapper.UserMapper">

</mapper>
```

### 5.7 Create the Service Layer

Create `kb-user-auth/src/main/java/com/knowledge/base/userauth/service/UserService.java`:

```java
package com.knowledge.base.userauth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.userauth.entity.User;

/**
 * User service interface
 */
public interface UserService extends IService<User> {

    /**
     * Query a user by username
     *
     * @param username the username
     * @return the user information
     */
    User getByUsername(String username);
}
```

Create the implementation class `kb-user-auth/src/main/java/com/knowledge/base/userauth/service/impl/UserServiceImpl.java`:

```java
package com.knowledge.base.userauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.userauth.entity.User;
import com.knowledge.base.userauth.mapper.UserMapper;
import com.knowledge.base.userauth.service.UserService;
import org.springframework.stereotype.Service;

/**
 * User service implementation class
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return this.getOne(wrapper);
    }
}
```

### 5.8 Create the Controller Layer

Create `kb-user-auth/src/main/java/com/knowledge/base/userauth/controller/UserController.java`:

```java
package com.knowledge.base.userauth.controller;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.userauth.entity.User;
import com.knowledge.base.userauth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * User controller
 */
@Tag(name = "User Management")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Query a user by ID
     */
    @Operation(summary = "Query user details")
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        return Result.success(user);
    }

    /**
     * Create a user
     */
    @Operation(summary = "Create user")
    @PostMapping
    public Result<Void> save(@RequestBody User user) {
        userService.save(user);
        return Result.success();
    }

    /**
     * Update a user
     */
    @Operation(summary = "Update user")
    @PutMapping
    public Result<Void> update(@RequestBody User user) {
        userService.updateById(user);
        return Result.success();
    }

    /**
     * Delete a user
     */
    @Operation(summary = "Delete user")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }
}
```

---

## VI. Creating the Other Service Modules

Following the same approach used above for the user & auth service module, you can go on to create the other service modules. Below is the basic structure for each service:

### 6.1 Document Service (kb-document)

```bash
mkdir -p kb-document/src/main/java/com/knowledge/base/document
mkdir -p kb-document/src/main/resources
```

Core features:
- Document management (CRUD)
- Document categorization
- Document tags
- Document comments
- Document version management

### 6.2 Search Service (kb-search)

```bash
mkdir -p kb-search/src/main/java/com/knowledge/base/search
mkdir -p kb-search/src/main/resources
```

Core features:
- Document full-text search
- Search suggestions
- Search history

### 6.3 File Service (kb-file)

```bash
mkdir -p kb-file/src/main/java/com/knowledge/base/file
mkdir -p kb-file/src/main/resources
```

Core features:
- File upload/download
- File storage management
- Support for multiple storage backends (local, OSS, S3, etc.)

### 6.4 AI Service (kb-ai)

```bash
mkdir -p kb-ai/src/main/java/com/knowledge/base/ai
mkdir -p kb-ai/src/main/resources
```

Core features:
- AI conversation
- Intelligent document processing
- AI feedback management

### 6.5 Knowledge Graph Service (kb-graph)

```bash
mkdir -p kb-graph/src/main/java/com/knowledge/base/graph
mkdir -p kb-graph/src/main/resources
```

Core features:
- Knowledge graph construction
- Graph relationship queries
- Community detection algorithms

### 6.6 Statistics Service (kb-statistics)

```bash
mkdir -p kb-statistics/src/main/java/com/knowledge/base/statistics
mkdir -p kb-statistics/src/main/resources
```

Core features:
- User behavior statistics
- Document access statistics
- Data trend analysis

### 6.7 Foundation Service (kb-foundation)

```bash
mkdir -p kb-foundation/src/main/java/com/knowledge/base/foundation
mkdir -p kb-foundation/src/main/resources
```

Core features:
- Data dictionary management
- System configuration management
- Operation log recording
- Notification management

### 6.8 API Gateway (kb-gateway)

```bash
mkdir -p kb-gateway/src/main/java/com/knowledge/base/gateway
mkdir -p kb-gateway/src/main/resources
```

Core features:
- Route forwarding
- Unified authentication
- Rate limiting and circuit breaking
- Logging

---

## VII. Database Design

### 7.1 Create the Database Initialization Script

Create a `sql` folder in the project root directory and add the initialization script.

Create the user & auth database script `sql/init_kb_user.sql`:

```sql
-- Create the database
CREATE DATABASE IF NOT EXISTS kb_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE kb_user;

-- User table
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL COMMENT 'User ID',
    username VARCHAR(50) NOT NULL COMMENT 'Username',
    password VARCHAR(100) NOT NULL COMMENT 'Password',
    nickname VARCHAR(50) COMMENT 'Nickname',
    email VARCHAR(100) COMMENT 'Email',
    phone VARCHAR(20) COMMENT 'Phone number',
    avatar VARCHAR(255) COMMENT 'Avatar',
    status TINYINT DEFAULT 0 COMMENT 'Status (0-normal, 1-disabled)',
    user_type TINYINT DEFAULT 0 COMMENT 'User type (0-regular user, 1-admin)',
    remark VARCHAR(500) COMMENT 'Remarks',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    create_by BIGINT COMMENT 'Creator ID',
    update_by BIGINT COMMENT 'Updater ID',
    deleted TINYINT DEFAULT 0 COMMENT 'Delete flag (0-not deleted, 1-deleted)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User table';

-- Role table
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL COMMENT 'Role ID',
    role_name VARCHAR(50) NOT NULL COMMENT 'Role name',
    role_code VARCHAR(50) NOT NULL COMMENT 'Role code',
    sort INT DEFAULT 0 COMMENT 'Sort order',
    status TINYINT DEFAULT 0 COMMENT 'Status (0-normal, 1-disabled)',
    remark VARCHAR(500) COMMENT 'Remarks',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    create_by BIGINT COMMENT 'Creator ID',
    update_by BIGINT COMMENT 'Updater ID',
    deleted TINYINT DEFAULT 0 COMMENT 'Delete flag (0-not deleted, 1-deleted)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role table';

-- Permission table
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT NOT NULL COMMENT 'Permission ID',
    parent_id BIGINT DEFAULT 0 COMMENT 'Parent permission ID',
    permission_name VARCHAR(50) NOT NULL COMMENT 'Permission name',
    permission_code VARCHAR(100) NOT NULL COMMENT 'Permission code',
    permission_type TINYINT DEFAULT 1 COMMENT 'Permission type (0-directory, 1-menu, 2-button)',
    path VARCHAR(200) COMMENT 'Route path',
    component VARCHAR(200) COMMENT 'Component path',
    icon VARCHAR(100) COMMENT 'Icon',
    sort INT DEFAULT 0 COMMENT 'Sort order',
    status TINYINT DEFAULT 0 COMMENT 'Status (0-normal, 1-disabled)',
    remark VARCHAR(500) COMMENT 'Remarks',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    create_by BIGINT COMMENT 'Creator ID',
    update_by BIGINT COMMENT 'Updater ID',
    deleted TINYINT DEFAULT 0 COMMENT 'Delete flag (0-not deleted, 1-deleted)',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Permission table';

-- User-role association table
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT NOT NULL COMMENT 'Primary key ID',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    role_id BIGINT NOT NULL COMMENT 'Role ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User-role association table';

-- Role-permission association table
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT NOT NULL COMMENT 'Primary key ID',
    role_id BIGINT NOT NULL COMMENT 'Role ID',
    permission_id BIGINT NOT NULL COMMENT 'Permission ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (id),
    KEY idx_role_id (role_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role-permission association table';
```

### 7.2 Run the Initialization Script

```bash
mysql -u root -p < sql/init_kb_user.sql
```

---

## VIII. Testing and Verification

### 8.1 Compile the Project

```bash
# Run from the project root directory
mvn clean compile
```

### 8.2 Start the Service

```bash
# Start the user & auth service
cd kb-user-auth
mvn spring-boot:run
```

### 8.3 Access the API Docs

Once started successfully, access the Knife4j docs:

```
http://localhost:8081/api/auth/doc.html
```

### 8.4 Test the Endpoints

Use Postman or Apifox to test the user endpoints:

**Create a user:**
```
POST http://localhost:8081/api/auth/user
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456",
  "nickname": "Test User",
  "email": "test@example.com"
}
```

**Query a user:**
```
GET http://localhost:8081/api/auth/user/1
```

---

## IX. Project Conventions

### 9.1 Naming Conventions

| Type | Convention | Example |
|------|------|------|
| Class name | UpperCamelCase | UserService |
| Method name | lowerCamelCase | getUserById |
| Variable name | lowerCamelCase | userName |
| Constant name | ALL_CAPS with underscores | MAX_SIZE |
| Package name | all lowercase | com.knowledge.base.service |

### 9.2 Code Structure Conventions

Each service module follows this structure:

```
kb-xxx/
├── src/main/java/com/knowledge/base/xxx/
│   ├── controller/      # Controller layer
│   ├── service/         # Service interface layer
│   │   └── impl/        # Service implementation layer
│   ├── mapper/          # Data access layer
│   ├── entity/          # Entity classes
│   ├── dto/             # Data transfer objects
│   ├── vo/              # View objects
│   ├── config/          # Configuration classes
│   └── utils/           # Utility classes
└── src/main/resources/
    ├── application.yml  # Configuration file
    └── mapper/          # MyBatis mapping files
```

### 9.3 Comment Conventions

All classes must have JavaDoc comments:

```java
/**
 * User service implementation class
 *
 * @author Zhang San
 * @since 2026-05-11
 */
public class UserServiceImpl implements UserService {
    /**
     * Query a user by username
     *
     * @param username the username
     * @return the user information
     */
    public User getByUsername(String username) {
        // ...
    }
}
```

---

## X. Frequently Asked Questions

### 10.1 Maven Dependency Download Failures

**Solution:**
1. Check your network connection
2. Configure the Aliyun Maven mirror (already configured in pom.xml)
3. Clear the local repository cache: `rm -rf ~/.m2/repository`

### 10.2 Database Connection Failures

**Solution:**
1. Check whether the MySQL service is running
2. Check whether the username and password are correct
3. Check whether the database has been created
4. Check whether the port is correct (default 3306)

### 10.3 Port Already in Use

**Solution:**
1. Find the process using the port: `lsof -i :8081`
2. Kill the process or change the port in the configuration file

### 10.4 MyBatis Plus Cannot Find the Mapper

**Solution:**
1. Check whether the @Mapper annotation has been added
2. Check whether the mapper-locations configuration is correct
3. Check whether the XML file's namespace is correct

---

## XI. Next Steps

Once the project skeleton is built, you can:

1. **Flesh out the business logic**: implement the concrete business logic in the Service layer
2. **Add unit tests**: add test cases for key functionality
3. **Integrate Redis**: add caching support
4. **Integrate RabbitMQ**: add message queue support
5. **Configure Docker**: add containerized deployment support
6. **Configure CI/CD**: add an automated deployment pipeline

---

## Summary

This article walked you through building, from scratch, the backend project skeleton for an enterprise knowledge base management system. Through this article, you should now have a grasp of:

1. How to build a Spring Boot 3.x multi-module project
2. Integrating and configuring MyBatis Plus
3. Designing a unified response structure and exception handling
4. How to divide a microservice architecture into modules
5. RESTful API design conventions

I hope this tutorial has been helpful, and wish you all the best in your project development!
