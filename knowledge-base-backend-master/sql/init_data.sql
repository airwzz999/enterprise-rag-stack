-- =====================================================
-- Enterprise Knowledge Base System - complete seed data script (DML)
-- =====================================================
-- Version: 1.0
-- Database: MySQL 8.0+
-- Character set: utf8mb4
-- =====================================================
-- Execution notes:
--   1. Must be run after create_tables.sql
--   2. Can be run as any MySQL user with existing privileges
--   3. INSERT statements are ordered according to data dependencies
--   4. All data uses Snowflake algorithm IDs, which are unique
--   5. Default admin password: admin123 (BCrypt encrypted)
-- =====================================================

SET NAMES utf8mb4;


-- =====================================================
-- Part 1: kb_user database - user authentication module
-- =====================================================

USE `kb_user`;

-- 1.1 Seed user data
-- Password is uniformly admin123 (BCrypt: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi)
INSERT INTO `kb_user` (`id`, `username`, `password`, `email`, `real_name`, `department`, `position`, `status`, `avatar`) VALUES
(1000000000000000001, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@company.com', 'System Administrator', 'Technology', 'System Architect', 1, 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin'),
(1000000000000000002, 'editor', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'editor@company.com', 'Content Editor', 'Content', 'Senior Editor', 1, 'https://api.dicebear.com/7.x/avataaars/svg?seed=editor'),
(1000000000000000003, 'tester', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'tester@company.com', 'Test Engineer', 'QA', 'Test Engineer', 1, 'https://api.dicebear.com/7.x/avataaars/svg?seed=tester'),
(1000000000000000004, 'developer', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'dev@company.com', 'Development Engineer', 'R&D', 'Senior Engineer', 1, 'https://api.dicebear.com/7.x/avataaars/svg?seed=developer'),
(1000000000000000005, 'product', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'product@company.com', 'Product Manager', 'Product', 'Senior Product Manager', 1, 'https://api.dicebear.com/7.x/avataaars/svg?seed=product'),
(1000000000000000006, 'designer', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'designer@company.com', 'UI Designer', 'Design', 'Senior Designer', 1, 'https://api.dicebear.com/7.x/avataaars/svg?seed=designer'),
(1000000000000000007, 'sales', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'sales@company.com', 'Sales Manager', 'Sales', 'Sales Manager', 1, 'https://api.dicebear.com/7.x/avataaars/svg?seed=sales'),
(1000000000000000008, 'hr', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'hr@company.com', 'HR Specialist', 'Human Resources', 'HR Specialist', 1, 'https://api.dicebear.com/7.x/avataaars/svg?seed=hr'),
(1000000000000000009, 'finance', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'finance@company.com', 'Finance Manager', 'Finance', 'Finance Manager', 1, 'https://api.dicebear.com/7.x/avataaars/svg?seed=finance'),
(1000000000000000010, 'guest', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'guest@company.com', 'Guest User', 'External', 'Guest', 1, 'https://api.dicebear.com/7.x/avataaars/svg?seed=guest');

-- 1.2 Seed role data
INSERT INTO `kb_role` (`id`, `role_name`, `role_code`, `description`, `sort`, `status`) VALUES
(2000000000000000001, 'Super Admin', 'ROLE_SUPER_ADMIN', 'Has all system permissions', 1, 1),
(2000000000000000002, 'Admin', 'ROLE_ADMIN', 'Has system administration permissions', 2, 1),
(2000000000000000003, 'Editor', 'ROLE_EDITOR', 'Can edit and manage documents', 3, 1),
(2000000000000000004, 'Reviewer', 'ROLE_REVIEWER', 'Can review documents', 4, 1),
(2000000000000000005, 'Regular User', 'ROLE_USER', 'Regular user permissions', 5, 1),
(2000000000000000006, 'Guest', 'ROLE_GUEST', 'Read-only guest access', 6, 1);

-- 1.3 Seed permission data (top-level menus)
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`) VALUES
(3000000000000000001, 0, 'Dashboard', 'dashboard', 1, '/dashboard', 'DashboardOutlined', 1, 1),
(3000000000000000002, 0, 'Document Center', 'document', 1, '/documents', 'FileTextOutlined', 2, 1),
(3000000000000000046, 0, 'File Management', 'file', 1, '/files', 'FolderOpenOutlined', 3, 1),
(3000000000000000003, 0, 'Knowledge Graph', 'graph', 1, '/knowledge-graph', 'NodeIndexOutlined', 4, 1),
(3000000000000000005, 0, 'Search', 'search', 1, '/search', 'SearchOutlined', 5, 1),
(3000000000000000004, 0, 'AI Assistant', 'ai', 1, '/ai', 'RobotOutlined', 6, 1),
(3000000000000000047, 0, 'AI Writing', 'ai-writing', 1, '/ai-writing', 'EditOutlined', 7, 1),
(3000000000000000006, 0, 'Notification Center', 'notification', 1, '/notifications', 'BellOutlined', 8, 1),
(3000000000000000007, 0, 'Personal Center', 'profile', 1, '/profile', 'UserOutlined', 9, 1),
(3000000000000000008, 0, 'System Management', 'system', 1, '/admin', 'SettingOutlined', 10, 1);

-- 1.4 Seed permission data (Document Management submenus)
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`) VALUES
(3000000000000000011, 3000000000000000002, 'Document List', 'document:list', 1, '/documents', NULL, 1, 1),
(3000000000000000012, 3000000000000000002, 'Create Document', 'document:create', 2, NULL, NULL, 2, 1),
(3000000000000000013, 3000000000000000002, 'Edit Document', 'document:edit', 2, NULL, NULL, 3, 1),
(3000000000000000014, 3000000000000000002, 'Delete Document', 'document:delete', 2, NULL, NULL, 4, 1),
(3000000000000000015, 3000000000000000002, 'Document Review', 'document:review', 2, NULL, NULL, 5, 1),
(3000000000000000016, 3000000000000000002, 'Document Category', 'document:category', 1, '/admin/categories', NULL, 6, 1),
(3000000000000000017, 3000000000000000002, 'Document Tags', 'document:tag', 1, '/admin/tags', NULL, 7, 1),
(3000000000000000018, 3000000000000000016, 'Query Categories', 'document:category:query', 3, NULL, NULL, 1, 1);

