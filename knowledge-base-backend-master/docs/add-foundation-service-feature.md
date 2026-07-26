# Adding the Foundation Service Feature

## I. Overview

The foundation service (kb-foundation) is the core supporting module of the knowledge base system, providing system-level shared services such as data dictionary management, system configuration management, notification management, and operation log recording. This article describes in detail how to build the foundation service module from scratch.

### 1.1 Service Positioning

The foundation service, as the system's infrastructure layer, provides the following to other business modules:

| Feature Module | Description | Use Case |
|---------|------|---------|
| Dictionary management | Manages the dictionary data in the system | Dropdown options, data enums, etc. |
| System configuration | Manages system runtime parameters | AI configuration, storage configuration, notification configuration, etc. |
| Notification management | Unified message notification service | System messages, comment notifications, @-mentions, etc. |
| Operation logs | Records user operation behavior | Audit trails, security monitoring |

### 1.2 Technical Architecture

```
kb-foundation
├── Data persistence layer: MyBatis Plus + MySQL
├── Cache layer: Redis (dictionary cache, config cache)
├── Message queue: RabbitMQ (asynchronous notifications)
├── Real-time communication: WebSocket + STOMP (notification push)
├── API docs: Knife4j
└── Infrastructure: Spring Boot 3.2
```

---

## II. Environment Setup

### 2.1 Middleware Installation

The foundation service requires the following middleware:

```bash
# Install Redis (Mac)
brew install redis
brew services start redis

# Install RabbitMQ (Mac)
brew install rabbitmq
brew services start rabbitmq

# Verify service status
redis-cli ping    # Should return PONG
rabbitmq-server  # Start the RabbitMQ service
```

### 2.2 Database Preparation

Create the kb_foundation database:

```bash
mysql -u root -p -e "CREATE DATABASE kb_foundation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

---

## III. Creating the Module Skeleton

### 3.1 Create the Maven Module

Create the kb-foundation submodule in the project root directory:

```bash
mkdir -p kb-foundation/src/main/java/com/knowledge/base/foundation
mkdir -p kb-foundation/src/main/resources
```

### 3.2 Configure pom.xml

Create `kb-foundation/pom.xml`:

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

    <artifactId>kb-foundation</artifactId>
    <packaging>jar</packaging>
    <name>Knowledge Base Foundation Service</name>
    <description>Foundation service (system configuration, notifications, operation logs, dictionary management)</description>

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

        <!-- Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- WebSocket -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>

        <!-- MySQL driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Druid data source -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-3-starter</artifactId>
        </dependency>

        <!-- MyBatis Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Spring Cache -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>

        <!-- RabbitMQ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>

        <!-- Hutool -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Knife4j -->
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        </dependency>

        <!-- Test -->
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

---

## IV. Database Design

### 4.1 Dictionary Type Table (kb_dict)

```sql
CREATE TABLE `kb_dict` (
    `id` BIGINT NOT NULL COMMENT 'Dictionary ID',
    `dict_code` VARCHAR(100) NOT NULL COMMENT 'Dictionary code',
    `dict_name` VARCHAR(100) NOT NULL COMMENT 'Dictionary name',
    `dict_type` VARCHAR(50) NOT NULL COMMENT 'Dictionary type',
    `description` VARCHAR(500) COMMENT 'Description',
    `sort` INT DEFAULT 0 COMMENT 'Sort order',
    `status` TINYINT DEFAULT 1 COMMENT 'Status: 0-disabled, 1-normal',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `create_by` BIGINT COMMENT 'Creator ID',
    `update_by` BIGINT COMMENT 'Updater ID',
    `deleted` TINYINT DEFAULT 0 COMMENT 'Delete flag: 0-not deleted, 1-deleted',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_code` (`dict_code`),
    KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dictionary type table';
```

### 4.2 Dictionary Data Table (kb_dict_data)

```sql
CREATE TABLE `kb_dict_data` (
    `id` BIGINT NOT NULL COMMENT 'Data ID',
    `dict_id` BIGINT NOT NULL COMMENT 'Dictionary ID',
    `dict_code` VARCHAR(100) NOT NULL COMMENT 'Dictionary code',
    `dict_label` VARCHAR(100) NOT NULL COMMENT 'Dictionary label',
    `dict_value` VARCHAR(100) NOT NULL COMMENT 'Dictionary value',
    `dict_sort` INT DEFAULT 0 COMMENT 'Sort order',
    `css_class` VARCHAR(100) COMMENT 'CSS class name',
    `list_class` VARCHAR(100) COMMENT 'List style',
    `is_default` TINYINT DEFAULT 0 COMMENT 'Is default: 0-no, 1-yes',
    `status` TINYINT DEFAULT 1 COMMENT 'Status: 0-disabled, 1-normal',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (`id`),
    KEY `idx_dict_id` (`dict_id`),
    KEY `idx_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dictionary data table';
```

### 4.3 System Configuration Table (kb_system_config)

```sql
CREATE TABLE `kb_system_config` (
    `id` BIGINT NOT NULL COMMENT 'Config ID',
    `config_key` VARCHAR(100) NOT NULL COMMENT 'Config key',
    `config_value` TEXT COMMENT 'Config value',
    `config_type` VARCHAR(20) NOT NULL COMMENT 'Config type: string/number/boolean/json',
    `category` VARCHAR(50) NOT NULL COMMENT 'Config category: AI/STORAGE/NOTIFICATION/SECURITY, etc.',
    `description` VARCHAR(500) COMMENT 'Config description',
    `is_public` TINYINT DEFAULT 0 COMMENT 'Is public: 0-private, 1-public',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `create_by` BIGINT COMMENT 'Creator ID',
    `update_by` BIGINT COMMENT 'Updater ID',
    `deleted` TINYINT DEFAULT 0 COMMENT 'Delete flag: 0-not deleted, 1-deleted',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System configuration table';
```

### 4.4 System Notification Table (kb_notification)

```sql
CREATE TABLE `kb_notification` (
    `id` BIGINT NOT NULL COMMENT 'Notification ID',
    `user_id` BIGINT NOT NULL COMMENT 'Recipient user ID',
    `user_name` VARCHAR(50) COMMENT 'User name (denormalized field)',
    `notification_type` VARCHAR(20) NOT NULL COMMENT 'Notification type: system/comment/mention/review/like',
    `title` VARCHAR(200) NOT NULL COMMENT 'Notification title',
    `content` TEXT COMMENT 'Notification content',
    `link` VARCHAR(500) COMMENT 'Redirect link',
    `related_type` VARCHAR(50) COMMENT 'Related type',
    `related_id` BIGINT COMMENT 'Related ID',
    `is_read` TINYINT DEFAULT 0 COMMENT 'Whether read: 0-unread, 1-read',
    `read_time` DATETIME COMMENT 'Read time',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_read` (`is_read`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System notification table';
```

### 4.5 Operation Log Table (kb_operation_log)

```sql
CREATE TABLE `kb_operation_log` (
    `id` BIGINT NOT NULL COMMENT 'Log ID',
    `module` VARCHAR(50) COMMENT 'Module name',
    `operation_type` VARCHAR(20) COMMENT 'Operation type: LOGIN/CREATE/UPDATE/DELETE, etc.',
    `operation_desc` VARCHAR(500) COMMENT 'Operation description',
    `request_method` VARCHAR(10) COMMENT 'Request method: GET/POST/PUT/DELETE',
    `request_url` VARCHAR(500) COMMENT 'Request URL',
    `request_params` TEXT COMMENT 'Request parameters (JSON)',
    `response_result` TEXT COMMENT 'Response result (JSON)',
    `user_id` BIGINT COMMENT 'Operator user ID',
    `username` VARCHAR(50) COMMENT 'Operator username',
    `ip_address` VARCHAR(50) COMMENT 'IP address',
    `location` VARCHAR(200) COMMENT 'Location',
    `user_agent` VARCHAR(500) COMMENT 'User agent',
    `execute_time` INT COMMENT 'Execution time (ms)',
    `status` TINYINT COMMENT 'Status: 0-failed, 1-succeeded',
    `error_msg` TEXT COMMENT 'Error message',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Operation time',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_operation_type` (`operation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Operation log table';
```

---

## V. Application Bootstrap Configuration

### 5.1 Create the Bootstrap Class

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/FoundationApplication.java`:

```java
package com.knowledge.base.foundation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * kb-foundation foundation service bootstrap class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@EnableAsync
@EnableCaching
@EnableTransactionManagement
@SpringBootApplication
@ComponentScan(basePackages = {"com.knowledge.base.foundation", "com.knowledge.base.common"})
public class FoundationApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoundationApplication.class, args);
        System.out.println("""

                ========================================
                   Foundation service started successfully!
                   Service name: kb-foundation
                   Service port: 8089
                   API docs: http://localhost:8089/api/foundation/doc.html
                   Druid monitor: http://localhost:8089/api/foundation/druid/
                ========================================
                """);
    }
}
```

### 5.2 Configure application.yml

Create `kb-foundation/src/main/resources/application.yml`:

```yaml
server:
  port: 8089
  servlet:
    context-path: /api/foundation

spring:
  application:
    name: kb-foundation

  # Data source configuration
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/kb_foundation?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
      filters: stat,wall
      connection-properties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000
      web-stat-filter:
        enabled: true
        url-pattern: /*
        exclusions: "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*"
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        reset-enable: false
        login-username: admin
        login-password: admin

  # Redis configuration
  redis:
    host: localhost
    port: 6379
    password:
    database: 9
    timeout: 10000ms
    lettuce:
      pool:
        max-active: 8
        max-wait: -1ms
        max-idle: 8
        min-idle: 0

  # RabbitMQ configuration
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 1

  # Jackson configuration
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null

  # Cache configuration
  cache:
    type: redis
    redis:
      time-to-live: 600000

# MyBatis Plus configuration
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.knowledge.base.foundation.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: input
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# Logging configuration
logging:
  level:
    root: INFO
    com.knowledge.base: DEBUG
    com.baomidou.mybatisplus: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"

# Swagger configuration
knife4j:
  enable: true
  setting:
    language: zh_cn
  production: false

# WebSocket configuration
websocket:
  enabled: true
  path: /ws/notification
  allowed-origins: "*"

# Notification configuration
notification:
  retention-days: 90
  batch-size: 100
```

---

## VI. Infrastructure Configuration

### 6.1 Redis Cache Configuration

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/config/RedisCacheConfig.java`:

```java
package com.knowledge.base.foundation.config;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis cache configuration
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
@EnableCaching
public class RedisCacheConfig extends CachingConfigurerSupport {

    /**
     * Cache manager
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Configure serialization
        RedisSerializationContext.SerializationPair<Object> jsonSerializer =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer());

        // Configure the cache policy
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(jsonSerializer)
                .entryTtl(Duration.ofMinutes(10)) // Default 10-minute expiration
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .transactionAware()
                .build();
    }
}
```

### 6.2 RabbitMQ Messaging Configuration

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/config/RabbitMQConfig.java`:

```java
package com.knowledge.base.foundation.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ message queue configuration
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class RabbitMQConfig {

    // Notification exchange
    public static final String NOTIFICATION_EXCHANGE = "kb.notification.exchange";

    // Notification queue
    public static final String NOTIFICATION_QUEUE = "kb.notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.#";

    // System configuration update exchange
    public static final String CONFIG_EXCHANGE = "kb.config.exchange";

    // System configuration update queue
    public static final String CONFIG_QUEUE = "kb.config.queue";
    public static final String CONFIG_ROUTING_KEY = "config.update";

    /**
     * Notification exchange (topic mode)
     */
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    /**
     * Notification queue
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    /**
     * Notification queue binding
     */
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }

    /**
     * System configuration update exchange (direct mode)
     */
    @Bean
    public DirectExchange configExchange() {
        return new DirectExchange(CONFIG_EXCHANGE, true, false);
    }

    /**
     * System configuration update queue
     */
    @Bean
    public Queue configQueue() {
        return QueueBuilder.durable(CONFIG_QUEUE).build();
    }

    /**
     * System configuration queue binding
     */
    @Bean
    public Binding configBinding() {
        return BindingBuilder.bind(configQueue())
                .to(configExchange())
                .with(CONFIG_ROUTING_KEY);
    }

    /**
     * RabbitMQ template (configures the message converter)
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
}
```

### 6.3 WebSocket Real-Time Communication Configuration

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/config/WebSocketConfig.java`:

```java
package com.knowledge.base.foundation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure the message broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable the simple message broker, used to push messages to clients
        registry.enableSimpleBroker("/topic", "/queue");
        // Set the prefix for messages sent by the client
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Configure the STOMP endpoints
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the WebSocket endpoint, allowing cross-origin requests
        registry.addEndpoint("/ws/notification")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Enable SockJS support
    }
}
```

### 6.4 Knife4j API Documentation Configuration

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/config/Knife4jConfig.java`:

```java
package com.knowledge.base.foundation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j configuration class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Configuration
public class Knife4jConfig {

    /**
     * Configure OpenAPI
     *
     * @return the OpenAPI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Knowledge Base System - Foundation Service API Docs")
                .version("1.0.0")
                .description("Provides data dictionary, system configuration, notification management, operation logs, and related features")
                .contact(new Contact()
                    .name("airwzz999")
                    .email("support@knowledge-base.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
```

---

## VII. Common Module Base Classes

Before starting entity class design, let's introduce the base classes in the common module, which will be used by every module.

### 7.1 Base Entity Class BaseEntity

Create `kb-common/src/main/java/com/knowledge/base/common/config/BaseEntity.java`:

```java
package com.knowledge.base.common.config;

import com.baomidou.mybatisplus.annotation.*;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base entity class
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; all entity
 * classes should extend this class</p>
 * <p>Contains common fields: ID, creation time, update time, logical delete flag</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key ID (generated via the Snowflake algorithm)
     */
    @TableId(type = IdType.INPUT)
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

    /**
     * Optimistic lock version number
     */
    @Version
    private Integer version;

    /**
     * Automatically fill in the ID before inserting
     */
    public void preInsert() {
        if (this.id == null) {
            this.id = SnowflakeIdGenerator.getInstance().nextId();
        }
    }
}
```

**Design points:**
- Uses `@TableId(type = IdType.INPUT)` to configure manually supplied IDs (generated via the Snowflake algorithm)
- Uses `@TableField(fill = FieldFill.INSERT)` to auto-fill the creation time and creator
- Uses `@TableField(fill = FieldFill.INSERT_UPDATE)` to auto-fill the update time and updater
- Uses `@TableLogic` to configure logical deletion
- Uses `@Version` to configure optimistic locking
- Provides a `preInsert()` method that automatically generates the ID before insertion

### 7.2 Pagination Parameter Class PageParam

Create `kb-common/src/main/java/com/knowledge/base/common/result/PageParam.java`:

```java
package com.knowledge.base.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Base class for pagination query parameters
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Pagination query parameters")
public class PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Current page number
     */
    @Schema(description = "Current page number", example = "1")
    private Long current = 1L;

    /**
     * Page size
     */
    @Schema(description = "Page size", example = "10")
    private Long size = 10L;

    /**
     * Sort field
     */
    @Schema(description = "Sort field")
    private String sortField;

    /**
     * Sort direction (asc/desc)
     */
    @Schema(description = "Sort direction", example = "desc")
    private String sortOrder;

    /**
     * Get the offset
     */
    public long getOffset() {
        return (current - 1) * size;
    }
}
```

**Design points:**
- Provides a unified base class for pagination parameters
- Defaults to page 1, with a page size of 10
- Supports a sort field and sort direction
- Provides a `getOffset()` method to compute the offset

### 7.3 Pagination Result Class PageResult

Create `kb-common/src/main/java/com/knowledge/base/common/result/PageResult.java`:

```java
package com.knowledge.base.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Pagination result wrapper class
 *
 * @param <T> data type
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pagination result")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Current page number
     */
    @Schema(description = "Current page number")
    private Long current;

    /**
     * Page size
     */
    @Schema(description = "Page size")
    private Long size;

    /**
     * Total record count
     */
    @Schema(description = "Total record count")
    private Long total;

    /**
     * Total page count
     */
    @Schema(description = "Total page count")
    private Long pages;

    /**
     * The data list
     */
    @Schema(description = "Data list")
    private List<T> records;

    /**
     * Build a pagination result
     */
    public static <T> PageResult<T> of(Long current, Long size, Long total, List<T> records) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setCurrent(current);
        pageResult.setSize(size);
        pageResult.setTotal(total);
        pageResult.setRecords(records);
        pageResult.setPages((total + size - 1) / size);
        return pageResult;
    }

    /**
     * An empty pagination result
     */
    @SuppressWarnings("unchecked")
    public static <T> PageResult<T> empty() {
        return PageResult.<T>builder()
                .current(1L)
                .size(10L)
                .total(0L)
                .records(Collections.emptyList())
                .build();
    }

    /**
     * Check whether there is data
     */
    public boolean hasData() {
        return records != null && !records.isEmpty();
    }
}
```

**Design points:**
- Uses the `@Builder` pattern to support flexible construction
- Contains complete pagination information (current page, page size, total record count, total page count, data list)
- Provides an `of()` static method to quickly build a pagination result
- Provides an `empty()` method that returns an empty pagination result
- Provides a `hasData()` method to check whether there is data

### 7.4 Unified Response Result Class Result

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

**Design points:**
- Uses `@JsonInclude(JsonInclude.Include.NON_NULL)` to omit null fields
- Contains the response code, response message, response data, and timestamp
- Provides several overloaded `success()` and `error()` methods
- Automatically adds a timestamp

### 7.5 Response Code Enum ResultCode

Create `kb-common/src/main/java/com/knowledge/base/common/result/ResultCode.java`:

```java
package com.knowledge.base.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response code enum class
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; centrally
 * manages the system's response codes</p>
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
    SUCCESS(200, "Operation succeeded"),

    /**
     * Failure
     */
    ERROR(500, "Operation failed"),

    /**
     * Parameter error
     */
    PARAM_ERROR(400, "Parameter error"),

    /**
     * Missing parameter
     */
    PARAM_MISSING(400, "Missing parameter"),

    /**
     * Invalid parameter
     */
    PARAM_INVALID(400, "Invalid parameter"),

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
     * Request method not supported
     */
    METHOD_NOT_ALLOWED(405, "Request method not supported"),

    /**
     * Request timeout
     */
    REQUEST_TIMEOUT(408, "Request timeout"),

    /**
     * Internal system error
     */
    INTERNAL_SERVER_ERROR(500, "Internal system error"),

    /**
     * Service unavailable
     */
    SERVICE_UNAVAILABLE(503, "Service unavailable"),

    /**
     * Incorrect username or password
     */
    USERNAME_OR_PASSWORD_ERROR(10001, "Incorrect username or password"),

    /**
     * User does not exist
     */
    USER_NOT_EXIST(10002, "User does not exist"),

    /**
     * User already exists
     */
    USER_ALREADY_EXIST(10003, "User already exists"),

    /**
     * User has been disabled
     */
    USER_DISABLED(10004, "User has been disabled"),

    /**
     * Invalid token
     */
    TOKEN_INVALID(10005, "Invalid token"),

    /**
     * Token has expired
     */
    TOKEN_EXPIRED(10006, "Token has expired"),

    /**
     * Access denied
     */
    ACCESS_DENIED(10007, "Access denied"),

    /**
     * Document does not exist
     */
    DOCUMENT_NOT_EXIST(20001, "Document does not exist"),

    /**
     * Document already exists
     */
    DOCUMENT_ALREADY_EXIST(20002, "Document already exists"),

    /**
     * Document category does not exist
     */
    CATEGORY_NOT_EXIST(20003, "Document category does not exist"),

    /**
     * File upload failed
     */
    FILE_UPLOAD_FAILED(20004, "File upload failed"),

    /**
     * File type not supported
     */
    FILE_TYPE_NOT_SUPPORTED(20005, "File type not supported"),

    /**
     * File size exceeds the limit
     */
    FILE_SIZE_EXCEEDED(20006, "File size exceeds the limit");

    /**
     * Response code
     */
    private final Integer code;

    /**
     * Response message
     */
    private final String message;
}
```

**Design points:**
- Uses `@Getter` and `@AllArgsConstructor` to simplify the code
- Includes standard HTTP response codes
- Includes custom business response codes (10xxx-user-related, 20xxx-document-related)
- Response codes are categorized for easier extension

### 7.6 Usage Example

**An entity class extending BaseEntity:**
```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_dict")
public class Dict extends BaseEntity {
    private String dictCode;
    private String dictName;
    // Other fields...
}
```

**A query DTO extending PageParam:**
```java
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemConfigQueryDTO extends PageParam {
    private String configKey;
    private String category;
    // Other query conditions...
}
```

**A Controller returning Result:**
```java
@GetMapping("/{id}")
public Result<Dict> getDictById(@PathVariable Long id) {
    Dict dict = dictService.getById(id);
    return Result.success(dict);
}
```

**A Service returning PageResult:**
```java
public PageResult<Dict> pageDicts(PageParam pageParam) {
    IPage<Dict> page = dictMapper.selectPage(
        new Page<>(pageParam.getCurrent(), pageParam.getSize()), null
    );
    return PageResult.of(
        page.getCurrent(),
        page.getSize(),
        page.getTotal(),
        page.getRecords()
    );
}
```

---

## VIII. Entity Class Design

### 7.1 Dictionary Type Entity

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/entity/Dict.java`:

```java
package com.knowledge.base.foundation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Dictionary type entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_dict")
@Schema(description = "Dictionary type entity")
public class Dict extends BaseEntity {

    @Schema(description = "Dictionary code")
    @TableField("dict_code")
    private String dictCode;

    @Schema(description = "Dictionary name")
    @TableField("dict_name")
    private String dictName;

    @Schema(description = "Dictionary type")
    @TableField("dict_type")
    private String dictType;

    @Schema(description = "Description")
    @TableField("description")
    private String description;

    @Schema(description = "Sort order")
    @TableField("sort")
    private Integer sort;

    @Schema(description = "Status: 0-disabled, 1-normal")
    @TableField("status")
    private Integer status;

    @Schema(description = "Creation time")
    @TableField("create_time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    @TableField("update_time")
    private LocalDateTime updateTime;
}
```

### 7.2 Dictionary Data Entity

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/entity/DictData.java`:

```java
package com.knowledge.base.foundation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Dictionary data entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_dict_data")
@Schema(description = "Dictionary data entity")
public class DictData extends BaseEntity {

    @Schema(description = "Dictionary ID")
    @TableField("dict_id")
    private Long dictId;

    @Schema(description = "Dictionary code (redundant)")
    @TableField("dict_code")
    private String dictCode;

    @Schema(description = "Dictionary label")
    @TableField("dict_label")
    private String dictLabel;

    @Schema(description = "Dictionary value")
    @TableField("dict_value")
    private String dictValue;

    @Schema(description = "Sort order")
    @TableField("dict_sort")
    private Integer dictSort;

    @Schema(description = "CSS class name")
    @TableField("css_class")
    private String cssClass;

    @Schema(description = "List style")
    @TableField("list_class")
    private String listClass;

    @Schema(description = "Is default: 0-no, 1-yes")
    @TableField("is_default")
    private Integer isDefault;

    @Schema(description = "Status: 0-disabled, 1-normal")
    @TableField("status")
    private Integer status;

    @Schema(description = "Creation time")
    @TableField("create_time")
    private LocalDateTime createTime;
}
```

### 7.3 System Configuration Entity

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/entity/SystemConfig.java`:

```java
package com.knowledge.base.foundation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * System configuration entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_system_config")
@Schema(description = "System configuration entity")
public class SystemConfig extends BaseEntity {

    @Schema(description = "Config key")
    @TableField("config_key")
    private String configKey;

    @Schema(description = "Config value")
    @TableField("config_value")
    private String configValue;

    @Schema(description = "Config type: string/number/boolean/json")
    @TableField("config_type")
    private String configType;

    @Schema(description = "Config category: AI/STORAGE/NOTIFICATION/SECURITY, etc.")
    @TableField("category")
    private String category;

    @Schema(description = "Config description")
    @TableField("description")
    private String description;

    @Schema(description = "Is public: 0-private, 1-public")
    @TableField("is_public")
    private Integer isPublic;

    @Schema(description = "Creation time")
    @TableField("create_time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    @TableField("update_time")
    private LocalDateTime updateTime;
}
```

### 7.4 System Notification Entity

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/entity/Notification.java`:

```java
package com.knowledge.base.foundation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * System notification entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_notification")
@Schema(description = "System notification entity")
public class Notification extends BaseEntity {

    @Schema(description = "Recipient user ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "User name (redundant field)")
    @TableField("user_name")
    private String userName;

    @Schema(description = "Notification type: system/comment/mention/review/like")
    @TableField("notification_type")
    private String notificationType;

    @Schema(description = "Notification title")
    @TableField("title")
    private String title;

    @Schema(description = "Notification content")
    @TableField("content")
    private String content;

    @Schema(description = "Redirect link")
    @TableField("link")
    private String link;

    @Schema(description = "Related type")
    @TableField("related_type")
    private String relatedType;

    @Schema(description = "Related ID")
    @TableField("related_id")
    private Long relatedId;

    @Schema(description = "Whether read: 0-unread, 1-read")
    @TableField("is_read")
    private Integer isRead;

    @Schema(description = "Read time")
    @TableField("read_time")
    private LocalDateTime readTime;

    @Schema(description = "Creation time")
    @TableField("create_time")
    private LocalDateTime createTime;
}
```

### 7.5 Operation Log Entity

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/entity/OperationLog.java`:

```java
package com.knowledge.base.foundation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowledge.base.common.config.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Operation log entity class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_operation_log")
@Schema(description = "Operation log entity")
public class OperationLog extends BaseEntity {

    @Schema(description = "Module name")
    @TableField("module")
    private String module;

    @Schema(description = "Operation type: LOGIN/CREATE/UPDATE/DELETE, etc.")
    @TableField("operation_type")
    private String operationType;

    @Schema(description = "Operation description")
    @TableField("operation_desc")
    private String operationDesc;

    @Schema(description = "Request method: GET/POST/PUT/DELETE")
    @TableField("request_method")
    private String requestMethod;

    @Schema(description = "Request URL")
    @TableField("request_url")
    private String requestUrl;

    @Schema(description = "Request parameters (JSON)")
    @TableField("request_params")
    private String requestParams;

    @Schema(description = "Response result (JSON)")
    @TableField("response_result")
    private String responseResult;

    @Schema(description = "Operator user ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "Operator username")
    @TableField("username")
    private String username;

    @Schema(description = "IP address")
    @TableField("ip_address")
    private String ipAddress;

    @Schema(description = "Location")
    @TableField("location")
    private String location;

    @Schema(description = "User agent")
    @TableField("user_agent")
    private String userAgent;

    @Schema(description = "Execution time (ms)")
    @TableField("execute_time")
    private Integer executeTime;

    @Schema(description = "Status: 0-failed, 1-succeeded")
    @TableField("status")
    private Integer status;

    @Schema(description = "Error message")
    @TableField("error_msg")
    private String errorMsg;

    @Schema(description = "Operation time")
    @TableField("create_time")
    private LocalDateTime createTime;
}
```

### 7.6 DTO and VO Design

Create the DTO and VO classes used for data transfer and display.

#### 7.6.1 Dictionary DTO

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/dto/DictDTO.java`:

```java
package com.knowledge.base.foundation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Dictionary DTO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used to receive
 * request parameters for creating/updating a dictionary type</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Dictionary type request parameters")
public class DictDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Dictionary ID
     */
    @Schema(description = "Dictionary ID", example = "1234567890123456789")
    private Long id;

    /**
     * Dictionary code
     */
    @Schema(description = "Dictionary code", required = true, example = "sys_user_gender")
    @NotBlank(message = "Dictionary code must not be empty")
    @Size(max = 100, message = "Dictionary code must not exceed 100 characters")
    private String dictCode;

    /**
     * Dictionary name
     */
    @Schema(description = "Dictionary name", required = true, example = "User gender")
    @NotBlank(message = "Dictionary name must not be empty")
    @Size(max = 100, message = "Dictionary name must not exceed 100 characters")
    private String dictName;

