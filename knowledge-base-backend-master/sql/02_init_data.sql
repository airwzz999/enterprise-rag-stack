-- =====================================================
-- Enterprise Knowledge Base System - seed data script
-- This script contains enterprise-level seed data
-- =====================================================

SET NAMES utf8mb4;
USE `knowledge_base`;

-- =====================================================
-- 1. Seed user data
-- =====================================================

-- Default admin accounts (password: admin123, BCrypt encrypted value)
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

-- =====================================================
-- 2. Seed role and permission data
-- =====================================================

-- Role data
INSERT INTO `kb_role` (`id`, `role_name`, `role_code`, `description`, `sort`, `status`) VALUES
(2000000000000000001, 'Super Admin', 'ROLE_SUPER_ADMIN', 'Has all system permissions', 1, 1),
(2000000000000000002, 'Admin', 'ROLE_ADMIN', 'Has system administration permissions', 2, 1),
(2000000000000000003, 'Editor', 'ROLE_EDITOR', 'Can edit and manage documents', 3, 1),
(2000000000000000004, 'Reviewer', 'ROLE_REVIEWER', 'Can review documents', 4, 1),
(2000000000000000005, 'Regular User', 'ROLE_USER', 'Regular user permissions', 5, 1),
(2000000000000000006, 'Guest', 'ROLE_GUEST', 'Read-only guest access', 6, 1);

-- Permission data (menu permissions)
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`) VALUES
-- Top-level menus
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

-- Submenus (Document Management)
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`) VALUES
(3000000000000000011, 3000000000000000002, 'Document List', 'document:list', 1, '/documents', NULL, 1, 1),
(3000000000000000012, 3000000000000000002, 'Create Document', 'document:create', 2, NULL, NULL, 2, 1),
(3000000000000000013, 3000000000000000002, 'Edit Document', 'document:edit', 2, NULL, NULL, 3, 1),
(3000000000000000014, 3000000000000000002, 'Delete Document', 'document:delete', 2, NULL, NULL, 4, 1),
(3000000000000000015, 3000000000000000002, 'Document Review', 'document:review', 2, NULL, NULL, 5, 1),
(3000000000000000016, 3000000000000000002, 'Document Category', 'document:category', 1, '/admin/categories', NULL, 6, 1),
(3000000000000000017, 3000000000000000002, 'Document Tags', 'document:tag', 1, '/admin/tags', NULL, 7, 1),
(3000000000000000018, 3000000000000000002, 'Version Management', 'document:version', 2, NULL, NULL, 8, 1);