-- 1.5 Seed permission data (File Management submenus)
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`) VALUES
(3000000000000000048, 3000000000000000046, 'File List', 'file:list', 1, '/files', NULL, 1, 1),
(3000000000000000049, 3000000000000000046, 'Upload File', 'file:upload', 2, NULL, NULL, 2, 1),
(3000000000000000050, 3000000000000000046, 'Delete File', 'file:delete', 2, NULL, NULL, 3, 1);

-- 1.6 Seed permission data (System Management submenus)
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`) VALUES
(3000000000000000021, 3000000000000000008, 'User Management', 'system:user', 1, '/admin/users', NULL, 1, 1),
(3000000000000000022, 3000000000000000008, 'Role Management', 'system:role', 1, '/admin/roles', NULL, 2, 1),
(3000000000000000023, 3000000000000000008, 'Permission Management', 'system:permission', 1, '/admin/permissions', NULL, 3, 1),
(3000000000000000024, 3000000000000000008, 'Team Management', 'system:team', 1, '/admin/teams', NULL, 4, 1),
(3000000000000000025, 3000000000000000008, 'Data Statistics', 'system:statistics', 1, '/admin/statistics', NULL, 5, 1),
(3000000000000000026, 3000000000000000008, 'Review Management', 'system:review', 1, '/admin/review', NULL, 6, 1),
(3000000000000000027, 3000000000000000008, 'System Settings', 'system:settings', 1, '/admin/settings', NULL, 7, 1),
(3000000000000000051, 3000000000000000008, 'System Configuration', 'system:config', 1, '/admin/system-config', NULL, 8, 1),
(3000000000000000052, 3000000000000000008, 'Dictionary Management', 'system:dictionary', 1, '/admin/dictionary', NULL, 9, 1),
(3000000000000000053, 3000000000000000008, 'Operation Logs', 'system:operation-log', 1, '/admin/operation-logs', NULL, 10, 1),
(3000000000000000054, 3000000000000000008, 'Notification Templates', 'system:notification-template', 1, '/admin/notification-templates', NULL, 11, 1);

-- 1.7 Seed permission data (API permissions)
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `api_url`, `method`, `sort`, `status`) VALUES
(3000000000000000031, 0, 'Document Query API', 'api:document:query', 3, '/api/document/**', 'GET', 1, 1),
(3000000000000000032, 0, 'Document Create API', 'api:document:create', 3, '/api/document', 'POST', 2, 1),
(3000000000000000033, 0, 'Document Update API', 'api:document:update', 3, '/api/document/**', 'PUT', 3, 1),
(3000000000000000034, 0, 'Document Delete API', 'api:document:delete', 3, '/api/document/**', 'DELETE', 4, 1),
(3000000000000000035, 0, 'User Management API', 'api:user:manage', 3, '/api/user/**', '*', 5, 1),
(3000000000000000036, 0, 'Role Management API', 'api:role:manage', 3, '/api/role/**', '*', 6, 1);

-- 1.8 Seed user-role associations
INSERT INTO `kb_user_role` (`id`, `user_id`, `role_id`, `create_by`) VALUES
(4000000000000000001, 1000000000000000001, 2000000000000000001, 1000000000000000001),
(4000000000000000002, 1000000000000000002, 2000000000000000003, 1000000000000000001),
(4000000000000000003, 1000000000000000003, 2000000000000000005, 1000000000000000001),
(4000000000000000004, 1000000000000000004, 2000000000000000005, 1000000000000000001),
(4000000000000000005, 1000000000000000005, 2000000000000000005, 1000000000000000001),
(4000000000000000006, 1000000000000000006, 2000000000000000005, 1000000000000000001),
(4000000000000000007, 1000000000000000007, 2000000000000000005, 1000000000000000001),
(4000000000000000008, 1000000000000000008, 2000000000000000005, 1000000000000000001),
(4000000000000000009, 1000000000000000009, 2000000000000000005, 1000000000000000001),
(4000000000000000010, 1000000000000000010, 2000000000000000006, 1000000000000000001);

-- 1.9 Seed role-permission associations (super admin has all permissions)
INSERT INTO `kb_role_permission` (`id`, `role_id`, `permission_id`)
SELECT
    5000000000000000000 + ROW_NUMBER() OVER (ORDER BY `id`),
    2000000000000000001,
    `id`
FROM `kb_permission`;

-- 1.10 Seed team data
INSERT INTO `kb_team` (`id`, `team_name`, `team_code`, `description`, `icon`, `leader_id`, `parent_id`, `sort`) VALUES
(8000000000000000001, 'Technology Center', 'TECH_CENTER', 'Responsible for all company technical R&D work', 'tech', 1000000000000000004, 0, 1),
(8000000000000000002, 'Product Center', 'PRODUCT_CENTER', 'Responsible for product planning and design', 'product', 1000000000000000005, 0, 2),
(8000000000000000003, 'Operations Center', 'OPS_CENTER', 'Responsible for business operations and marketing', 'ops', 1000000000000000007, 0, 3),
(8000000000000000004, 'Administrative Center', 'ADMIN_CENTER', 'Responsible for company administrative, HR, and finance work', 'admin', 1000000000000000008, 0, 4),
(8000000000000000005, 'Backend Development Team', 'BACKEND_TEAM', 'Backend system development', 'backend', 1000000000000000004, 8000000000000000001, 1),
(8000000000000000006, 'Frontend Development Team', 'FRONTEND_TEAM', 'Frontend system development', 'frontend', 1000000000000000004, 8000000000000000001, 2),
(8000000000000000007, 'QA Team', 'QA_TEAM', 'Quality assurance and testing', 'qa', 1000000000000000003, 8000000000000000001, 3);

-- 1.11 Seed team members
INSERT INTO `kb_team_member` (`id`, `team_id`, `user_id`, `member_role`) VALUES
(9000000000000000001, 8000000000000000005, 1000000000000000004, 'leader'),
(9000000000000000002, 8000000000000000005, 1000000000000000001, 'member'),
(9000000000000000003, 8000000000000000006, 1000000000000000006, 'member'),
(9000000000000000004, 8000000000000000007, 1000000000000000003, 'leader');


-- =====================================================
-- Part 2: kb_document database - document management module
-- =====================================================

USE `kb_document`;