    /**
     * Dictionary type
     */
    @Schema(description = "Dictionary type", required = true, example = "system")
    @NotBlank(message = "Dictionary type must not be empty")
    @Size(max = 50, message = "Dictionary type must not exceed 50 characters")
    private String dictType;

    /**
     * Description
     */
    @Schema(description = "Description", example = "User gender dictionary")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Sort order
     */
    @Schema(description = "Sort order", example = "0")
    @NotNull(message = "Sort order must not be empty")
    private Integer sort;

    /**
     * Status: 0-disabled, 1-normal
     */
    @Schema(description = "Status", example = "1")
    @NotNull(message = "Status must not be empty")
    private Integer status;
}
```

#### 7.6.2 Dictionary Data DTO

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/dto/DictDataDTO.java`:

```java
package com.knowledge.base.foundation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Dictionary data DTO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used to receive
 * request parameters for creating/updating dictionary data</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Dictionary data request parameters")
public class DictDataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Dictionary data ID
     */
    @Schema(description = "Dictionary data ID", example = "1234567890123456789")
    private Long id;

    /**
     * Dictionary ID
     */
    @Schema(description = "Dictionary ID", required = true, example = "1234567890123456789")
    @NotNull(message = "Dictionary ID must not be empty")
    private Long dictId;

    /**
     * Dictionary code (redundant)
     */
    @Schema(description = "Dictionary code", example = "sys_user_gender")
    @Size(max = 100, message = "Dictionary code must not exceed 100 characters")
    private String dictCode;

    /**
     * Dictionary label
     */
    @Schema(description = "Dictionary label", required = true, example = "Male")
    @NotBlank(message = "Dictionary label must not be empty")
    @Size(max = 100, message = "Dictionary label must not exceed 100 characters")
    private String dictLabel;

    /**
     * Dictionary value
     */
    @Schema(description = "Dictionary value", required = true, example = "1")
    @NotBlank(message = "Dictionary value must not be empty")
    @Size(max = 100, message = "Dictionary value must not exceed 100 characters")
    private String dictValue;

    /**
     * Sort order
     */
    @Schema(description = "Sort order", example = "0")
    @NotNull(message = "Sort order must not be empty")
    private Integer dictSort;

    /**
     * CSS class name
     */
    @Schema(description = "CSS class name", example = "default")
    @Size(max = 100, message = "CSS class name must not exceed 100 characters")
    private String cssClass;

    /**
     * List style
     */
    @Schema(description = "List style", example = "primary")
    @Size(max = 100, message = "List style must not exceed 100 characters")
    private String listClass;

    /**
     * Is default: 0-no, 1-yes
     */
    @Schema(description = "Is default", example = "0")
    @NotNull(message = "Is default must not be empty")
    private Integer isDefault;

    /**
     * Status: 0-disabled, 1-normal
     */
    @Schema(description = "Status", example = "1")
    @NotNull(message = "Status must not be empty")
    private Integer status;
}
```

#### 7.6.3 System Configuration DTO

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/dto/SystemConfigDTO.java`:

```java
package com.knowledge.base.foundation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * System configuration DTO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used to receive
 * request parameters for creating/updating system configuration</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "System configuration request parameters")
public class SystemConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Config ID
     */
    @Schema(description = "Config ID", example = "1234567890123456789")
    private Long id;

    /**
     * Config key
     */
    @Schema(description = "Config key", required = true, example = "ai.model.name")
    @NotBlank(message = "Config key must not be empty")
    @Size(max = 100, message = "Config key must not exceed 100 characters")
    private String configKey;

    /**
     * Config value
     */
    @Schema(description = "Config value", required = true, example = "gpt-4")
    @NotBlank(message = "Config value must not be empty")
    @Size(max = 1000, message = "Config value must not exceed 1000 characters")
    private String configValue;

    /**
     * Config type: string/number/boolean/json
     */
    @Schema(description = "Config type", required = true, example = "string")
    @NotBlank(message = "Config type must not be empty")
    @Size(max = 20, message = "Config type must not exceed 20 characters")
    private String configType;

    /**
     * Config category: AI/STORAGE/NOTIFICATION/SECURITY, etc.
     */
    @Schema(description = "Config category", required = true, example = "AI")
    @NotBlank(message = "Config category must not be empty")
    @Size(max = 50, message = "Config category must not exceed 50 characters")
    private String category;

    /**
     * Config description
     */
    @Schema(description = "Config description", example = "AI model name configuration")
    @Size(max = 500, message = "Config description must not exceed 500 characters")
    private String description;

    /**
     * Is public: 0-private, 1-public
     */
    @Schema(description = "Is public", example = "0")
    @NotNull(message = "Is public must not be empty")
    private Integer isPublic;
}
```

#### 7.6.4 System Configuration Query DTO

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/dto/SystemConfigQueryDTO.java`:

```java
package com.knowledge.base.foundation.dto;

import com.knowledge.base.common.result.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * System configuration query DTO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used for
 * system configuration query conditions</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "System configuration query parameters")
public class SystemConfigQueryDTO extends PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Config key
     */
    @Schema(description = "Config key", example = "ai.model.name")
    private String configKey;

    /**
     * Config category: AI/STORAGE/NOTIFICATION/SECURITY, etc.
     */
    @Schema(description = "Config category", example = "AI")
    private String category;

    /**
     * Config type: string/number/boolean/json
     */
    @Schema(description = "Config type", example = "string")
    private String configType;

    /**
     * Is public: 0-private, 1-public
     */
    @Schema(description = "Is public", example = "0")
    private Integer isPublic;

    /**
     * Keyword search (config key or description)
     */
    @Schema(description = "Keyword", example = "AI")
    private String keyword;
}
```

#### 7.6.5 Notification DTO

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/dto/NotificationDTO.java`:

```java
package com.knowledge.base.foundation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * Notification DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Notification DTO")
public class NotificationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Notification ID")
    private Long id;

    @NotNull(message = "Recipient user ID must not be empty")
    @Schema(description = "Recipient user ID")
    private Long userId;

    @Schema(description = "User name (redundant field)")
    private String userName;

    @NotBlank(message = "Notification type must not be empty")
    @Schema(description = "Notification type: system/comment/mention/review/like")
    private String notificationType;

    @NotBlank(message = "Notification title must not be empty")
    @Schema(description = "Notification title")
    private String title;

    @NotBlank(message = "Notification content must not be empty")
    @Schema(description = "Notification content")
    private String content;

    @Schema(description = "Redirect link")
    private String link;

    @Schema(description = "Related type")
    private String relatedType;

    @Schema(description = "Related ID")
    private Long relatedId;

    @Schema(description = "Whether read: 0-unread, 1-read")
    private Integer isRead;
}
```

#### 7.6.6 Notification Query DTO

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/dto/NotificationQueryDTO.java`:

```java
package com.knowledge.base.foundation.dto;

import com.knowledge.base.common.result.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Notification query DTO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Notification query DTO")
public class NotificationQueryDTO extends PageParam {

    private static final long serialVersionUID = 1L;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Notification type")
    private String notificationType;

    @Schema(description = "Whether read: 0-unread, 1-read")
    private Integer isRead;

    @Schema(description = "Start time")
    private String startTime;

    @Schema(description = "End time")
    private String endTime;
}
```

#### 7.6.7 Operation Log DTO

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/dto/OperationLogDTO.java`:

```java
package com.knowledge.base.foundation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Operation log DTO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used to receive
 * request parameters for creating an operation log</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Schema(description = "Operation log request parameters")
public class OperationLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Log ID
     */
    @Schema(description = "Log ID", example = "1234567890123456789")
    private Long id;

    /**
     * Module name
     */
    @Schema(description = "Module name", required = true, example = "User Management")
    @NotBlank(message = "Module name must not be empty")
    @Size(max = 50, message = "Module name must not exceed 50 characters")
    private String module;

    /**
     * Operation type: LOGIN/CREATE/UPDATE/DELETE, etc.
     */
    @Schema(description = "Operation type", required = true, example = "CREATE")
    @NotBlank(message = "Operation type must not be empty")
    @Size(max = 20, message = "Operation type must not exceed 20 characters")
    private String operationType;

    /**
     * Operation description
     */
    @Schema(description = "Operation description", required = true, example = "Create user")
    @NotBlank(message = "Operation description must not be empty")
    @Size(max = 500, message = "Operation description must not exceed 500 characters")
    private String operationDesc;

    /**
     * Request method: GET/POST/PUT/DELETE
     */
    @Schema(description = "Request method", example = "POST")
    @Size(max = 10, message = "Request method must not exceed 10 characters")
    private String requestMethod;

    /**
     * Request URL
     */
    @Schema(description = "Request URL", example = "/api/users")
    @Size(max = 500, message = "Request URL must not exceed 500 characters")
    private String requestUrl;

    /**
     * Request parameters (JSON)
     */
    @Schema(description = "Request parameters", example = "{\"username\":\"zhangsan\"}")
    private String requestParams;

    /**
     * Response result (JSON)
     */
    @Schema(description = "Response result", example = "{\"code\":200,\"message\":\"success\"}")
    private String responseResult;

    /**
     * Operator user ID
     */
    @Schema(description = "Operator user ID", example = "1234567890123456789")
    private Long userId;

    /**
     * Operator username
     */
    @Schema(description = "Operator username", example = "admin")
    @Size(max = 50, message = "Operator username must not exceed 50 characters")
    private String username;

    /**
     * IP address
     */
    @Schema(description = "IP address", example = "192.168.1.1")
    @Size(max = 50, message = "IP address must not exceed 50 characters")
    private String ipAddress;

    /**
     * Location
     */
    @Schema(description = "Location", example = "Beijing")
    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    /**
     * User agent
     */
    @Schema(description = "User agent", example = "Mozilla/5.0...")
    @Size(max = 500, message = "User agent must not exceed 500 characters")
    private String userAgent;

    /**
     * Execution time (milliseconds)
     */
    @Schema(description = "Execution time (ms)", example = "100")
    private Integer executeTime;

    /**
     * Status: 0-failed, 1-succeeded
     */
    @Schema(description = "Status", example = "1")
    @NotNull(message = "Status must not be empty")
    private Integer status;

    /**
     * Error message
     */
    @Schema(description = "Error message", example = "Operation failed")
    @Size(max = 2000, message = "Error message must not exceed 2000 characters")
    private String errorMsg;
}
```

#### 7.6.8 Operation Log Query DTO

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/dto/OperationLogQueryDTO.java`:

```java
package com.knowledge.base.foundation.dto;

import com.knowledge.base.common.result.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Operation log query DTO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used for
 * operation log query conditions</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Operation log query parameters")
public class OperationLogQueryDTO extends PageParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Module name
     */
    @Schema(description = "Module name", example = "User Management")
    private String module;

    /**
     * Operation type: LOGIN/CREATE/UPDATE/DELETE, etc.
     */
    @Schema(description = "Operation type", example = "CREATE")
    private String operationType;

    /**
     * Operator user ID
     */
    @Schema(description = "Operator user ID", example = "1234567890123456789")
    private Long userId;

    /**
     * Operator username
     */
    @Schema(description = "Operator username", example = "admin")
    private String username;

    /**
     * Status: 0-failed, 1-succeeded
     */
    @Schema(description = "Status", example = "1")
    private Integer status;

    /**
     * Start time
     */
    @Schema(description = "Start time", example = "2024-01-01 00:00:00")
    private String startTime;

    /**
     * End time
     */
    @Schema(description = "End time", example = "2024-12-31 23:59:59")
    private String endTime;

    /**
     * Keyword search (operation description or request URL)
     */
    @Schema(description = "Keyword", example = "create")
    private String keyword;
}
```

#### 7.6.9 Dictionary VO

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/vo/DictVO.java`:

```java
package com.knowledge.base.foundation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Dictionary VO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used to return
 * dictionary type information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dictionary type response")
public class DictVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Dictionary ID
     */
    @Schema(description = "Dictionary ID")
    private Long id;

    /**
     * Dictionary code
     */
    @Schema(description = "Dictionary code")
    private String dictCode;

    /**
     * Dictionary name
     */
    @Schema(description = "Dictionary name")
    private String dictName;

    /**
     * Dictionary type
     */
    @Schema(description = "Dictionary type")
    private String dictType;

    /**
     * Description
     */
    @Schema(description = "Description")
    private String description;

    /**
     * Sort order
     */
    @Schema(description = "Sort order")
    private Integer sort;

    /**
     * Status: 0-disabled, 1-normal
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createTime;

    /**
     * Update time
     */
    @Schema(description = "Update time")
    private LocalDateTime updateTime;
}
```

#### 7.6.10 Dictionary Data VO

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/vo/DictDataVO.java`:

```java
package com.knowledge.base.foundation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Dictionary data VO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used to return
 * dictionary data information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dictionary data response")
public class DictDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Dictionary data ID
     */
    @Schema(description = "Dictionary data ID")
    private Long id;

    /**
     * Dictionary ID
     */
    @Schema(description = "Dictionary ID")
    private Long dictId;

    /**
     * Dictionary code (redundant)
     */
    @Schema(description = "Dictionary code")
    private String dictCode;

    /**
     * Dictionary label
     */
    @Schema(description = "Dictionary label")
    private String dictLabel;

    /**
     * Dictionary value
     */
    @Schema(description = "Dictionary value")
    private String dictValue;

    /**
     * Sort order
     */
    @Schema(description = "Sort order")
    private Integer dictSort;

    /**
     * CSS class name
     */
    @Schema(description = "CSS class name")
    private String cssClass;

    /**
     * List style
     */
    @Schema(description = "List style")
    private String listClass;

    /**
     * Is default: 0-no, 1-yes
     */
    @Schema(description = "Is default")
    private Integer isDefault;

    /**
     * Status: 0-disabled, 1-normal
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Creation time
     */
    @Schema(description = "Creation time")
    private LocalDateTime createTime;
}
```

#### 7.6.11 System Configuration VO

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/vo/SystemConfigVO.java`:

```java
package com.knowledge.base.foundation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * System configuration VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "System configuration VO")
public class SystemConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Config ID")
    private Long id;

    @Schema(description = "Config key")
    private String configKey;

    @Schema(description = "Config value")
    private String configValue;

    @Schema(description = "Config type")
    private String configType;

    @Schema(description = "Config category")
    private String category;

    @Schema(description = "Config description")
    private String description;

    @Schema(description = "Is public")
    private Integer isPublic;

    @Schema(description = "Creation time")
    private LocalDateTime createTime;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;
}
```

#### 7.6.12 Notification VO

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/vo/NotificationVO.java`:

```java
package com.knowledge.base.foundation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Notification VO
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification VO")
public class NotificationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Notification ID")
    private Long id;

    @Schema(description = "Recipient user ID")
    private Long userId;

    @Schema(description = "User name")
    private String userName;

    @Schema(description = "Notification type")
    private String notificationType;

    @Schema(description = "Notification title")
    private String title;

    @Schema(description = "Notification content")
    private String content;

    @Schema(description = "Redirect link")
    private String link;

    @Schema(description = "Related type")
    private String relatedType;

    @Schema(description = "Related ID")
    private Long relatedId;

    @Schema(description = "Whether read")
    private Integer isRead;

    @Schema(description = "Read time")
    private LocalDateTime readTime;

    @Schema(description = "Creation time")
    private LocalDateTime createTime;
}
```

#### 7.6.13 Operation Log VO

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/vo/OperationLogVO.java`:

```java
package com.knowledge.base.foundation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Operation log VO
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; used to return
 * operation log information</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Operation log response")
public class OperationLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Log ID
     */
    @Schema(description = "Log ID")
    private Long id;

    /**
     * Module name
     */
    @Schema(description = "Module name")
    private String module;

    /**
     * Operation type: LOGIN/CREATE/UPDATE/DELETE, etc.
     */
    @Schema(description = "Operation type")
    private String operationType;

    /**
     * Operation description
     */
    @Schema(description = "Operation description")
    private String operationDesc;

    /**
     * Request method: GET/POST/PUT/DELETE
     */
    @Schema(description = "Request method")
    private String requestMethod;

    /**
     * Request URL
     */
    @Schema(description = "Request URL")
    private String requestUrl;

    /**
     * Request parameters (JSON)
     */
    @Schema(description = "Request parameters")
    private String requestParams;

    /**
     * Response result (JSON)
     */
    @Schema(description = "Response result")
    private String responseResult;

    /**
     * Operator user ID
     */
    @Schema(description = "Operator user ID")
    private Long userId;

    /**
     * Operator username
     */
    @Schema(description = "Operator username")
    private String username;

    /**
     * IP address
     */
    @Schema(description = "IP address")
    private String ipAddress;

    /**
     * Location
     */
    @Schema(description = "Location")
    private String location;

    /**
     * User agent
     */
    @Schema(description = "User agent")
    private String userAgent;

    /**
     * Execution time (milliseconds)
     */
    @Schema(description = "Execution time (ms)")
    private Integer executeTime;

    /**
     * Status: 0-failed, 1-succeeded
     */
    @Schema(description = "Status")
    private Integer status;

    /**
     * Error message
     */
    @Schema(description = "Error message")
    private String errorMsg;

    /**
     * Operation time
     */
    @Schema(description = "Operation time")
    private LocalDateTime createTime;
}
```

---

## IX. Data Access Layer

### 8.1 Dictionary Mapper

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/mapper/DictMapper.java`:

```java
package com.knowledge.base.foundation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.foundation.entity.Dict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Dictionary type Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface DictMapper extends BaseMapper<Dict> {

    @Select("SELECT * FROM kb_dict WHERE dict_code = #{dictCode}")
    Dict selectByDictCode(@Param("dictCode") String dictCode);

    @Select("SELECT * FROM kb_dict WHERE dict_type = #{dictType} AND status = 1 ORDER BY sort")
    List<Dict> selectByDictType(@Param("dictType") String dictType);
}
```

### 8.2 Dictionary Data Mapper

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/mapper/DictDataMapper.java`:

```java
package com.knowledge.base.foundation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.foundation.entity.DictData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Dictionary data Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface DictDataMapper extends BaseMapper<DictData> {

    @Select("SELECT * FROM kb_dict_data WHERE dict_code = #{dictCode} AND status = 1 ORDER BY dict_sort")
    List<DictData> selectByDictCode(@Param("dictCode") String dictCode);

    @Select("SELECT * FROM kb_dict_data WHERE dict_id = #{dictId} AND status = 1 ORDER BY dict_sort")
    List<DictData> selectByDictId(@Param("dictId") Long dictId);
}
```

### 8.3 System Configuration Mapper

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/mapper/SystemConfigMapper.java`:

```java
package com.knowledge.base.foundation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.foundation.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * System configuration Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    @Select("SELECT * FROM kb_system_config WHERE config_key = #{configKey} AND deleted = 0")
    SystemConfig selectByConfigKey(@Param("configKey") String configKey);

    @Select("SELECT * FROM kb_system_config WHERE category = #{category} AND deleted = 0 ORDER BY id")
    List<SystemConfig> selectByCategory(@Param("category") String category);
}
```