-- Submenus (File Management)
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`) VALUES
(3000000000000000048, 3000000000000000046, 'File List', 'file:list', 1, '/files', NULL, 1, 1),
(3000000000000000049, 3000000000000000046, 'Upload File', 'file:upload', 2, NULL, NULL, 2, 1),
(3000000000000000050, 3000000000000000046, 'Delete File', 'file:delete', 2, NULL, NULL, 3, 1);

-- Submenus (System Management)
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

-- API permissions
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `api_url`, `method`, `sort`, `status`) VALUES
(3000000000000000031, 0, 'Document Query API', 'api:document:query', 3, '/api/document/**', 'GET', 1, 1),
(3000000000000000032, 0, 'Document Create API', 'api:document:create', 3, '/api/document', 'POST', 2, 1),
(3000000000000000033, 0, 'Document Update API', 'api:document:update', 3, '/api/document/**', 'PUT', 3, 1),
(3000000000000000034, 0, 'Document Delete API', 'api:document:delete', 3, '/api/document/**', 'DELETE', 4, 1),
(3000000000000000035, 0, 'User Management API', 'api:user:manage', 3, '/api/user/**', '*', 5, 1),
(3000000000000000036, 0, 'Role Management API', 'api:role:manage', 3, '/api/role/**', '*', 6, 1);

-- User-role associations
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

-- Super admin has all permissions
INSERT INTO `kb_role_permission` (`id`, `role_id`, `permission_id`)
SELECT 5000000000000000000 + (@row:=@row+1), 2000000000000000001, `id`
FROM `kb_permission`, (SELECT @row:=0) r;

-- =====================================================
-- 3. Seed document category data
-- =====================================================

INSERT INTO `kb_category` (`id`, `category_name`, `parent_id`, `category_icon`, `description`, `sort`, `document_count`) VALUES
-- Top-level categories
(6000000000000000001, 'Technical Documentation', 0, 'tech', 'Documentation related to technical development', 1, 0),
(6000000000000000002, 'Product Documentation', 0, 'product', 'Product design and requirements documents', 2, 0),
(6000000000000000003, 'Business Processes', 0, 'business', 'Company business process guidelines', 3, 0),
(6000000000000000004, 'Human Resources', 0, 'hr', 'HR policies and management guidelines', 4, 0),
(6000000000000000005, 'Financial Policies', 0, 'finance', 'Financial management policies and processes', 5, 0),
(6000000000000000006, 'Marketing', 0, 'marketing', 'Marketing strategies and plans', 6, 0),
(6000000000000000007, 'Legal & Compliance', 0, 'legal', 'Laws, regulations, and compliance requirements', 7, 0),
(6000000000000000008, 'Training Materials', 0, 'training', 'Employee training and learning materials', 8, 0);

-- Technical Documentation subcategories
INSERT INTO `kb_category` (`id`, `category_name`, `parent_id`, `category_icon`, `description`, `sort`, `document_count`) VALUES
(6000000000000000011, 'Backend Development', 6000000000000000001, 'backend', 'Backend technology stack development documentation', 1, 0),
(6000000000000000012, 'Frontend Development', 6000000000000000001, 'frontend', 'Frontend technology stack development documentation', 2, 0),
(6000000000000000013, 'Database', 6000000000000000001, 'database', 'Database design and optimization', 3, 0),
(6000000000000000014, 'DevOps', 6000000000000000001, 'devops', 'Operations, deployment, and CI/CD', 4, 0),
(6000000000000000015, 'Architecture Design', 6000000000000000001, 'architecture', 'System architecture design documentation', 5, 0);

-- Product Documentation subcategories
INSERT INTO `kb_category` (`id`, `category_name`, `parent_id`, `category_icon`, `description`, `sort`, `document_count`) VALUES
(6000000000000000021, 'Product Requirements', 6000000000000000002, 'requirement', 'Product Requirements Document (PRD)', 1, 0),
(6000000000000000022, 'UI Design', 6000000000000000002, 'design', 'UI/UX design guidelines', 2, 0),
(6000000000000000023, 'Product Planning', 6000000000000000002, 'planning', 'Product planning and roadmap', 3, 0),
(6000000000000000024, 'Competitive Analysis', 6000000000000000002, 'competitive', 'Competitive analysis reports', 4, 0);

-- =====================================================
-- 4. Seed tag data
-- =====================================================

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

-- =====================================================
-- 5. Seed team data
-- =====================================================

INSERT INTO `kb_team` (`id`, `team_name`, `team_code`, `description`, `icon`, `leader_id`, `parent_id`, `sort`, `status`) VALUES
(8000000000000000001, 'Technology Center', 'TECH_CENTER', 'Responsible for all company technical R&D work', 'tech', 1000000000000000004, 0, 1, 1),
(8000000000000000002, 'Product Center', 'PRODUCT_CENTER', 'Responsible for product planning and design', 'product', 1000000000000000005, 0, 2, 1),
(8000000000000000003, 'Operations Center', 'OPS_CENTER', 'Responsible for business operations and marketing', 'ops', 1000000000000000007, 0, 3, 1),
(8000000000000000004, 'Administrative Center', 'ADMIN_CENTER', 'Responsible for company administrative, HR, and finance work', 'admin', 1000000000000000008, 0, 4, 1),
(8000000000000000005, 'Backend Development Team', 'BACKEND_TEAM', 'Backend system development', 'backend', 1000000000000000004, 8000000000000000001, 1, 1),
(8000000000000000006, 'Frontend Development Team', 'FRONTEND_TEAM', 'Frontend system development', 'frontend', 1000000000000000004, 8000000000000000001, 2, 1),
(8000000000000000007, 'QA Team', 'QA_TEAM', 'Quality assurance and testing', 'qa', 1000000000000000003, 8000000000000000001, 3, 1);

-- Team members
INSERT INTO `kb_team_member` (`id`, `team_id`, `user_id`, `member_role`, `join_time`) VALUES
(9000000000000000001, 8000000000000000005, 1000000000000000004, 'leader', NOW()),
(9000000000000000002, 8000000000000000005, 1000000000000000001, 'member', NOW()),
(9000000000000000003, 8000000000000000006, 1000000000000000004, 'member', NOW()),
(9000000000000000004, 8000000000000000006, 1000000000000000006, 'member', NOW()),
(9000000000000000005, 8000000000000000007, 1000000000000000003, 'leader', NOW());

-- =====================================================
-- 6. Seed document data
-- =====================================================

INSERT INTO `kb_document` (`id`, `title`, `content`, `summary`, `category_id`, `author_id`, `status`, `is_public`, `view_count`, `like_count`, `comment_count`, `version`, `publish_time`) VALUES
-- Technical documentation
(1000000000000000001,
'Spring Boot 3.x Quick Start Guide',
'# Spring Boot 3.x Quick Start Guide\n\n## Project Initialization\n\nCreate a project using Spring Initializr:\n\n```xml\n<dependency>\n    <groupId>org.springframework.boot</groupId>\n    <artifactId>spring-boot-starter-web</artifactId>\n</dependency>\n```\n\n## Core Features\n\n### 1. Auto-configuration\nAuto-configuration in Spring Boot greatly simplifies development work...\n\n### 2. Starter Dependencies\nProvides a series of starter dependencies...\n\n### 3. Command-Line Interface\nComes with a built-in spring command-line tool...\n\n## Best Practices\n\n- Follow convention over configuration\n- Use a well-layered architecture\n- Use a configuration center',
'A complete Spring Boot 3.x beginner tutorial, covering project initialization, core feature introductions, and best practices.',
6000000000000000011, 1000000000000000004, 'published', 1, 1523, 89, 23, 1, '2024-01-15 10:00:00'),

(1000000000000000002,
'React 18 + TypeScript Best Practices',
'# React 18 + TypeScript Best Practices\n\n## Project Structure\n\n```\nsrc/\n├── components/     # Shared components\n├── pages/          # Page components\n├── hooks/          # Custom hooks\n├── services/       # API services\n├── stores/         # State management\n├── types/          # Type definitions\n└── utils/          # Utility functions\n```\n\n## Core Concepts\n\n### Function Components\n```typescript\ninterface Props {\n  title: string;\n  count: number;\n}\n\nexport const MyComponent: React.FC<Props> = ({ title, count }) => {\n  return <div>{title}: {count}</div>;\n};\n```\n\n## State Management\nUses Zustand for state management...',
'Frontend development best practices based on React 18 and TypeScript, covering project structure, core concepts, and state management.',
6000000000000000012, 1000000000000000006, 'published', 1, 2187, 156, 45, 1, '2024-02-10 14:30:00'),