-- 2.1 Seed document category data
INSERT INTO `kb_category` (`id`, `category_name`, `parent_id`, `category_icon`, `description`, `sort`, `document_count`) VALUES
-- Top-level categories
(6000000000000000001, 'Technical Documentation', 0, 'tech', 'Documentation related to technical development', 1, 0),
(6000000000000000002, 'Product Documentation', 0, 'product', 'Product design and requirements documents', 2, 0),
(6000000000000000003, 'Business Processes', 0, 'business', 'Company business process guidelines', 3, 0),
(6000000000000000004, 'Human Resources', 0, 'hr', 'HR policies and management guidelines', 4, 0),
(6000000000000000005, 'Financial Policies', 0, 'finance', 'Financial management policies and processes', 5, 0),
(6000000000000000006, 'Marketing', 0, 'marketing', 'Marketing strategies and plans', 6, 0),
(6000000000000000007, 'Legal & Compliance', 0, 'legal', 'Laws, regulations, and compliance requirements', 7, 0),
(6000000000000000008, 'Training Materials', 0, 'training', 'Employee training and learning materials', 8, 0),
-- Technical Documentation subcategories
(6000000000000000011, 'Backend Development', 6000000000000000001, 'backend', 'Backend technology stack development documentation', 1, 0),
(6000000000000000012, 'Frontend Development', 6000000000000000001, 'frontend', 'Frontend technology stack development documentation', 2, 0),
(6000000000000000013, 'Database', 6000000000000000001, 'database', 'Database design and optimization', 3, 0),
(6000000000000000014, 'DevOps', 6000000000000000001, 'devops', 'Operations, deployment, and CI/CD', 4, 0),
(6000000000000000015, 'Architecture Design', 6000000000000000001, 'architecture', 'System architecture design documentation', 5, 0),
-- Product Documentation subcategories
(6000000000000000021, 'Product Requirements', 6000000000000000002, 'requirement', 'Product Requirements Document (PRD)', 1, 0),
(6000000000000000022, 'UI Design', 6000000000000000002, 'design', 'UI/UX design guidelines', 2, 0),
(6000000000000000023, 'Product Planning', 6000000000000000002, 'planning', 'Product planning and roadmap', 3, 0),
(6000000000000000024, 'Competitive Analysis', 6000000000000000002, 'competitive', 'Competitive analysis reports', 4, 0);

-- 2.2 Seed tag data
INSERT INTO `kb_tag` (`id`, `tag_name`, `tag_color`, `description`, `use_count`) VALUES
(7000000000000000001, 'Important', '#ff4d4f', 'Important document tag', 0),
(7000000000000000002, 'Pinned', '#1890ff', 'Pinned document tag', 0),
(7000000000000000003, 'Recommended', '#52c41a', 'Recommended document tag', 0),
(7000000000000000004, 'Draft', '#d9d9d9', 'Draft document tag', 0),
(7000000000000000005, 'Java', '#b07219', 'Java technology tag', 0),
(7000000000000000006, 'Spring Boot', '#6db33f', 'Spring Boot tag', 0),
(7000000000000000007, 'React', '#61dafb', 'React frontend tag', 0),
(7000000000000000008, 'MySQL', '#4479a1', 'MySQL database tag', 0),
(7000000000000000009, 'Redis', '#dc382d', 'Redis cache tag', 0),
(7000000000000000010, 'Docker', '#2496ed', 'Docker container tag', 0),
(7000000000000000011, 'Architecture', '#722ed1', 'System architecture tag', 0),
(7000000000000000012, 'Guidelines', '#fa8c16', 'Development guidelines tag', 0);

-- 2.3 Seed document data
INSERT INTO `kb_document` (`id`, `title`, `content`, `summary`, `category_id`, `author_id`, `author_name`, `status`, `is_public`, `view_count`, `like_count`, `comment_count`, `version`, `publish_time`) VALUES
(1000000000000000001,
'Spring Boot 3.x Quick Start Guide',
'# Spring Boot 3.x Quick Start Guide\n\n## Project Initialization\n\nCreate a project using Spring Initializr:\n\n```xml\n<dependency>\n    <groupId>org.springframework.boot</groupId>\n    <artifactId>spring-boot-starter-web</artifactId>\n</dependency>\n```\n\n## Core Features\n\n### 1. Auto-configuration\nAuto-configuration in Spring Boot greatly simplifies development work...\n\n### 2. Starter Dependencies\nProvides a series of starter dependencies...\n\n### 3. Command-Line Interface\nComes with a built-in spring command-line tool...\n\n## Best Practices\n\n- Follow convention over configuration\n- Use a well-layered architecture\n- Use a configuration center',
'A complete Spring Boot 3.x beginner tutorial, covering project initialization, core feature introductions, and best practices.',
6000000000000000011, 1000000000000000004, 'developer', 'published', 1, 1523, 89, 23, 1, '2024-01-15 10:00:00'),

(1000000000000000002,
'React 18 + TypeScript Best Practices',
'# React 18 + TypeScript Best Practices\n\n## Project Structure\n\n```\nsrc/\n├── components/     # Shared components\n├── pages/          # Page components\n├── hooks/          # Custom hooks\n├── services/       # API services\n├── stores/         # State management\n├── types/          # Type definitions\n└── utils/          # Utility functions\n```\n\n## Core Concepts\n\n### Function Components\n```typescript\ninterface Props {\n  title: string;\n  count: number;\n}\n\nexport const MyComponent: React.FC<Props> = ({ title, count }) => {\n  return <div>{title}: {count}</div>;\n};\n```\n\n## State Management\nUses Zustand for state management...',
'Frontend development best practices based on React 18 and TypeScript, covering project structure, core concepts, and state management.',
6000000000000000012, 1000000000000000006, 'designer', 'published', 1, 2187, 156, 45, 1, '2024-02-10 14:30:00'),

(1000000000000000003,
'MySQL 8.0 Performance Optimization Guide',
'# MySQL 8.0 Performance Optimization Guide\n\n## Index Optimization\n\n### Index Design Principles\n1. Prioritize creating indexes on highly selective columns\n2. Composite indexes should follow the leftmost prefix principle\n3. Avoid redundant indexes\n\n### Index Creation Examples\n```sql\n-- Create a composite index\nCREATE INDEX idx_user_email ON user(username, email);\n\n-- Create a full-text index\nCREATE FULLTEXT INDEX idx_content ON article(content);\n```\n\n## Query Optimization\n\n### EXPLAIN Analysis\nUse EXPLAIN to analyze the query execution plan...\n\n### Slow Query Log\nConfigure the slow query log to pinpoint performance bottlenecks...',
'A complete guide to MySQL 8.0 database performance optimization, covering index optimization, query optimization, and slow query analysis.',
6000000000000000013, 1000000000000000001, 'admin', 'published', 1, 3421, 234, 67, 1, '2024-01-28 09:15:00'),

