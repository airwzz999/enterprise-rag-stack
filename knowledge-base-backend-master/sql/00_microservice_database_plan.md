# Enterprise Knowledge Base System - Microservice Database Split Plan

## 📊 Architecture Design Principles

Follows the **Database per Service** pattern, where each microservice has its own independent database, achieving:
- Service data isolation
- Independent deployment and scaling
- Fault isolation
- Technology stack flexibility

## 🗄️ Database Split Plan

### Service-to-Database Mapping

| Microservice | Database Name | Port | Tables Included | Description |
|--------|-----------|------|--------|------|
| kb-user-auth | kb_user | 8081 | Users, roles, permissions, teams | User authentication and authorization |
| kb-document | kb_document | 8082 | Documents, categories, tags, comments, reviews, versions | Core document management |
| kb-search | kb_search | 8083 | Search history | Search service |
| kb-file | kb_file | 8084 | File information | File storage |
| kb-ai | kb_ai | 8086 | AI conversations, messages, feedback | AI service |
| kb-statistics | kb_statistics | 8085 | Statistics data | Data statistics |
| kb-notification | kb_notification | 8087 | Notification messages | Notification service |
| kb-graph | kb_graph | 8088 | Graph nodes and relationships | Knowledge graph |
| kb-common | kb_common | - | Operation logs, system configuration, dictionaries | Common module (shared) |

### Database Details

#### 1. kb_user (user authentication database)
```
📦 kb_user
├── kb_user                  # User table
├── kb_role                  # Role table
├── kb_permission            # Permission table
├── kb_user_role             # User-role association table
├── kb_role_permission       # Role-permission association table
├── kb_team                  # Team table
└── kb_team_member           # Team member table
```

#### 2. kb_document (document management database)
```
📦 kb_document
├── kb_document              # Document table
├── kb_category              # Document category table
├── kb_tag                   # Document tag table
├── kb_document_tag          # Document-tag association table
├── kb_comment               # Document comment table
├── kb_document_review       # Document review table
└── kb_document_version      # Document version table
```

#### 3. kb_search (search service database)
```
📦 kb_search
└── kb_search_history        # Search history table
```

#### 4. kb_file (file service database)
```
📦 kb_file
└── kb_file                  # File information table
```

#### 5. kb_ai (AI service database)
```
📦 kb_ai
├── kb_ai_conversation       # AI conversation table
├── kb_ai_message            # AI message table
└── kb_ai_feedback           # AI feedback table
```

#### 6. kb_statistics (statistics service database)
```
📦 kb_statistics
├── kb_document_statistics   # Document statistics table
├── kb_user_statistics       # User statistics table
├── kb_view_record           # View record table
└── kb_comment_statistics    # Comment statistics table
```

#### 7. kb_notification (notification service database)
```
📦 kb_notification
└── kb_notification          # System notification table
```

#### 8. kb_graph (graph service database)
```
📦 kb_graph
├── kb_graph_node            # Graph node table (optional, primarily uses Neo4j)
├── kb_graph_edge            # Graph edge table (optional, primarily uses Neo4j)
└── kb_graph_community       # Graph community table
```

#### 9. kb_common (common module database)
```
📦 kb_common
├── kb_operation_log         # Operation log table
├── kb_system_config         # System configuration table
├── kb_dict                  # Dictionary table
└── kb_dict_data             # Dictionary data table
```

## 🔗 Cross-Service Association Handling

### Inter-Service Communication Methods

| Scenario | Solution | Example |
|------|----------|------|
| User queries documents | Via the document service API | documentService.getByAuthorId(userId) |
| Add comment to a document | The document service calls the user service to get user info | userService.getUserById(userId) |
| Aggregate statistics data | The statistics service reads data from each service | Data synced via a message queue |
| Global search | The search service indexes data from each service | Elasticsearch syncs data from each service |

### Distributed Transaction Handling

| Scenario | Solution | Description |
|------|----------|------|
| Document creation + statistics update | Asynchronous message queue | After a document is created, a message is sent; the statistics service consumes it and updates |
| User deletion + data cleanup | Saga pattern | Executed step by step, with compensation on failure |
| Document review + notification | Event-driven | An event is published when review completes; the notification service subscribes |