(1000000000000000003,
'MySQL 8.0 Performance Optimization Guide',
'# MySQL 8.0 Performance Optimization Guide\n\n## Index Optimization\n\n### Index Design Principles\n1. Prioritize creating indexes on highly selective columns\n2. Composite indexes should follow the leftmost prefix principle\n3. Avoid redundant indexes\n\n### Index Creation Examples\n```sql\n-- Create a composite index\nCREATE INDEX idx_user_email ON user(username, email);\n\n-- Create a full-text index\nCREATE FULLTEXT INDEX idx_content ON article(content);\n```\n\n## Query Optimization\n\n### EXPLAIN Analysis\nUse EXPLAIN to analyze the query execution plan...\n\n### Slow Query Log\nConfigure the slow query log to pinpoint performance bottlenecks...',
'A complete guide to MySQL 8.0 database performance optimization, covering index optimization, query optimization, and slow query analysis.',
6000000000000000013, 1000000000000000001, 'published', 1, 3421, 234, 67, 1, '2024-01-28 09:15:00'),

(1000000000000000004,
'Docker + Kubernetes Containerized Deployment',
'# Docker + Kubernetes Containerized Deployment\n\n## Docker Basics\n\n### Writing a Dockerfile\n```dockerfile\nFROM openjdk:21-jdk-slim\nWORKDIR /app\nCOPY target/*.jar app.jar\nEXPOSE 8080\nENTRYPOINT ["java", "-jar", "app.jar"]\n```\n\n## Kubernetes Deployment\n\n### Deployment Configuration\n```yaml\napiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: knowledge-base\nspec:\n  replicas: 3\n  selector:\n    matchLabels:\n      app: knowledge-base\n  template:\n    metadata:\n      labels:\n        app: knowledge-base\n    spec:\n      containers:\n      - name: app\n        image: kb-app:latest\n        ports:\n        - containerPort: 8080\n```',
'A containerized microservice deployment practice based on Docker and Kubernetes.',
6000000000000000014, 1000000000000000004, 'published', 1, 1876, 98, 19, 1, '2024-03-05 16:20:00'),