(1000000000000000004,
'Docker + Kubernetes Containerized Deployment',
'# Docker + Kubernetes Containerized Deployment\n\n## Docker Basics\n\n### Writing a Dockerfile\n```dockerfile\nFROM openjdk:21-jdk-slim\nWORKDIR /app\nCOPY target/*.jar app.jar\nEXPOSE 8080\nENTRYPOINT ["java", "-jar", "app.jar"]\n```\n\n## Kubernetes Deployment\n\n### Deployment Configuration\n```yaml\napiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: knowledge-base\nspec:\n  replicas: 3\n  selector:\n    matchLabels:\n      app: knowledge-base\n  template:\n    metadata:\n      labels:\n        app: knowledge-base\n    spec:\n      containers:\n      - name: app\n        image: kb-app:latest\n        ports:\n        - containerPort: 8080\n```',
'A containerized microservice deployment practice based on Docker and Kubernetes.',
6000000000000000014, 1000000000000000004, 'developer', 'published', 1, 1876, 98, 19, 1, '2024-03-05 16:20:00'),

(1000000000000000005,
'Enterprise Knowledge Base Product Requirements Document (PRD)',
'# Enterprise Knowledge Base Product Requirements Document\n\n## 1. Product Overview\n\n### 1.1 Product Positioning\nAn enterprise-grade knowledge management platform that helps companies build up knowledge assets and improve collaboration efficiency.\n\n### 1.2 Target Users\n- Employees: need to quickly find and access knowledge\n- Administrators: need to manage and maintain the knowledge base\n- Decision-makers: need knowledge data analytics\n\n## 2. Functional Requirements\n\n### 2.1 Core Features\n1. Document management\n2. Knowledge search\n3. Collaborative editing\n4. Access control\n5. Data statistics\n\n### 2.2 User Experience\n- A clean, intuitive interface\n- Fast, responsive search\n- A convenient editing experience',
'A complete Enterprise Knowledge Base product requirements document, covering product positioning, target users, and functional requirements.',
6000000000000000021, 1000000000000000005, 'product', 'published', 1, 987, 45, 12, 1, '2024-02-01 10:00:00'),

(1000000000000000006,
'UI Design Guidelines V2.0',
'# UI Design Guidelines V2.0\n\n## Color System\n\n### Primary Colors\n- Primary: #1890ff (blue)\n- Success: #52c41a (green)\n- Warning: #faad14 (orange)\n- Error: #ff4d4f (red)\n\n### Neutral Colors\n- Heading: #262626\n- Body text: #595959\n- Secondary: #8c8c8c\n- Disabled: #bfbfbf\n\n## Typography\n\n### Font Sizes\n- Large heading: 24px\n- Medium heading: 18px\n- Body text: 14px\n- Secondary text: 12px\n\n## Component Guidelines\n\n### Buttons\n- Primary button: blue background\n- Secondary button: white background\n- Text button: no border',
'Enterprise Knowledge Base UI design guidelines, covering the color system, typography, and component guidelines.',
6000000000000000022, 1000000000000000006, 'designer', 'published', 1, 654, 34, 8, 1, '2024-02-15 14:00:00'),

(1000000000000000007,
'Document Review Process Guidelines',
'# Document Review Process Guidelines\n\n## 1. Process Overview\n\nDocuments must go through a review process before publishing to ensure content quality.\n\n## 2. Review Process\n\n### 2.1 Submit for Review\nAfter completing a document, the author clicks the "Submit for Review" button.\n\n### 2.2 Reviewer Actions\n- Check content accuracy\n- Check formatting compliance\n- Provide review comments\n\n### 2.3 Review Outcome\n- **Approved**: the document is published automatically\n- **Rejected**: the author revises based on the feedback and resubmits\n\n## 3. Review Standards\n\n### Content Quality\n- Accurate and complete information\n- Clear, well-organized logic\n- Consistent, standardized formatting',
'Detailed guidelines for the document review process, including process steps and review standards.',
6000000000000000003, 1000000000000000002, 'editor', 'published', 1, 1234, 67, 15, 1, '2024-01-20 11:00:00'),

(1000000000000000008,
'Employee Onboarding Guide',
'# Employee Onboarding Guide\n\n## Welcome Aboard!\n\nFirst of all, welcome to our team! Here are some things to know as you get started.\n\n## Onboarding Process\n\n### Day One\n1. Complete HR paperwork\n2. Office environment orientation\n3. Account and access provisioning\n4. Meet your team members\n\n### Week One\n1. Get familiar with business processes\n2. Attend new hire training\n3. Complete basic tasks\n4. One-on-one mentoring with a buddy\n\n## Commonly Used Systems\n\n- Knowledge base system: https://kb.company.com\n- OA office system: https://oa.company.com\n- Email system: mail.company.com\n\n## Benefits\n\n### Social Insurance and Housing Fund\nContributed according to national regulations.\n\n### Paid Annual Leave\n- After 1 year of service: 5 days\n- After 3 years of service: 10 days\n- After 5 years of service: 15 days',
'A new employee onboarding guide, covering the onboarding process, commonly used systems, and benefits information.',
6000000000000000004, 1000000000000000008, 'hr', 'published', 1, 5678, 234, 56, 1, '2024-01-01 09:00:00'),

(1000000000000000009,
'Expense Reimbursement Process Guide',
'# Expense Reimbursement Process Guide\n\n## Reimbursement Principles\n\n1. **Authentic and legal**: receipts must be genuine and valid\n2. **Pre-approval required**: large expenses require prior approval\n3. **Timely reimbursement**: submit within 1 month of the expense being incurred\n\n## Reimbursement Process\n\n### Step 1: Organize Receipts\n- Invoices must be VAT invoices issued to the company\n- Receipt date, amount, and item must be clear\n\n### Step 2: Fill Out the Reimbursement Form\nFill out the reimbursement form in the OA system and upload photos of the receipts.\n\n### Step 3: Approval Process\n- Department manager approval\n- Finance review\n- General manager approval (for amounts > 5000 CNY)\n\n### Step 4: Payment\nOnce approved, payment is made to the payroll card within 3-5 business days.\n\n## Notes\n\n- All receipts should be organized in chronological order\n- Transportation expenses must note the departure and destination\n- Entertainment expenses must note the reason and attendees',
'Detailed explanation of the company expense reimbursement process, covering reimbursement principles, process steps, and notes.',
6000000000000000005, 1000000000000000009, 'finance', 'published', 1, 3456, 123, 34, 1, '2024-01-10 14:00:00');