### Data Consistency Strategy

1. **Eventual consistency**: Guaranteed via message queues
2. **Compensation mechanism**: Saga pattern for handling distributed transactions
3. **Idempotent design**: All APIs support idempotent calls
4. **Event sourcing**: Key operations are recorded as event logs

## 📁 Database Creation Scripts

### Execution Order

```bash
# 1. Create all databases
mysql -u root -p123456 < sql/00_create_databases.sql

# 2. Create the table structure for each database
mysql -u root -p123456 kb_user < sql/01_kb_user.sql
mysql -u root -p123456 kb_document < sql/02_kb_document.sql
mysql -u root -p123456 kb_search < sql/03_kb_search.sql
mysql -u root -p123456 kb_file < sql/04_kb_file.sql
mysql -u root -p123456 kb_ai < sql/05_kb_ai.sql
mysql -u root -p123456 kb_statistics < sql/06_kb_statistics.sql
mysql -u root -p123456 kb_notification < sql/07_kb_notification.sql
mysql -u root -p123456 kb_graph < sql/08_kb_graph.sql
mysql -u root -p123456 kb_common < sql/09_kb_common.sql

# 3. Seed data for each database
mysql -u root -p123456 kb_user < sql/init_kb_user.sql
mysql -u root -p123456 kb_document < sql/init_kb_document.sql
# ... other seed scripts
```

## ⚙️ Configuration File Changes

### Per-Service Configuration

Each microservice's application.yml configures its corresponding database name:

```yaml
# kb-user-auth
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/kb_user

# kb-document
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/kb_document

# kb-search
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/kb_search
```

## 🚀 Migration Steps

### Migrating from a monolithic database to microservice databases

1. **Back up existing data**
   ```bash
   mysqldump -u root -p123456 knowledge_base > kb_backup.sql
   ```

2. **Create the new databases**
   ```bash
   mysql -u root -p123456 < sql/00_create_databases.sql
   ```

3. **Migrate table structures in stages**
   ```bash
   # Run each database creation script in order
   ```

4. **Migrate data**
   - Use mysqldump to export individual tables
   - Import them into the corresponding new database
   - Or use a data migration tool

5. **Update configuration files**
   - Modify all application.yml files
   - Update database names

6. **Test and verify**
   - Unit tests
   - Integration tests
   - End-to-end tests

## 📊 Database Connection Pool Configuration

Configure the connection pool independently for each service:

```yaml
spring:
  datasource:
    druid:
      initial-size: 5      # Adjust based on service load
      min-idle: 5
      max-active: 20       # Can be configured differently per service
      max-wait: 60000
```

### Recommended Connection Pool Sizes

| Service | Initial Connections | Min Idle | Max Active |
|------|----------|----------|----------|
| user-auth | 10 | 5 | 50 |
| document | 20 | 10 | 100 |
| search | 5 | 5 | 30 |
| statistics | 5 | 5 | 20 |

## 🔒 Security Recommendations

1. **Database user isolation**
   ```sql
   -- Create a dedicated database user for each service
   CREATE USER 'kb_user'@'%' IDENTIFIED BY 'kb_user_2024';
   CREATE USER 'kb_document'@'%' IDENTIFIED BY 'kb_document_2024';
   -- Grant privileges
   GRANT ALL ON kb_user.* TO 'kb_user'@'%';
   GRANT ALL ON kb_document.* TO 'kb_document'@'%';
   ```

2. **Network isolation**
   - Use VPC isolation in production
   - Only allow inter-service access over the internal network

3. **Backup strategy**
   - Back up each database independently
   - Use different retention policies as needed

## 📝 Notes

1. **Distributed ID generation**
   - Ensure all services use the Snowflake algorithm to generate globally unique IDs
   - Avoid ID collisions

2. **Foreign key handling**
   - The microservice architecture does not use cross-database foreign keys
   - Data consistency is guaranteed at the application layer

3. **Transaction boundaries**
   - Transactions are scoped to a single service
   - Cross-service transactions use eventual consistency

4. **Monitoring and alerting**
   - Monitor the connection count and performance of each database
   - Set reasonable alert thresholds