-- Product documentation
(1000000000000000005,
'Enterprise Knowledge Base Product Requirements Document (PRD)',
'# Enterprise Knowledge Base Product Requirements Document\n\n## 1. Product Overview\n\n### 1.1 Product Positioning\nAn enterprise-grade knowledge management platform that helps companies build up knowledge assets and improve collaboration efficiency.\n\n### 1.2 Target Users\n- Employees: need to quickly find and access knowledge\n- Administrators: need to manage and maintain the knowledge base\n- Decision-makers: need knowledge data analytics\n\n## 2. Functional Requirements\n\n### 2.1 Core Features\n1. Document management\n2. Knowledge search\n3. Collaborative editing\n4. Access control\n5. Data statistics\n\n### 2.2 User Experience\n- A clean, intuitive interface\n- Fast, responsive search\n- A convenient editing experience',
'A complete Enterprise Knowledge Base product requirements document, covering product positioning, target users, and functional requirements.',
6000000000000000021, 1000000000000000005, 'published', 1, 987, 45, 12, 1, '2024-02-01 10:00:00'),

(1000000000000000006,
'UI Design Guidelines V2.0',
'# UI Design Guidelines V2.0\n\n## Color System\n\n### Primary Colors\n- Primary: #1890ff (blue)\n- Success: #52c41a (green)\n- Warning: #faad14 (orange)\n- Error: #ff4d4f (red)\n\n### Neutral Colors\n- Heading: #262626\n- Body text: #595959\n- Secondary: #8c8c8c\n- Disabled: #bfbfbf\n\n## Typography\n\n### Font Sizes\n- Large heading: 24px\n- Medium heading: 18px\n- Body text: 14px\n- Secondary text: 12px\n\n## Component Guidelines\n\n### Buttons\n- Primary button: blue background\n- Secondary button: white background\n- Text button: no border',
'Enterprise Knowledge Base UI design guidelines, covering the color system, typography, and component guidelines.',
6000000000000000022, 1000000000000000006, 'published', 1, 654, 34, 8, 1, '2024-02-15 14:00:00'),

-- Business processes
(1000000000000000007,
'Document Review Process Guidelines',
'# Document Review Process Guidelines\n\n## 1. Process Overview\n\nDocuments must go through a review process before publishing to ensure content quality.\n\n## 2. Review Process\n\n### 2.1 Submit for Review\nAfter completing a document, the author clicks the "Submit for Review" button.\n\n### 2.2 Reviewer Actions\n- Check content accuracy\n- Check formatting compliance\n- Provide review comments\n\n### 2.3 Review Outcome\n- **Approved**: the document is published automatically\n- **Rejected**: the author revises based on the feedback and resubmits\n\n## 3. Review Standards\n\n### Content Quality\n- Accurate and complete information\n- Clear, well-organized logic\n- Consistent, standardized formatting\n\n### Technical Documentation\n- Code must be runnable\n- Complete configuration instructions\n- Clear notes and caveats',
'Detailed guidelines for the document review process, including process steps and review standards.',
6000000000000000003, 1000000000000000002, 'published', 1, 1234, 67, 15, 1, '2024-01-20 11:00:00'),

-- Human resources
(1000000000000000008,
'Employee Onboarding Guide',
'# Employee Onboarding Guide\n\n## Welcome Aboard!\n\nFirst of all, welcome to our team! Here are some things to know as you get started.\n\n## Onboarding Process\n\n### Day One\n1. Complete HR paperwork\n2. Office environment orientation\n3. Account and access provisioning\n4. Meet your team members\n\n### Week One\n1. Get familiar with business processes\n2. Attend new hire training\n3. Complete basic tasks\n4. One-on-one mentoring with a buddy\n\n## Commonly Used Systems\n\n- Knowledge base system: https://kb.company.com\n- OA office system: https://oa.company.com\n- Email system: mail.company.com\n\n## Benefits\n\n### Social Insurance and Housing Fund\nContributed according to national regulations.\n\n### Paid Annual Leave\n- After 1 year of service: 5 days\n- After 3 years of service: 10 days\n- After 5 years of service: 15 days',
'A new employee onboarding guide, covering the onboarding process, commonly used systems, and benefits information.',
6000000000000000004, 1000000000000000008, 'published', 1, 5678, 234, 56, 1, '2024-01-01 09:00:00'),