-- 2.4 Seed document-tag associations
INSERT INTO `kb_document_tag` (`id`, `document_id`, `tag_id`) VALUES
(1100000000000000001, 1000000000000000001, 7000000000000000006),
(1100000000000000002, 1000000000000000001, 7000000000000000005),
(1100000000000000003, 1000000000000000001, 7000000000000000011),
(1100000000000000004, 1000000000000000002, 7000000000000000007),
(1100000000000000005, 1000000000000000002, 7000000000000000012),
(1100000000000000006, 1000000000000000003, 7000000000000000008),
(1100000000000000007, 1000000000000000003, 7000000000000000009),
(1100000000000000008, 1000000000000000004, 7000000000000000010),
(1100000000000000009, 1000000000000000004, 7000000000000000005);

-- 2.5 Seed comment data
INSERT INTO `kb_comment` (`id`, `document_id`, `content`, `user_id`, `user_name`, `parent_id`, `like_count`, `status`) VALUES
(1200000000000000001, 1000000000000000001, 'This article is very detailed and helped me a lot!', 1000000000000000002, 'editor', 0, 12, 1),
(1200000000000000002, 1000000000000000001, 'One suggestion: it would help to go into more detail on how auto-configuration works.', 1000000000000000004, 'developer', 0, 5, 1),
(1200000000000000003, 1000000000000000002, 'The TypeScript type definitions are very well structured, learned a lot!', 1000000000000000003, 'tester', 0, 8, 1),
(1200000000000000004, 1000000000000000002, 'Looking forward to the next article about Hooks.', 1000000000000000002, 'editor', 0, 3, 1),
(1200000000000000005, 1000000000000000003, 'The index optimization tips are very practical, I have already applied them in my project.', 1000000000000000005, 'product', 0, 15, 1),
(1200000000000000006, 1000000000000000005, 'The PRD is written very clearly, the product logic is complete.', 1000000000000000001, 'admin', 0, 6, 1),
(1200000000000000007, 1000000000000000008, 'The onboarding guide is very detailed, it helped me get up to speed with the company quickly.', 1000000000000000003, 'tester', 0, 23, 1),
(1200000000000000008, 1000000000000000008, 'Suggest adding some notes about remote work.', 1000000000000000007, 'sales', 0, 2, 1);


-- =====================================================
-- Part 3: kb_ai database - AI module
-- =====================================================

USE `kb_ai`;

-- 3.1 Seed AI conversation data
INSERT INTO `kb_ai_conversation` (`id`, `user_id`, `user_name`, `title`, `model_name`, `message_count`) VALUES
(1600000000000000001, 1000000000000000001, 'admin', 'Discussion about Spring Boot', 'qwen-turbo', 2),
(1600000000000000002, 1000000000000000002, 'editor', 'Frontend development questions', 'qwen-turbo', 2),
(1600000000000000003, 1000000000000000004, 'developer', 'Database optimization suggestions', 'qwen-turbo', 2);

-- 3.2 Seed AI message data
INSERT INTO `kb_ai_message` (`id`, `conversation_id`, `role`, `content`, `tokens`) VALUES
(1700000000000000001, 1600000000000000001, 'user', 'What is the principle behind Spring Boot auto-configuration?', 20),
(1700000000000000002, 1600000000000000001, 'assistant', 'Spring Boot auto-configuration is implemented via conditional annotations (@ConditionalOnClass, @ConditionalOnMissingBean, etc.). It decides whether to load a given configuration based on the jars present on the classpath and the beans already defined...', 150),
(1700000000000000003, 1600000000000000002, 'user', 'What are the new features in React 18?', 18),
(1700000000000000004, 1600000000000000002, 'assistant', 'The main new features in React 18 include: 1. Concurrent rendering 2. Automatic batching 3. Transitions 4. Suspense improvements...', 120),
(1700000000000000005, 1600000000000000003, 'user', 'How can I optimize MySQL query performance?', 15),
(1700000000000000006, 1600000000000000003, 'assistant', 'MySQL query optimization can start from several angles: 1. Index optimization 2. Query statement optimization 3. Table structure optimization 4. Parameter tuning...', 135);


-- =====================================================
-- Part 4: kb_statistics database - statistics module
-- =====================================================

USE `kb_statistics`;

-- 4.1 Seed document statistics data
INSERT INTO `kb_document_statistics` (`id`, `document_id`, `document_title`, `view_count`, `like_count`, `comment_count`, `collect_count`, `share_count`, `stat_date`) VALUES
(1800000000000000001, 1000000000000000001, 'Spring Boot 3.x Quick Start Guide', 1523, 89, 23, 45, 12, CURDATE()),
(1800000000000000002, 1000000000000000002, 'React 18 + TypeScript Best Practices', 2187, 156, 45, 67, 23, CURDATE()),
(1800000000000000003, 1000000000000000003, 'MySQL 8.0 Performance Optimization Guide', 3421, 234, 67, 89, 34, CURDATE()),
(1800000000000000004, 1000000000000000004, 'Docker + Kubernetes Containerized Deployment', 1876, 98, 19, 34, 8, CURDATE()),
(1800000000000000005, 1000000000000000005, 'Enterprise Knowledge Base Product Requirements Document (PRD)', 987, 45, 12, 23, 5, CURDATE()),
(1800000000000000006, 1000000000000000006, 'UI Design Guidelines V2.0', 654, 34, 8, 12, 3, CURDATE()),
(1800000000000000007, 1000000000000000007, 'Document Review Process Guidelines', 1234, 67, 15, 34, 7, CURDATE()),
(1800000000000000008, 1000000000000000008, 'Employee Onboarding Guide', 5678, 234, 56, 89, 45, CURDATE()),
(1800000000000000009, 1000000000000000009, 'Expense Reimbursement Process Guide', 3456, 123, 34, 56, 21, CURDATE());

-- 4.2 Seed user statistics data
INSERT INTO `kb_user_statistics` (`id`, `user_id`, `user_name`, `document_count`, `comment_count`, `like_count`, `view_count`, `login_count`, `stat_date`) VALUES
(1900000000000000001, 1000000000000000001, 'admin', 3, 15, 45, 2345, 67, CURDATE()),
(1900000000000000002, 1000000000000000004, 'developer', 2, 23, 89, 4523, 89, CURDATE()),
(1900000000000000003, 1000000000000000002, 'editor', 1, 12, 34, 1234, 45, CURDATE()),
(1900000000000000004, 1000000000000000006, 'designer', 1, 8, 34, 876, 23, CURDATE()),
(1900000000000000005, 1000000000000000005, 'product', 1, 6, 23, 1567, 34, CURDATE()),
(1900000000000000006, 1000000000000000003, 'tester', 0, 8, 15, 987, 12, CURDATE());


-- =====================================================
-- Part 5: kb_notification database - notification module
-- =====================================================

USE `kb_notification`;

