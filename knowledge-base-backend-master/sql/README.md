# Enterprise Knowledge Base System - Database Scripts

This directory contains all the database scripts for the Enterprise Knowledge Base System.

## 📁 File Descriptions

| File | Description | Execution Order |
|--------|------|----------|
| `01_create_tables.sql` | Creates all database table structures | 1 |
| `02_init_data.sql` | Seeds enterprise-level data | 2 |

## 🚀 Quick Start

### 1. Create the database

```sql
CREATE DATABASE IF NOT EXISTS `knowledge_base`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

### 2. Run the table creation script

```bash
mysql -u root -p knowledge_base < 01_create_tables.sql
```

Or in the MySQL client:

```sql
USE knowledge_base;
SOURCE /path/to/01_create_tables.sql;
```

### 3. Seed data

```bash
mysql -u root -p knowledge_base < 02_init_data.sql
```

Or in the MySQL client:

```sql
USE knowledge_base;
SOURCE /path/to/02_init_data.sql;
```

## 📊 Database Table Structure

### Core Table Summary

| Module | Table Count | Description |
|------|--------|------|
| User Authentication Module (kb-user-auth) | 6 tables | Users, roles, permissions, team management |
| Document Management Module (kb-document) | 8 tables | Documents, categories, tags, comments, reviews, versions |
| Search Module (kb-search) | 1 table | Search history |
| File Management Module (kb-file) | 1 table | File information management |
| Notification Module (kb-notification) | 1 table | System notifications |
| AI Module (kb-ai) | 3 tables | AI conversations, messages, feedback |
| Statistics Module (kb-statistics) | 4 tables | Document, user, comment, and view statistics |
| Common Module (kb-common) | 5 tables | Operation logs, system configuration, dictionaries |
| **Total** | **29 tables** | - |

### Table Relationships

```
kb_user (user table)
  ├── kb_user_role (user-role association)
  │     └── kb_role (role table)
  │           └── kb_role_permission (role-permission association)
  │                 └── kb_permission (permission table)
  ├── kb_team_member (team members)
  │     └── kb_team (team table)
  └── kb_document (document table)
        ├── kb_category (category table)
        ├── kb_document_tag (document-tag association)
        │     └── kb_tag (tag table)
        ├── kb_document_version (document versions)
        ├── kb_comment (comment table)
        ├── kb_document_review (document review)
        └── kb_file (file table)
```

## 👤 Default Accounts

After system initialization, the following test accounts are available:

| Username | Password | Role | Description |
|--------|------|------|------|
| admin | admin123 | Super Admin | Has all permissions |
| editor | admin123 | Editor | Can edit and manage documents |
| tester | admin123 | Regular User | Test account |
| developer | admin123 | Regular User | Developer account |
| product | admin123 | Regular User | Product manager account |
| designer | admin123 | Regular User | UI designer account |
| sales | admin123 | Regular User | Sales manager account |
| hr | admin123 | Regular User | HR specialist account |
| finance | admin123 | Regular User | Finance manager account |
| guest | admin123 | Guest | Read-only access |

⚠️ **Important**: Please change the default passwords immediately after deploying to production!

## 📦 Seed Data Description

### Data Included

1. **User data**: 10 test users, covering different departments
2. **Roles and permissions**: 6 roles + 36 permissions (menus, buttons, APIs)
3. **Document categories**: 8 top-level categories + multiple subcategories
4. **Tag data**: 12 common tags
5. **Team data**: 4 centers + 3 development teams
6. **Document data**: 9 sample documents (technical, product, process, etc.)
7. **Comment data**: 8 sample comments
8. **System configuration**: 12 system config items
9. **Dictionary data**: 2 dictionary types + 6 dictionary entries
10. **Notification data**: 4 sample notifications
11. **AI conversations**: 3 conversations + 5 messages
12. **Statistics data**: document and user statistics

### Document Category Structure

```
Technical Documentation (💻)
├── Backend Development (🔧)
├── Frontend Development (🎨)
├── Database (🗄️)
├── DevOps (🚀)
└── Architecture Design (🏗️)

Product Documentation (📦)
├── Product Requirements (📝)
├── UI Design (🎭)
├── Product Planning (🎯)
└── Competitive Analysis (🔍)

Business Processes (📋)
Human Resources (👥)
Financial Policies (💰)
Marketing (📈)
Legal & Compliance (⚖️)
Training Materials (📚)
```

## 🔧 Maintenance Notes

### Backing up the database

```bash
mysqldump -u root -p knowledge_base > kb_backup_$(date +%Y%m%d).sql
```

### Restoring the database

```bash
mysql -u root -p knowledge_base < kb_backup_20240101.sql
```

### Clearing test data

To clear all data while keeping the table structure:

```sql
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE kb_ai_conversation;
TRUNCATE TABLE kb_ai_feedback;
TRUNCATE TABLE kb_ai_message;
TRUNCATE TABLE kb_comment;
TRUNCATE TABLE kb_comment_statistics;
TRUNCATE TABLE kb_dict;
TRUNCATE TABLE kb_dict_data;
TRUNCATE TABLE kb_document;
TRUNCATE TABLE kb_document_review;
TRUNCATE TABLE kb_document_statistics;
TRUNCATE TABLE kb_document_tag;
TRUNCATE TABLE kb_document_version;
TRUNCATE TABLE kb_file;
TRUNCATE TABLE kb_notification;
TRUNCATE TABLE kb_operation_log;
TRUNCATE TABLE kb_permission;
TRUNCATE TABLE kb_role;
TRUNCATE TABLE kb_role_permission;
TRUNCATE TABLE kb_search_history;
TRUNCATE TABLE kb_system_config;
TRUNCATE TABLE kb_tag;
TRUNCATE TABLE kb_team;
TRUNCATE TABLE kb_team_member;
TRUNCATE TABLE kb_user;
TRUNCATE TABLE kb_user_role;
TRUNCATE TABLE kb_user_statistics;
TRUNCATE TABLE kb_view_record;
TRUNCATE TABLE kb_category;
SET FOREIGN_KEY_CHECKS = 1;
```

## 📝 Notes

1. **Character set**: Databases and tables consistently use the `utf8mb4` character set
2. **Collation**: Uses the `utf8mb4_unicode_ci` collation
3. **Time fields**: Consistently use the `DATETIME` type
4. **Primary key type**: Uses `BIGINT`, paired with the Snowflake algorithm to generate distributed IDs
5. **Soft deletes**: Core business tables include a `deleted` field to implement soft deletion
6. **Audit fields**: Includes `create_time`, `update_time`, `create_by`, `update_by`
7. **Index optimization**: Appropriate indexes have been added for commonly queried fields
8. **Full-text search**: The document table includes a full-text index for content search

## 🔄 Version History

| Version | Date | Description |
|------|------|------|
| v1.0.0 | 2024-01-01 | Initial version, created the complete table structure and seed data |

## 📞 Technical Support

If you have any questions, please contact the technical team or file an Issue.