-- Financial policies
(1000000000000000009,
'Expense Reimbursement Process Guide',
'# Expense Reimbursement Process Guide\n\n## Reimbursement Principles\n\n1. **Authentic and legal**: receipts must be genuine and valid\n2. **Pre-approval required**: large expenses require prior approval\n3. **Timely reimbursement**: submit within 1 month of the expense being incurred\n\n## Reimbursement Process\n\n### Step 1: Organize Receipts\n- Invoices must be VAT invoices issued to the company\n- Receipt date, amount, and item must be clear\n\n### Step 2: Fill Out the Reimbursement Form\nFill out the reimbursement form in the OA system and upload photos of the receipts.\n\n### Step 3: Approval Process\n- Department manager approval\n- Finance review\n- General manager approval (for amounts > 5000 CNY)\n\n### Step 4: Payment\nOnce approved, payment is made to the payroll card within 3-5 business days.\n\n## Notes\n\n- All receipts should be organized in chronological order\n- Transportation expenses must note the departure and destination\n- Entertainment expenses must note the reason and attendees',
'Detailed explanation of the company expense reimbursement process, covering reimbursement principles, process steps, and notes.',
6000000000000000005, 1000000000000000009, 'published', 1, 3456, 123, 34, 1, '2024-01-10 14:00:00');

-- Document-tag associations
INSERT INTO `kb_document_tag` (`id`, `document_id`, `tag_id`) VALUES
(1100000000000000001, 1000000000000000001, 7000000000000000006),
(1100000000000000002, 1000000000000000001, 7000000000000000005),
(1100000000000000003, 1000000000000000001, 7000000000000000011),
(1100000000000000004, 1000000000000000002, 7000000000000000007),
(1100000000000000005, 1000000000000000002, 7000000000000000012),
(1100000000000000006, 1000000000000000003, 7000000000000000008),
(1100000000000000007, 1000000000000000003, 7000000000000000009),
(1100000000000000008, 1000000000000000004, 7000000000000000010),
(1100000000000000009, 1000000000000000004, 7000000000000000014),
(1100000000000000010, 1000000000000000005, 7000000000000000002);

-- =====================================================
-- 7. Seed comment data
-- =====================================================

INSERT INTO `kb_comment` (`id`, `document_id`, `content`, `user_id`, `parent_id`, `like_count`, `status`) VALUES
(1200000000000000001, 1000000000000000001, 'This article is very detailed and helped me a lot!', 1000000000000000002, 0, 12, 1),
(1200000000000000002, 1000000000000000001, 'One suggestion: it would help to go into more detail on how auto-configuration works.', 1000000000000000004, 0, 5, 1),
(1200000000000000003, 1000000000000000002, 'The TypeScript type definitions are very well structured, learned a lot!', 1000000000000000003, 0, 8, 1),
(1200000000000000004, 1000000000000000002, 'Looking forward to the next article about Hooks.', 1000000000000000002, 0, 3, 1),
(1200000000000000005, 1000000000000000003, 'The index optimization tips are very practical, I have already applied them in my project.', 1000000000000000005, 0, 15, 1),
(1200000000000000006, 1000000000000000005, 'The PRD is written very clearly, the product logic is complete.', 1000000000000000001, 0, 6, 1),
(1200000000000000007, 1000000000000000008, 'The onboarding guide is very detailed, it helped me get up to speed with the company quickly.', 1000000000000000003, 0, 23, 1),
(1200000000000000008, 1000000000000000008, 'Suggest adding some notes about remote work.', 1000000000000000007, 0, 2, 1);

-- =====================================================
-- 8. Seed system configuration data
-- =====================================================

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

-- =====================================================
-- 9. Seed dictionary data
-- =====================================================

-- Document status dictionary
INSERT INTO `kb_dict` (`id`, `dict_code`, `dict_name`, `dict_type`, `description`, `sort`) VALUES
(1400000000000000001, 'document_status', 'Document status', 'document', 'Document status enum', 1);

INSERT INTO `kb_dict_data` (`id`, `dict_id`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `status`) VALUES
(1400000000000000001, 1400000000000000001, 'Draft', 'draft', 1, 'default', 1),
(1400000000000000002, 1400000000000000001, 'Published', 'published', 2, 'success', 1),
(1400000000000000003, 1400000000000000001, 'Archived', 'archived', 3, 'info', 1);

-- Review status dictionary
INSERT INTO `kb_dict` (`id`, `dict_code`, `dict_name`, `dict_type`, `description`, `sort`) VALUES
(1400000000000000002, 'review_status', 'Review status', 'review', 'Review status enum', 2);

INSERT INTO `kb_dict_data` (`id`, `dict_id`, `dict_label`, `dict_value`, `dict_sort`, `css_class`, `status`) VALUES
(1400000000000000004, 1400000000000000002, 'Pending review', 'pending', 1, 'warning', 1),
(1400000000000000005, 1400000000000000002, 'Approved', 'approved', 2, 'success', 1),
(1400000000000000006, 1400000000000000002, 'Rejected', 'rejected', 3, 'error', 1);