-- 5.1 Seed notification data
INSERT INTO `kb_notification` (`id`, `user_id`, `user_name`, `notification_type`, `title`, `content`, `link`, `is_read`) VALUES
(1500000000000000001, 1000000000000000002, 'editor', 'system', 'Welcome to the Enterprise Knowledge Base', 'Welcome to the Enterprise Knowledge Base system, start your knowledge management journey!', '/documents', 0),
(1500000000000000002, 1000000000000000004, 'developer', 'comment', 'Your document received a new comment', '"Spring Boot 3.x Quick Start Guide" received a new comment', '/documents/1000000000000000001', 0),
(1500000000000000003, 1000000000000000005, 'product', 'review', 'Document review approved', 'Your "Enterprise Knowledge Base Product Requirements Document (PRD)" has passed review', '/documents/1000000000000000005', 1),
(1500000000000000004, 1000000000000000001, 'admin', 'mention', 'Someone mentioned you', 'developer mentioned you in "Docker + Kubernetes Containerized Deployment"', '/documents/1000000000000000004', 0);


-- =====================================================
-- Part 6: kb_common database - common module
-- =====================================================

USE `kb_common`;

-- 6.1 Seed system configuration data
INSERT INTO `kb_system_config` (`id`, `config_key`, `config_value`, `config_type`, `category`, `description`, `is_public`) VALUES
(1300000000000000001, 'site.name', 'Enterprise Knowledge Base', 'string', 'basic', 'Site name', 1),
(1300000000000000002, 'site.logo', '/logo.png', 'string', 'basic', 'Site logo', 1),
(1300000000000000003, 'site.allowRegister', 'true', 'boolean', 'basic', 'Allow user registration', 1),
(1300000000000000004, 'upload.maxSize', '104857600', 'number', 'upload', 'Maximum upload file size (bytes)', 0),
(1300000000000000005, 'upload.allowTypes', '.doc,.docx,.pdf,.txt,.md,.png,.jpg,.jpeg', 'string', 'upload', 'Allowed file types', 0),
(1300000000000000006, 'security.sessionTimeout', '7200', 'number', 'security', 'Session timeout (seconds)', 0),
(1300000000000000007, 'security.passwordMinLength', '8', 'number', 'security', 'Minimum password length', 0),
(1300000000000000008, 'email.enabled', 'false', 'boolean', 'email', 'Enable email notifications', 0),
(1300000000000000009, 'email.host', 'smtp.example.com', 'string', 'email', 'SMTP server', 0),
(1300000000000000010, 'email.port', '587', 'number', 'email', 'SMTP port', 0),
(1300000000000000011, 'ai.model', 'qwen-turbo', 'string', 'ai', 'AI model name', 0),
(1300000000000000012, 'ai.maxTokens', '2000', 'number', 'ai', 'Max AI token count', 0);

-- 6.2 Seed dictionary type data
INSERT INTO `kb_dict` (`id`, `dict_code`, `dict_name`, `dict_type`, `description`, `sort`) VALUES
(1400000000000000001, 'document_status', 'Document status', 'document', 'Document status enum', 1),
(1400000000000000002, 'review_status', 'Review status', 'review', 'Review status enum', 2),
(1400000000000000003, 'notification_type', 'Notification type', 'notification', 'Notification type enum', 3);

-- 6.3 Seed dictionary data values
INSERT INTO `kb_dict_data` (`id`, `dict_id`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `status`) VALUES
-- Document status
(1400000000000000001, 1400000000000000001, 'Draft', 'draft', 1, 'default', 1),
(1400000000000000002, 1400000000000000001, 'Published', 'published', 2, 'success', 1),
(1400000000000000003, 1400000000000000001, 'Archived', 'archived', 3, 'info', 1),
-- Review status
(1400000000000000004, 1400000000000000002, 'Pending review', 'pending', 1, 'warning', 1),
(1400000000000000005, 1400000000000000002, 'Approved', 'approved', 2, 'success', 1),
(1400000000000000006, 1400000000000000002, 'Rejected', 'rejected', 3, 'error', 1),
-- Notification type
(1400000000000000007, 1400000000000000003, 'System notification', 'system', 1, 'blue', 1),
(1400000000000000008, 1400000000000000003, 'Comment notification', 'comment', 2, 'green', 1),
(1400000000000000009, 1400000000000000003, 'Mention notification', 'mention', 3, 'orange', 1),
(1400000000000000010, 1400000000000000003, 'Review notification', 'review', 4, 'purple', 1),
(1400000000000000011, 1400000000000000003, 'Like notification', 'like', 5, 'red', 1);


-- =====================================================
-- Part 7: kb_foundation database - foundation service
-- =====================================================

USE `kb_foundation`;

-- 7.1 Seed system configuration data
INSERT INTO `kb_system_config` (`id`, `config_key`, `config_value`, `config_type`, `category`, `description`, `is_public`) VALUES
-- AI configuration
(2000000000000000001, 'qwen.api.key', '', 'string', 'AI', 'Qwen API key', 0),
(2000000000000000002, 'qwen.model.name', 'qwen-max', 'string', 'AI', 'Qwen model name', 1),
(2000000000000000003, 'qwen.embedding.model', 'text-embedding-v3', 'string', 'AI', 'Qwen embedding model', 1),
(2000000000000000004, 'milvus.host', 'localhost', 'string', 'AI', 'Milvus host address', 1),
(2000000000000000005, 'milvus.port', '19530', 'number', 'AI', 'Milvus port', 1),
-- Storage configuration
(2000000000000000006, 'rustfs.endpoints', 'http://localhost:8200', 'json', 'STORAGE', 'RustFS endpoint list', 1),
(2000000000000000007, 'rustfs.bucket', 'knowledge-docs', 'string', 'STORAGE', 'RustFS storage bucket', 1),
(2000000000000000008, 'file.upload.max.size', '52428800', 'number', 'STORAGE', 'Maximum file upload size (bytes)', 1),
(2000000000000000009, 'file.upload.allowed.types', 'pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md', 'string', 'STORAGE', 'Allowed file upload types', 1),
-- Notification configuration
(2000000000000000010, 'email.enabled', 'true', 'boolean', 'NOTIFICATION', 'Whether email notifications are enabled', 1),
(2000000000000000011, 'email.host', 'smtp.example.com', 'string', 'NOTIFICATION', 'Mail server address', 0),
(2000000000000000012, 'email.port', '587', 'number', 'NOTIFICATION', 'Mail server port', 0),
(2000000000000000013, 'notification.retention.days', '90', 'number', 'NOTIFICATION', 'Notification retention days', 1),
(2000000000000000014, 'websocket.enabled', 'true', 'boolean', 'NOTIFICATION', 'Whether WebSocket push is enabled', 1),
-- Security configuration
(2000000000000000015, 'auth.session.timeout', '7200', 'number', 'SECURITY', 'Session timeout (seconds)', 1),
(2000000000000000016, 'auth.password.min.length', '8', 'number', 'SECURITY', 'Minimum password length', 1),
(2000000000000000017, 'auth.password.require.special', 'true', 'boolean', 'SECURITY', 'Whether the password requires special characters', 1),
(2000000000000000018, 'auth.login.max.retry', '5', 'number', 'SECURITY', 'Maximum login retry attempts', 1),
-- System configuration
(2000000000000000019, 'system.name', 'Enterprise Knowledge Base', 'string', 'SYSTEM', 'System name', 1),
(2000000000000000020, 'system.version', '1.0.0', 'string', 'SYSTEM', 'System version', 1),
(2000000000000000021, 'system.logo', '/logo.png', 'string', 'SYSTEM', 'System logo path', 1),
(2000000000000000022, 'user.registration.enabled', 'true', 'boolean', 'SYSTEM', 'Whether user registration is allowed', 1),
(2000000000000000023, 'user.default.role', 'VIEWER', 'string', 'SYSTEM', 'Default role for new users', 1);

