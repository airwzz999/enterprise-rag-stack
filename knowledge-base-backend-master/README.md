# Knowledge Base System Backend

Enterprise knowledge base management system backend built on Spring Boot 3.x + MyBatis Plus.

## Project Overview

This project uses a Maven multi-module architecture, providing complete user permission management and document management functionality.

### Tech Stack

- **Framework**: Spring Boot 3.2.0
- **Database**: MySQL 8.0+
- **ORM**: MyBatis Plus 3.5.5
- **Cache**: Redis 7.2 (optional)
- **API Docs**: Knife4j 4.3.0
- **Utility Library**: Hutool 5.8.24
- **JDK**: Java 21

### Module Overview

| Module | Port | Description |
|-----|------|------|
| kb-gateway | 8080 | API Gateway |
| kb-user-auth | 8081 | User & Auth Service |
| kb-document | 8082 | Document Service |
| kb-common | - | Common module |

## Quick Start

### Requirements

- JDK 21+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.2+ (optional)

### Database Initialization

1. Create the database and run the initialization script:

```bash
mysql -u root -p < sql/init.sql
```

2. Default admin account:
   - Username: admin
   - Password: 123456

### Modify Configuration

Modify the `application.yml` file under each service module to configure the database connection information:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/knowledge_base
    username: root
    password: 123456
```

### Starting the Services

#### Option 1: Start from IDE

Start the following services in order from your IDE:
1. GatewayApplication (8080)
2. UserAuthApplication (8081)
3. DocumentApplication (8082)

#### Option 2: Start from the Command Line

```bash
# Build the project
mvn clean package

# Start the gateway
java -jar kb-gateway/target/kb-gateway-1.0.0-SNAPSHOT.jar

# Start the user & auth service
java -jar kb-user-auth/target/kb-user-auth-1.0.0-SNAPSHOT.jar

# Start the document service
java -jar kb-document/target/kb-document-1.0.0-SNAPSHOT.jar
```

### Accessing the API Docs

- **User & Auth Service**: http://localhost:8081/api/auth/doc.html
- **Document Service**: http://localhost:8082/api/document/doc.html

## Project Structure

```
knowledge-base-backend/
├── kb-common/              # Common module
│   ├── annotation/         # Custom annotations
│   ├── aspect/             # Aspects
│   ├── config/             # Configuration classes
│   ├── constants/          # Constant definitions
│   ├── enums/              # Enum classes
│   ├── exception/          # Exception classes
│   ├── handler/            # Handlers
│   ├── result/             # Response result wrapper
│   └── utils/              # Utility classes
├── kb-gateway/             # API Gateway
│   └── filter/             # Gateway filters
├── kb-user-auth/           # User & Auth Service
│   ├── controller/         # Controllers
│   ├── service/            # Service layer
│   ├── mapper/             # Data access layer
│   ├── entity/             # Entity classes
│   ├── dto/                # Data transfer objects
│   └── vo/                 # View objects
└── kb-document/            # Document Service
    ├── controller/         # Controllers
    ├── service/            # Service layer
    ├── mapper/             # Data access layer
    ├── entity/             # Entity classes
    ├── dto/                # Data transfer objects
    └── vo/                 # View objects
```

## Code Conventions

This project strictly follows the Alibaba Java Development Guidelines:

1. **Naming Conventions**
   - Class names use UpperCamelCase
   - Method and variable names use lowerCamelCase
   - Constants use ALL_CAPS with underscores

2. **Comment Conventions**
   - All classes must have JavaDoc comments
   - All public methods must have JavaDoc comments
   - Complex business logic must have inline comments

3. **Exception Handling**
   - Use a unified exception handling mechanism
   - Define custom business exceptions
   - Never catch `Throwable`

4. **Logging Conventions**
   - Use Slf4j for logging
   - Set log levels sensibly
   - Disable DEBUG logging in production

## Core Features

### User & Auth Service

- User login/logout
- User management (CRUD)
- Role management
- Permission management
- Token management (JWT)

### Document Service

- Document management (CRUD)
- Document categorization
- File upload/download
- Document search
- Document view/like/favorite
- Rich text editor support

## Development Guide

### Adding a New Endpoint

1. Define request parameters in a DTO
2. Define response data in a VO
3. Define the business method in the Service interface
4. Implement the business logic in the ServiceImpl
5. Expose the REST endpoint in the Controller

### ID Generation

All IDs are generated using the Snowflake algorithm:

```java
Long id = SnowflakeIdGenerator.getInstance().nextId();
```

### Unified Response

All endpoints return responses in a unified format:

```java
return Result.success(data);
return Result.error(ResultCode.PARAM_ERROR);
```

### Exception Handling

Use custom exceptions:

```java
throw new BusinessException(ResultCode.USER_NOT_EXIST);
throw new UnauthorizedException("Invalid token");
```

## Deployment

### Docker Deployment

```bash
# Build the image
docker build -t knowledge-base-backend:1.0.0 .

# Start the containers
docker-compose up -d
```

### Production Configuration

The following must be configured for production:

1. Update the database connection information
2. Configure the Redis connection
3. Update the JWT secret
4. Disable the Swagger docs
5. Configure the log output path

## License

Apache License 2.0

## Contact

- Project home: https://github.com/knowledge-base/backend
- Issue tracker: https://github.com/knowledge-base/backend/issues
- Email: support@knowledge-base.com