-- =====================================================
-- 10. Seed notification data
-- =====================================================

INSERT INTO `kb_notification` (`id`, `user_id`, `notification_type`, `title`, `content`, `link`, `is_read`) VALUES
(1500000000000000001, 1000000000000000002, 'system', 'Welcome to the Enterprise Knowledge Base', 'Welcome to the Enterprise Knowledge Base system, start your knowledge management journey!', '/documents', 0),
(1500000000000000002, 1000000000000000004, 'comment', 'Your document received a new comment', '"Spring Boot 3.x Quick Start Guide" received a new comment', '/documents/1000000000000000001', 0),
(1500000000000000003, 1000000000000000005, 'review', 'Document review approved', 'Your "Enterprise Knowledge Base Product Requirements Document (PRD)" has passed review', '/documents/1000000000000000005', 1),
(1500000000000000004, 1000000000000000001, 'mention', 'Someone mentioned you', 'developer mentioned you in "Docker + Kubernetes Containerized Deployment"', '/documents/1000000000000000004', 0);

-- =====================================================
-- 11. Seed AI conversation data
-- =====================================================

INSERT INTO `kb_ai_conversation` (`id`, `user_id`, `title`, `model_name`) VALUES
(1600000000000000001, 1000000000000000001, 'Discussion about Spring Boot', 'qwen-turbo'),
(1600000000000000002, 1000000000000000002, 'Frontend development questions', 'qwen-turbo'),
(1600000000000000003, 1000000000000000004, 'Database optimization suggestions', 'qwen-turbo');

INSERT INTO `kb_ai_message` (`id`, `conversation_id`, `role`, `content`, `tokens`, `created_at`) VALUES
(1700000000000000001, 1600000000000000001, 'user', 'What is the principle behind Spring Boot auto-configuration?', 20, NOW()),
(1700000000000000002, 1600000000000000001, 'assistant', 'Spring Boot auto-configuration is implemented via conditional annotations (@ConditionalOnClass, @ConditionalOnMissingBean, etc.). It decides whether to load a given configuration based on the jars present on the classpath and the beans already defined...', 150, NOW()),
(1700000000000000003, 1600000000000000001, 'user', 'Could you explain that in more detail?', 10, NOW()),
(1700000000000000004, 1600000000000000002, 'user', 'What are the new features in React 18?', 18, NOW()),
(1700000000000000005, 1600000000000000002, 'assistant', 'The main new features in React 18 include: 1. Concurrent rendering 2. Automatic batching 3. Transitions 4. Suspense improvements...', 120, NOW());

-- =====================================================
-- 12. Seed statistics data
-- =====================================================

-- Document statistics
INSERT INTO `kb_document_statistics` (`id`, `document_id`, `view_count`, `like_count`, `comment_count`, `collect_count`, `share_count`, `stat_date`) VALUES
(1800000000000000001, 1000000000000000001, 1523, 89, 23, 45, 12, CURDATE()),
(1800000000000000002, 1000000000000000002, 2187, 156, 45, 67, 23, CURDATE()),
(1800000000000000003, 1000000000000000003, 3421, 234, 67, 89, 34, CURDATE());

-- User statistics
INSERT INTO `kb_user_statistics` (`id`, `user_id`, `document_count`, `comment_count`, `like_count`, `view_count`, `login_count`, `stat_date`) VALUES
(1900000000000000001, 1000000000000000001, 3, 15, 45, 2345, 67, CURDATE()),
(1900000000000000002, 1000000000000000004, 2, 23, 89, 4523, 89, CURDATE()),
(1900000000000000003, 1000000000000000002, 1, 12, 34, 1234, 45, CURDATE());

-- =====================================================
-- Completion message
-- =====================================================

SELECT 'Seed data initialization complete!' AS message;
SELECT CONCAT('Users initialized: ', COUNT(*)) AS info FROM `kb_user`;
SELECT CONCAT('Roles initialized: ', COUNT(*)) AS info FROM `kb_role`;
SELECT CONCAT('Permissions initialized: ', COUNT(*)) AS info FROM `kb_permission`;
SELECT CONCAT('Categories initialized: ', COUNT(*)) AS info FROM `kb_category`;
SELECT CONCAT('Documents initialized: ', COUNT(*)) AS info FROM `kb_document`;