-- 7.2 Seed dictionary type data
INSERT INTO `kb_dict` (`id`, `dict_code`, `dict_name`, `dict_type`, `description`, `sort`, `status`) VALUES
(3000000000000000001, 'document_status', 'Document status', 'DOCUMENT', 'Document status: draft/pending review/published/rejected', 1, 1),
(3000000000000000002, 'notification_type', 'Notification type', 'SYSTEM', 'System notification type', 2, 1),
(3000000000000000003, 'operation_type', 'Operation type', 'SYSTEM', 'System operation type', 3, 1),
(3000000000000000004, 'file_type', 'File type', 'FILE', 'Supported file types', 4, 1),
(3000000000000000005, 'user_type', 'User type', 'USER', 'User type classification', 5, 1);

-- 7.3 Seed dictionary data values
-- Document status dictionary data
INSERT INTO `kb_dict_data` (`id`, `dict_id`, `dict_code`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `is_default`, `status`) VALUES
(3100000000000000001, 3000000000000000001, 'document_status', 'Draft', '0', 1, 'badge-gray', 1, 1),
(3100000000000000002, 3000000000000000001, 'document_status', 'Pending review', '1', 2, 'badge-yellow', 0, 1),
(3100000000000000003, 3000000000000000001, 'document_status', 'Published', '2', 3, 'badge-green', 0, 1),
(3100000000000000004, 3000000000000000001, 'document_status', 'Rejected', '3', 4, 'badge-red', 0, 1);

-- Notification type dictionary data
INSERT INTO `kb_dict_data` (`id`, `dict_id`, `dict_code`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `is_default`, `status`) VALUES
(3200000000000000001, 3000000000000000002, 'notification_type', 'System notification', 'system', 1, 'badge-blue', 1, 1),
(3200000000000000002, 3000000000000000002, 'notification_type', 'Comment notification', 'comment', 2, 'badge-green', 0, 1),
(3200000000000000003, 3000000000000000002, 'notification_type', '@Mention', 'mention', 3, 'badge-orange', 0, 1),
(3200000000000000004, 3000000000000000002, 'notification_type', 'Review notification', 'review', 4, 'badge-purple', 0, 1),
(3200000000000000005, 3000000000000000002, 'notification_type', 'Like notification', 'like', 5, 'badge-pink', 0, 1);

-- Operation type dictionary data
INSERT INTO `kb_dict_data` (`id`, `dict_id`, `dict_code`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `is_default`, `status`) VALUES
(3300000000000000001, 3000000000000000003, 'operation_type', 'Login', 'LOGIN', 1, NULL, 0, 1),
(3300000000000000002, 3000000000000000003, 'operation_type', 'Logout', 'LOGOUT', 2, NULL, 0, 1),
(3300000000000000003, 3000000000000000003, 'operation_type', 'Create', 'CREATE', 3, NULL, 0, 1),
(3300000000000000004, 3000000000000000003, 'operation_type', 'Update', 'UPDATE', 4, NULL, 0, 1),
(3300000000000000005, 3000000000000000003, 'operation_type', 'Delete', 'DELETE', 5, NULL, 0, 1),
(3300000000000000006, 3000000000000000003, 'operation_type', 'Query', 'QUERY', 6, NULL, 0, 1),
(3300000000000000007, 3000000000000000003, 'operation_type', 'Export', 'EXPORT', 7, NULL, 0, 1),
(3300000000000000008, 3000000000000000003, 'operation_type', 'Import', 'IMPORT', 8, NULL, 0, 1);

-- File type dictionary data
INSERT INTO `kb_dict_data` (`id`, `dict_id`, `dict_code`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `is_default`, `status`) VALUES
(3400000000000000001, 3000000000000000004, 'file_type', 'PDF Document', 'pdf', 1, 'file-pdf', 1, 1),
(3400000000000000002, 3000000000000000004, 'file_type', 'Word Document', 'doc', 2, 'file-word', 0, 1),
(3400000000000000003, 3000000000000000004, 'file_type', 'Excel Spreadsheet', 'xls', 3, 'file-excel', 0, 1),
(3400000000000000004, 3000000000000000004, 'file_type', 'PPT Presentation', 'ppt', 4, 'file-ppt', 0, 1),
(3400000000000000005, 3000000000000000004, 'file_type', 'Image', 'image', 5, 'file-image', 0, 1),
(3400000000000000006, 3000000000000000004, 'file_type', 'Video', 'video', 6, 'file-video', 0, 1),
(3400000000000000007, 3000000000000000004, 'file_type', 'Text', 'txt', 7, 'file-text', 0, 1),
(3400000000000000008, 3000000000000000004, 'file_type', 'Markdown', 'md', 8, 'file-markdown', 0, 1);