### 8.4 Notification Mapper

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/mapper/NotificationMapper.java`:

```java
package com.knowledge.base.foundation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.foundation.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Notification Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * Count a user's unread notifications
     *
     * @param userId the user ID
     * @return the unread count
     */
    Long countUnreadByUserId(@Param("userId") Long userId);
}
```

### 8.5 Operation Log Mapper

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/mapper/OperationLogMapper.java`:

```java
package com.knowledge.base.foundation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.foundation.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Operation log Mapper interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    /**
     * Delete logs older than the specified date
     *
     * @param date the date string
     * @return the number of deleted rows
     */
    int deleteBeforeDate(@Param("date") String date);
}
```

---

## X. Business Logic Layer

### 9.1 Dictionary Service

Create the interface `kb-foundation/src/main/java/com/knowledge/base/foundation/service/DictService.java`:

```java
package com.knowledge.base.foundation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.foundation.dto.DictDTO;
import com.knowledge.base.foundation.entity.Dict;
import com.knowledge.base.foundation.entity.DictData;
import com.knowledge.base.foundation.vo.DictDataVO;
import com.knowledge.base.foundation.vo.DictVO;

import java.util.List;

/**
 * Dictionary service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface DictService {

    /**
     * Paginated query of the dictionary type list
     *
     * @param current the current page
     * @param size    the page size
     * @param keyword the search keyword
     * @return paginated dictionary information
     */
    IPage<Dict> pageDicts(Long current, Long size, String keyword);

    /**
     * Query dictionary details by dictionary code
     *
     * @param code the dictionary code
     * @return the dictionary details
     */
    Dict getDictByCode(String code);

    /**
     * Create a dictionary
     *
     * @param dict the dictionary information
     * @return whether it succeeded
     */
    Boolean createDict(Dict dict);

    /**
     * Update a dictionary
     *
     * @param code the dictionary code
     * @param dict the dictionary information
     * @return whether it succeeded
     */
    Boolean updateDict(String code, Dict dict);

    /**
     * Delete a dictionary
     *
     * @param code the dictionary code
     * @return whether it succeeded
     */
    Boolean deleteDict(String code);

    /**
     * Get the dictionary data list
     *
     * @param code the dictionary code
     * @return the dictionary data list
     */
    List<DictData> getDictData(String code);

    /**
     * Add dictionary data
     *
     * @param code     the dictionary code
     * @param dictData the dictionary data
     * @return whether it succeeded
     */
    Boolean addDictData(String code, DictData dictData);

    /**
     * Update dictionary data
     *
     * @param code     the dictionary code
     * @param dictData the dictionary data
     * @return whether it succeeded
     */
    Boolean updateDictData(String code, DictData dictData);

    /**
     * Delete dictionary data
     *
     * @param code the dictionary code
     * @param id   the data ID
     * @return whether it succeeded
     */
    Boolean deleteDictData(String code, Long id);
}
```

Create the implementation class `kb-foundation/src/main/java/com/knowledge/base/foundation/service/impl/DictServiceImpl.java`:

```java
package com.knowledge.base.foundation.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.foundation.entity.Dict;
import com.knowledge.base.foundation.entity.DictData;
import com.knowledge.base.foundation.mapper.DictDataMapper;
import com.knowledge.base.foundation.mapper.DictMapper;
import com.knowledge.base.foundation.service.DictService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dictionary service implementation class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

    @Resource
    private DictMapper dictMapper;

    @Resource
    private DictDataMapper dictDataMapper;

    @Override
    public IPage<Dict> pageDicts(Long current, Long size, String keyword) {
        log.info("Paginated query of dictionaries: current={}, size={}, keyword={}", current, size, keyword);

        LambdaQueryWrapper<Dict> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(Dict::getDictCode, keyword)
                    .or()
                    .like(Dict::getDictName, keyword);
        }

        wrapper.orderByAsc(Dict::getSort);

        Page<Dict> page = new Page<>(current, size);
        return dictMapper.selectPage(page, wrapper);
    }

    @Override
    public Dict getDictByCode(String code) {
        log.info("Get dictionary by code: code={}", code);

        if (!StringUtils.hasText(code)) {
            throw new BusinessException("Dictionary code must not be empty");
        }

        return dictMapper.selectByDictCode(code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean createDict(Dict dict) {
        log.info("Create dictionary: code={}, name={}", dict.getDictCode(), dict.getDictName());

        if (!StringUtils.hasText(dict.getDictCode())) {
            throw new BusinessException("Dictionary code must not be empty");
        }
        if (!StringUtils.hasText(dict.getDictName())) {
            throw new BusinessException("Dictionary name must not be empty");
        }

        Dict existDict = dictMapper.selectByDictCode(dict.getDictCode());
        if (existDict != null) {
            throw new BusinessException("Dictionary code already exists");
        }

        dict.setId(SnowflakeIdGenerator.getInstance().nextId());
        dict.setCreateTime(LocalDateTime.now());
        dict.setUpdateTime(LocalDateTime.now());

        int count = dictMapper.insert(dict);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateDict(String code, Dict dict) {
        log.info("Update dictionary: code={}", code);

        Dict existDict = dictMapper.selectByDictCode(code);
        if (existDict == null) {
            throw new BusinessException("Dictionary does not exist");
        }

        existDict.setDictName(dict.getDictName());
        existDict.setDescription(dict.getDescription());
        existDict.setSort(dict.getSort());
        existDict.setStatus(dict.getStatus());
        existDict.setUpdateTime(LocalDateTime.now());

        int count = dictMapper.updateById(existDict);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDict(String code) {
        log.info("Delete dictionary: code={}", code);

        Dict existDict = dictMapper.selectByDictCode(code);
        if (existDict == null) {
            throw new BusinessException("Dictionary does not exist");
        }

        // Delete the dictionary data
        LambdaQueryWrapper<DictData> dataWrapper = new LambdaQueryWrapper<>();
        dataWrapper.eq(DictData::getDictId, existDict.getId());
        dictDataMapper.delete(dataWrapper);

        // Delete the dictionary type
        int count = dictMapper.deleteById(existDict.getId());
        return count > 0;
    }

    @Override
    public List<DictData> getDictData(String code) {
        log.info("Get dictionary data: code={}", code);

        if (!StringUtils.hasText(code)) {
            throw new BusinessException("Dictionary code must not be empty");
        }

        return dictDataMapper.selectByDictCode(code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addDictData(String code, DictData dictData) {
        log.info("Add dictionary data: code={}, label={}", code, dictData.getDictLabel());

        Dict dict = dictMapper.selectByDictCode(code);
        if (dict == null) {
            throw new BusinessException("Dictionary does not exist");
        }

        dictData.setId(SnowflakeIdGenerator.getInstance().nextId());
        dictData.setDictId(dict.getId());
        dictData.setCreateTime(LocalDateTime.now());

        int count = dictDataMapper.insert(dictData);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateDictData(String code, DictData dictData) {
        log.info("Update dictionary data: code={}, id={}", code, dictData.getId());

        int count = dictDataMapper.updateById(dictData);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDictData(String code, Long id) {
        log.info("Delete dictionary data: code={}, id={}", code, id);

        int count = dictDataMapper.deleteById(id);
        return count > 0;
    }
}
```

### 9.2 System Configuration Service

Create the interface `kb-foundation/src/main/java/com/knowledge/base/foundation/service/SystemConfigService.java`:

```java
package com.knowledge.base.foundation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.foundation.entity.SystemConfig;

import java.util.List;
import java.util.Map;

/**
 * System configuration service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface SystemConfigService {

    /**
     * Paginated query of the config list
     *
     * @param current  the current page
     * @param size     the page size
     * @param category the config category
     * @return paginated config information
     */
    IPage<SystemConfig> pageConfigs(Long current, Long size, String category);

    /**
     * Query a config item by config key
     *
     * @param key the config key
     * @return the config information
     */
    SystemConfig getConfigByKey(String key);

    /**
     * Create a config
     *
     * @param config the config information
     * @return whether it succeeded
     */
    Boolean createConfig(SystemConfig config);

    /**
     * Update a config
     *
     * @param key    the config key
     * @param config the config information
     * @return whether it succeeded
     */
    Boolean updateConfig(String key, SystemConfig config);

    /**
     * Delete a config
     *
     * @param key the config key
     * @return whether it succeeded
     */
    Boolean deleteConfig(String key);

    /**
     * Get configs by category
     *
     * @param category the config category
     * @return the config list
     */
    List<SystemConfig> getConfigsByCategory(String category);

    /**
     * Get public configs
     *
     * @return a Map of public configs
     */
    Map<String, String> getPublicConfigs();
}
```

Create the implementation class `kb-foundation/src/main/java/com/knowledge/base/foundation/service/impl/SystemConfigServiceImpl.java`:

```java
package com.knowledge.base.foundation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.foundation.entity.SystemConfig;
import com.knowledge.base.foundation.mapper.SystemConfigMapper;
import com.knowledge.base.foundation.service.SystemConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * System configuration service implementation class
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Override
    public IPage<SystemConfig> pageConfigs(Long current, Long size, String category) {
        log.info("Paginated query of configs: current={}, size={}, category={}", current, size, category);

        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(category)) {
            wrapper.eq(SystemConfig::getCategory, category);
        }

        wrapper.orderByAsc(SystemConfig::getId);

        Page<SystemConfig> page = new Page<>(current, size);
        return systemConfigMapper.selectPage(page, wrapper);
    }

    @Override
    @Cacheable(value = "systemConfig", key = "#key")
    public SystemConfig getConfigByKey(String key) {
        log.info("Get config: key={}", key);

        if (!StringUtils.hasText(key)) {
            throw new BusinessException("Config key must not be empty");
        }

        return systemConfigMapper.selectByConfigKey(key);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "systemConfig", allEntries = true)
    public Boolean createConfig(SystemConfig config) {
        log.info("Create config: key={}", config.getConfigKey());

        if (!StringUtils.hasText(config.getConfigKey())) {
            throw new BusinessException("Config key must not be empty");
        }

        SystemConfig existConfig = systemConfigMapper.selectByConfigKey(config.getConfigKey());
        if (existConfig != null) {
            throw new BusinessException("Config key already exists");
        }

        config.setId(SnowflakeIdGenerator.getInstance().nextId());
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());

        int count = systemConfigMapper.insert(config);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "systemConfig", allEntries = true)
    public Boolean updateConfig(String key, SystemConfig config) {
        log.info("Update config: key={}", key);

        SystemConfig existConfig = systemConfigMapper.selectByConfigKey(key);
        if (existConfig == null) {
            throw new BusinessException("Config does not exist");
        }

        existConfig.setConfigValue(config.getConfigValue());
        existConfig.setDescription(config.getDescription());
        existConfig.setCategory(config.getCategory());
        existConfig.setIsPublic(config.getIsPublic());
        existConfig.setUpdateTime(LocalDateTime.now());

        int count = systemConfigMapper.updateById(existConfig);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "systemConfig", allEntries = true)
    public Boolean deleteConfig(String key) {
        log.info("Delete config: key={}", key);

        SystemConfig existConfig = systemConfigMapper.selectByConfigKey(key);
        if (existConfig == null) {
            throw new BusinessException("Config does not exist");
        }

        int count = systemConfigMapper.deleteById(existConfig.getId());
        return count > 0;
    }

    @Override
    public List<SystemConfig> getConfigsByCategory(String category) {
        log.info("Get configs by category: category={}", category);

        if (!StringUtils.hasText(category)) {
            throw new BusinessException("Config category must not be empty");
        }

        return systemConfigMapper.selectByCategory(category);
    }

    @Override
    @Cacheable(value = "publicConfigs")
    public Map<String, String> getPublicConfigs() {
        log.info("Get public configs");

        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getIsPublic, 1);

        List<SystemConfig> configs = systemConfigMapper.selectList(wrapper);

        Map<String, String> result = new HashMap<>();
        for (SystemConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }
}
```

---

## XI. Controller Layer

### 10.1 Dictionary Management Controller

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/controller/DictController.java`:

```java
package com.knowledge.base.foundation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.foundation.entity.Dict;
import com.knowledge.base.foundation.entity.DictData;
import com.knowledge.base.foundation.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dictionary Controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/dicts")
@Tag(name = "Dictionary Management", description = "Dictionary data management endpoints")
public class DictController {

    @Resource
    private DictService dictService;

    /**
     * Paginated query of the dictionary type list
     *
     * @param current the current page
     * @param size    the page size
     * @param keyword the search keyword
     * @return paginated dictionary information
     */
    @GetMapping
    @Operation(summary = "Paginated query of dictionaries", description = "Paginated query of the dictionary type list")
    public Result<IPage<Dict>> pageDicts(
        @Parameter(description = "Current page") @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Long size,
        @Parameter(description = "Search keyword") @RequestParam(required = false) String keyword) {
        log.info("Paginated dictionary query request: current={}, size={}, keyword={}", current, size, keyword);

        IPage<Dict> page = dictService.pageDicts(current, size, keyword);
        return Result.success(page);
    }

    /**
     * Query dictionary details by dictionary code
     *
     * @param code the dictionary code
     * @return the dictionary details
     */
    @GetMapping("/{code}")
    @Operation(summary = "Query dictionary details", description = "Query dictionary details by dictionary code")
    public Result<Dict> getDictByCode(
        @Parameter(description = "Dictionary code", required = true)
        @PathVariable String code) {
        log.info("Query dictionary details request: code={}", code);

        Dict dict = dictService.getDictByCode(code);
        return Result.success(dict);
    }

    /**
     * Get the dictionary data list
     *
     * @param code the dictionary code
     * @return the dictionary data list
     */
    @GetMapping("/{code}/data")
    @Operation(summary = "Get dictionary data", description = "Get the dictionary data list by dictionary code")
    public Result<List<DictData>> getDictData(
        @Parameter(description = "Dictionary code", required = true)
        @PathVariable String code) {
        log.info("Get dictionary data request: code={}", code);

        List<DictData> dataList = dictService.getDictData(code);
        return Result.success(dataList);
    }

    /**
     * Create a dictionary
     *
     * @param dict the dictionary information
     * @return whether it succeeded
     */
    @PostMapping
    @Operation(summary = "Create dictionary", description = "Create a new dictionary type")
    public Result<Boolean> createDict(@Valid @RequestBody Dict dict) {
        log.info("Create dictionary request: code={}, name={}", dict.getDictCode(), dict.getDictName());

        Boolean success = dictService.createDict(dict);
        return Result.success("Dictionary created successfully", success);
    }

    /**
     * Update a dictionary
     *
     * @param code the dictionary code
     * @param dict the dictionary information
     * @return whether it succeeded
     */
    @PutMapping("/{code}")
    @Operation(summary = "Update dictionary", description = "Update dictionary type information")
    public Result<Boolean> updateDict(
        @Parameter(description = "Dictionary code", required = true)
        @PathVariable String code,
        @Valid @RequestBody Dict dict) {
        log.info("Update dictionary request: code={}", code);

        Boolean success = dictService.updateDict(code, dict);
        return Result.success("Dictionary updated successfully", success);
    }

    /**
     * Delete a dictionary
     *
     * @param code the dictionary code
     * @return whether it succeeded
     */
    @DeleteMapping("/{code}")
    @Operation(summary = "Delete dictionary", description = "Delete a dictionary by dictionary code")
    public Result<Boolean> deleteDict(
        @Parameter(description = "Dictionary code", required = true)
        @PathVariable String code) {
        log.info("Delete dictionary request: code={}", code);

        Boolean success = dictService.deleteDict(code);
        return Result.success("Dictionary deleted successfully", success);
    }

    /**
     * Add dictionary data
     *
     * @param code     the dictionary code
     * @param dictData the dictionary data
     * @return whether it succeeded
     */
    @PostMapping("/{code}/data")
    @Operation(summary = "Add dictionary data", description = "Add a data item to the specified dictionary")
    public Result<Boolean> addDictData(
        @Parameter(description = "Dictionary code", required = true)
        @PathVariable String code,
        @Valid @RequestBody DictData dictData) {
        log.info("Add dictionary data request: code={}, label={}", code, dictData.getDictLabel());

        Boolean success = dictService.addDictData(code, dictData);
        return Result.success("Dictionary data added successfully", success);
    }

    /**
     * Update dictionary data
     *
     * @param code     the dictionary code
     * @param dictData the dictionary data
     * @return whether it succeeded
     */
    @PutMapping("/{code}/data")
    @Operation(summary = "Update dictionary data", description = "Update a dictionary data item")
    public Result<Boolean> updateDictData(
        @Parameter(description = "Dictionary code", required = true)
        @PathVariable String code,
        @Valid @RequestBody DictData dictData) {
        log.info("Update dictionary data request: code={}, id={}", code, dictData.getId());

        Boolean success = dictService.updateDictData(code, dictData);
        return Result.success("Dictionary data updated successfully", success);
    }

    /**
     * Delete dictionary data
     *
     * @param code the dictionary code
     * @param id   the data ID
     * @return whether it succeeded
     */
    @DeleteMapping("/{code}/data/{id}")
    @Operation(summary = "Delete dictionary data", description = "Delete the specified dictionary data item")
    public Result<Boolean> deleteDictData(
        @Parameter(description = "Dictionary code", required = true)
        @PathVariable String code,
        @Parameter(description = "Data ID", required = true)
        @PathVariable Long id) {
        log.info("Delete dictionary data request: code={}, id={}", code, id);

        Boolean success = dictService.deleteDictData(code, id);
        return Result.success("Dictionary data deleted successfully", success);
    }
}
```

### 10.2 System Configuration Controller

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/controller/SystemConfigController.java`:

```java
package com.knowledge.base.foundation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.foundation.entity.SystemConfig;
import com.knowledge.base.foundation.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * System configuration Controller
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/config")
@Tag(name = "System Configuration Management", description = "System configuration management endpoints")
public class SystemConfigController {

    @Resource
    private SystemConfigService systemConfigService;

    /**
     * Paginated query of the config list
     *
     * @param current  the current page
     * @param size     the page size
     * @param category the config category
     * @return paginated config information
     */
    @GetMapping
    @Operation(summary = "Paginated query of configs", description = "Paginated query of the system configuration list")
    public Result<IPage<SystemConfig>> pageConfigs(
        @Parameter(description = "Current page") @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Long size,
        @Parameter(description = "Config category") @RequestParam(required = false) String category) {
        log.info("Paginated config query request: current={}, size={}, category={}", current, size, category);

        IPage<SystemConfig> page = systemConfigService.pageConfigs(current, size, category);
        return Result.success(page);
    }

    /**
     * Query a config item by config key
     *
     * @param key the config key
     * @return the config information
     */
    @GetMapping("/{key}")
    @Operation(summary = "Query config item", description = "Query a config item by config key")
    public Result<SystemConfig> getConfigByKey(
        @Parameter(description = "Config key", required = true)
        @PathVariable String key) {
        log.info("Query config item request: key={}", key);

        SystemConfig config = systemConfigService.getConfigByKey(key);
        return Result.success(config);
    }

    /**
     * Create a config
     *
     * @param config the config information
     * @return whether it succeeded
     */
    @PostMapping
    @Operation(summary = "Create config", description = "Create a new system config")
    public Result<Boolean> createConfig(@Valid @RequestBody SystemConfig config) {
        log.info("Create config request: key={}", config.getConfigKey());

        Boolean success = systemConfigService.createConfig(config);
        return Result.success("Config created successfully", success);
    }

    /**
     * Update a config
     *
     * @param key    the config key
     * @param config the config information
     * @return whether it succeeded
     */
    @PutMapping("/{key}")
    @Operation(summary = "Update config", description = "Update a system config")
    public Result<Boolean> updateConfig(
        @Parameter(description = "Config key", required = true)
        @PathVariable String key,
        @Valid @RequestBody SystemConfig config) {
        log.info("Update config request: key={}", key);

        Boolean success = systemConfigService.updateConfig(key, config);
        return Result.success("Config updated successfully", success);
    }

    /**
     * Delete a config
     *
     * @param key the config key
     * @return whether it succeeded
     */
    @DeleteMapping("/{key}")
    @Operation(summary = "Delete config", description = "Delete a config by config key")
    public Result<Boolean> deleteConfig(
        @Parameter(description = "Config key", required = true)
        @PathVariable String key) {
        log.info("Delete config request: key={}", key);

        Boolean success = systemConfigService.deleteConfig(key);
        return Result.success("Config deleted successfully", success);
    }

    /**
     * Get configs by category
     *
     * @param category the config category
     * @return the config list
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "Get configs by category", description = "Get the config list by config category")
    public Result<List<SystemConfig>> getConfigsByCategory(
        @Parameter(description = "Config category", required = true)
        @PathVariable String category) {
        log.info("Get configs by category request: category={}", category);

        List<SystemConfig> configs = systemConfigService.getConfigsByCategory(category);
        return Result.success(configs);
    }

    /**
     * Get public configs
     *
     * @return a Map of public configs
     */
    @GetMapping("/public")
    @Operation(summary = "Get public configs", description = "Get all public system configs")
    public Result<Map<String, String>> getPublicConfigs() {
        log.info("Get public configs request");

        Map<String, String> configs = systemConfigService.getPublicConfigs();
        return Result.success(configs);
    }
}
```

### 10.3 Notification Management Controller

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/controller/NotificationController.java`:

```java
package com.knowledge.base.foundation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.foundation.entity.Notification;
import com.knowledge.base.foundation.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification Management", description = "System notification management endpoints")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Paginated query of notifications", description = "Paginated query of the notification list")
    public Result<IPage<Notification>> pageNotifications(
        @Parameter(description = "Current page") @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Long size,
        @Parameter(description = "User ID") @RequestParam(required = false) Long userId,
        @Parameter(description = "Whether read") @RequestParam(required = false) Integer isRead) {
        log.info("Paginated notification query request: current={}, size={}, userId={}, isRead={}", current, size, userId, isRead);

        IPage<Notification> page = notificationService.pageNotifications(current, size, userId, isRead);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Query notification details", description = "Query notification details by notification ID")
    public Result<Notification> getNotificationById(
        @Parameter(description = "Notification ID", required = true)
        @PathVariable Long id) {
        log.info("Query notification details request: id={}", id);

        Notification notification = notificationService.getNotificationById(id);
        return Result.success(notification);
    }

    @PostMapping
    @Operation(summary = "Send notification", description = "Create a new notification")
    public Result<Boolean> sendNotification(@Valid @RequestBody Notification notification) {
        log.info("Send notification request: userId={}, title={}", notification.getUserId(), notification.getTitle());

        Boolean success = notificationService.sendNotification(notification);
        return Result.success("Notification sent successfully", success);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark as read", description = "Mark a notification as read")
    public Result<Boolean> markAsRead(
        @Parameter(description = "Notification ID", required = true)
        @PathVariable Long id) {
        log.info("Mark notification as read request: id={}", id);

        return notificationService.markAsRead(id);
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all as read", description = "Mark all of the user's unread notifications as read")
    public Result<Boolean> markAllAsRead(
        @Parameter(description = "User ID", required = true)
        @RequestParam Long userId) {
        log.info("Mark all as read request: userId={}", userId);

        return notificationService.markAllAsRead(userId);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification", description = "Delete a notification by notification ID")
    public Result<Boolean> deleteNotification(
        @Parameter(description = "Notification ID", required = true)
        @PathVariable Long id) {
        log.info("Delete notification request: id={}", id);

        return notificationService.deleteNotification(id);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count", description = "Get the user's unread notification count")
    public Result<Long> getUnreadCount(
        @Parameter(description = "User ID", required = true)
        @RequestParam Long userId) {
        log.info("Get unread count request: userId={}", userId);

        return notificationService.getUnreadCount(userId);
    }
}
```

### 10.4 Operation Log Controller

Create `kb-foundation/src/main/java/com/knowledge/base/foundation/controller/OperationLogController.java`:

```java
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
     * @param current       the current page
     * @param size          the page size
     * @param module        the module name
     * @param operationType the operation type
     * @param username      the username
     * @param startTime     the start time
     * @param endTime       the end time
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
     * @param id the log ID
     * @return the log details
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
     * @param startTime the start time
     * @param endTime   the end time
     * @return the statistics information
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
     * @return the number of deleted rows
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
```

---

## XII. Data Initialization

### 11.1 Run the Database Initialization Script

Run the initialization script in the project root directory:

```bash
mysql -u root -p < sql/init_kb_foundation.sql
```

### 11.2 Verify the Data

Check whether the database tables were created successfully:

```bash
mysql -u root -p kb_foundation -e "SHOW TABLES;"
```

Expected output:
```
+-------------------------+
| Tables_in_kb_foundation  |
+-------------------------+
| kb_dict                  |
| kb_dict_data             |
| kb_notification          |
| kb_operation_log         |
| kb_system_config         |
+-------------------------+
```

---

## XIII. Testing and Verification

### 12.1 Start the Service

```bash
# Option 1: start via Maven
cd kb-foundation
mvn spring-boot:run

# Option 2: start via IDE
# Open FoundationApplication.java in IDEA and click Run
```

Once started successfully, the console prints:

```
========================================
   Foundation service started successfully!
   Service name: kb-foundation
   Service port: 8089
   API docs: http://localhost:8089/api/foundation/doc.html
   Druid monitor: http://localhost:8089/api/foundation/druid/
========================================
```

### 12.2 Access the API Docs

Open a browser and visit: `http://localhost:8089/api/foundation/doc.html`

### 12.3 Test the Endpoints

#### Test the Dictionary Management Endpoints

**Create a dictionary type:**
```bash
curl -X POST http://localhost:8089/api/foundation/dicts \
  -H "Content-Type: application/json" \
  -d '{
    "dictCode": "test_dict",
    "dictName": "Test Dictionary",
    "dictType": "TEST",
    "description": "A dictionary for testing",
    "sort": 0,
    "status": 1
  }'
```

**Get dictionary data:**
```bash
curl http://localhost:8089/api/foundation/dicts/test_dict/data
```

#### Test the System Configuration Endpoints

**Create a config:**
```bash
curl -X POST http://localhost:8089/api/foundation/config \
  -H "Content-Type: application/json" \
  -d '{
    "configKey": "test.config",
    "configValue": "test_value",
    "configType": "string",
    "category": "TEST",
    "description": "Test config",
    "isPublic": 1
  }'
```

**Get public configs:**
```bash
curl http://localhost:8089/api/foundation/config/public
```

#### Test the Notification Endpoints

**Send a notification:**
```bash
curl -X POST http://localhost:8089/api/foundation/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "userName": "Test User",
    "notificationType": "system",
    "title": "Test Notification",
    "content": "This is a test notification",
    "link": "/test"
  }'
```

**Get the unread count:**
```bash
curl http://localhost:8089/api/foundation/notifications/unread-count?userId=1
```

---

## XIV. Summary

This article described in detail the process of building the foundation service module, covering the following core content:

### Completed Features

| Module | Feature | Description |
|------|------|------|
| Dictionary management | CRUD, data management | Provides system dictionary data management |
| System configuration | CRUD, category query, caching | Supports multiple config types, with caching |
| Notification management | Sending, marking as read, unread statistics | Supports multiple notification types |
| Operation logs | Recording, querying, statistics, cleanup | Complete audit log functionality |
| Infrastructure | Redis, RabbitMQ, WebSocket | Supports caching, message queuing, and real-time communication |

### Technical Highlights

1. **Clear layered architecture**: strictly follows the Controller → Service → Mapper three-layer architecture
2. **Caching support**: uses Spring Cache + Redis to implement config caching
3. **Message queue**: integrates RabbitMQ for asynchronous notifications
4. **Real-time communication**: uses WebSocket + STOMP for real-time push
5. **API docs**: uses Knife4j to auto-generate endpoint documentation

### Future Improvements

1. Add an AOP aspect to automatically record operation logs
2. Implement WebSocket notification push
3. Add broadcasting of dictionary data change events
4. Implement hot-reloading of configuration
5. Add data permission control

Through this article, you should now be able to grasp:
- How to organize a Spring Boot multi-module project
- Advanced usage of MyBatis Plus
- Integrating and using Redis caching
- Configuring the RabbitMQ message queue
- Implementing real-time communication with WebSocket

Happy building!