-- User type dictionary data
INSERT INTO `kb_dict_data` (`id`, `dict_id`, `dict_code`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `is_default`, `status`) VALUES
(3500000000000000001, 3000000000000000005, 'user_type', 'Super Admin', 'SUPER_ADMIN', 1, 'user-admin', 0, 1),
(3500000000000000002, 3000000000000000005, 'user_type', 'Knowledge Admin', 'KNOWLEDGE_ADMIN', 2, 'user-manager', 0, 1),
(3500000000000000003, 3000000000000000005, 'user_type', 'Content Admin', 'CONTENT_ADMIN', 3, 'user-editor', 0, 1),
(3500000000000000004, 3000000000000000005, 'user_type', 'Team Leader', 'TEAM_LEADER', 4, 'user-leader', 0, 1),
(3500000000000000005, 3000000000000000005, 'user_type', 'Contributor', 'CONTRIBUTOR', 5, 'user-contributor', 0, 1),
(3500000000000000006, 3000000000000000005, 'user_type', 'Regular User', 'VIEWER', 6, 'user-viewer', 1, 1);

-- 7.4 Seed notification data
INSERT INTO `kb_notification` (`id`, `user_id`, `user_name`, `notification_type`, `title`, `content`, `link`, `is_read`) VALUES
(1500000000000000001, 1000000000000000002, 'editor', 'system', 'Welcome to the Enterprise Knowledge Base', 'Welcome to the Enterprise Knowledge Base system, start your knowledge management journey!', '/documents', 0),
(1500000000000000002, 1000000000000000004, 'developer', 'comment', 'Your document received a new comment', '"Spring Boot 3.x Quick Start Guide" received a new comment', '/documents/1000000000000000001', 0),
(1500000000000000003, 1000000000000000005, 'product', 'review', 'Document review approved', 'Your "Enterprise Knowledge Base Product Requirements Document (PRD)" has passed review', '/documents/1000000000000000005', 1),
(1500000000000000004, 1000000000000000001, 'admin', 'mention', 'Someone mentioned you', 'developer mentioned you in "Docker + Kubernetes Containerized Deployment"', '/documents/1000000000000000004', 0);

-- 7.5 Seed operation log data
INSERT INTO `kb_operation_log` (`id`, `module`, `operation_type`, `operation_desc`, `request_method`, `request_url`, `user_id`, `username`, `ip_address`, `execute_time`, `status`) VALUES
(4000000000000000001, 'User Management', 'LOGIN', 'User login', 'POST', '/api/auth/login', 1000000000000000001, 'admin', '127.0.0.1', 125, 1),
(4000000000000000002, 'Document Management', 'CREATE', 'Create document', 'POST', '/api/document', 1000000000000000002, 'editor', '127.0.0.1', 342, 1),
(4000000000000000003, 'Document Management', 'UPDATE', 'Update document', 'PUT', '/api/document/1000000000000000001', 1000000000000000002, 'editor', '127.0.0.1', 215, 1),
(4000000000000000004, 'System Configuration', 'UPDATE', 'Update system configuration', 'PUT', '/api/foundation/config', 1000000000000000001, 'admin', '127.0.0.1', 89, 1),
(4000000000000000005, 'User Management', 'CREATE', 'Create user', 'POST', '/api/auth/user', 1000000000000000001, 'admin', '127.0.0.1', 156, 1);

-- 7.6 Seed notification template data
INSERT INTO `kb_notification_template` (`id`, `template_code`, `template_name`, `notification_type`, `title`, `content`, `variables`, `description`, `is_active`) VALUES
(1, 'EMAIL_VERIFY_CODE', 'Email verification code', 'EMAIL', 'Verification Code - {{systemName}}', 'Dear {{userName}}, your verification code is: {{verifyCode}}, valid for 5 minutes.', '["userName","verifyCode","systemName"]', 'Used for email verification and password recovery scenarios', 1),
(2, 'DOCUMENT_APPROVED', 'Document review approved', 'SYSTEM', 'Your document "{{documentTitle}}" has passed review', 'Your submitted document "{{documentTitle}}" has passed review. Thank you for your contribution!', '["documentTitle"]', 'Notification sent when a document review is approved', 1),
(3, 'DOCUMENT_REJECTED', 'Document review rejected', 'SYSTEM', 'Your document "{{documentTitle}}" needs revision', 'Your submitted document "{{documentTitle}}" did not pass review. Reason: {{rejectReason}}. Please revise and resubmit.', '["documentTitle","rejectReason"]', 'Notification sent when a document review is rejected', 1),
(4, 'NEW_COMMENT', 'New comment notification', 'SYSTEM', 'Your document received a new comment', '{{commentUsername}} commented on your document "{{documentTitle}}": {{commentContent}}', '["commentUsername","documentTitle","commentContent"]', 'Notification sent when a document receives a new comment', 1),
(5, 'DOCUMENT_LIKED', 'Document liked', 'SYSTEM', 'Your document received a new like', '{{likeUsername}} liked your document "{{documentTitle}}"', '["likeUsername","documentTitle"]', 'Notification sent when a document is liked', 1),
(6, 'WELCOME_MESSAGE', 'Welcome message', 'SYSTEM', 'Welcome to {{systemName}}', 'Dear {{userName}}, welcome to {{systemName}}! We look forward to your contributions.', '["userName","systemName"]', 'Welcome message sent after a user registers', 1);


-- =====================================================
-- Completion message
-- =====================================================

SELECT '========================================' AS '';
SELECT '  Seed data initialization complete!' AS message;
SELECT '========================================' AS '';

-- Count the volume of data in each database
USE `kb_user`;
SELECT CONCAT('kb_user: users=', COUNT(*)) AS info FROM `kb_user` UNION ALL
SELECT CONCAT('        roles=', COUNT(*)) FROM `kb_role` UNION ALL
SELECT CONCAT('        permissions=', COUNT(*)) FROM `kb_permission`;

USE `kb_document`;
SELECT CONCAT('kb_document: categories=', COUNT(*)) AS info FROM `kb_category` UNION ALL
SELECT CONCAT('           tags=', COUNT(*)) FROM `kb_tag` UNION ALL
SELECT CONCAT('           documents=', COUNT(*)) FROM `kb_document` UNION ALL
SELECT CONCAT('           comments=', COUNT(*)) FROM `kb_comment`;

USE `kb_ai`;
SELECT CONCAT('kb_ai: conversations=', COUNT(*)) AS info FROM `kb_ai_conversation` UNION ALL
SELECT CONCAT('       messages=', COUNT(*)) FROM `kb_ai_message`;

USE `kb_foundation`;
SELECT CONCAT('kb_foundation: config items=', COUNT(*)) AS info FROM `kb_system_config` UNION ALL
SELECT CONCAT('              dictionary types=', COUNT(*)) FROM `kb_dict` UNION ALL
SELECT CONCAT('              dictionary data=', COUNT(*)) FROM `kb_dict_data` UNION ALL
SELECT CONCAT('              notification templates=', COUNT(*)) FROM `kb_notification_template`;
