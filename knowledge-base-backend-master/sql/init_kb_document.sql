-- =====================================================
-- kb_document database - seed data
-- =====================================================

SET NAMES utf8mb4;
USE `kb_document`;

-- Seed document category data
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

-- Seed tag data
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

-- Seed document data
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
(1100000000000000009, 1000000000000000004, 7000000000000000005);

-- Seed comment data
INSERT INTO `kb_comment` (`id`, `document_id`, `content`, `user_id`, `user_name`, `parent_id`, `like_count`, `status`) VALUES
(1200000000000000001, 1000000000000000001, 'This article is very detailed and helped me a lot!', 1000000000000000002, 'editor', 0, 12, 1),
(1200000000000000002, 1000000000000000001, 'One suggestion: it would help to go into more detail on how auto-configuration works.', 1000000000000000004, 'developer', 0, 5, 1),
(1200000000000000003, 1000000000000000002, 'The TypeScript type definitions are very well structured, learned a lot!', 1000000000000000003, 'tester', 0, 8, 1),
(1200000000000000004, 1000000000000000002, 'Looking forward to the next article about Hooks.', 1000000000000000002, 'editor', 0, 3, 1),
(1200000000000000005, 1000000000000000003, 'The index optimization tips are very practical, I have already applied them in my project.', 1000000000000000005, 'product', 0, 15, 1),
(1200000000000000006, 1000000000000000005, 'The PRD is written very clearly, the product logic is complete.', 1000000000000000001, 'admin', 0, 6, 1),
(1200000000000000007, 1000000000000000008, 'The onboarding guide is very detailed, it helped me get up to speed with the company quickly.', 1000000000000000003, 'tester', 0, 23, 1),
(1200000000000000008, 1000000000000000008, 'Suggest adding some notes about remote work.', 1000000000000000007, 'sales', 0, 2, 1);

SELECT 'kb_document seed data initialization complete!' AS message;
SELECT CONCAT('Category count: ', COUNT(*)) AS info FROM `kb_category`;
SELECT CONCAT('Tag count: ', COUNT(*)) AS info FROM `kb_tag`;
SELECT CONCAT('Document count: ', COUNT(*)) AS info FROM `kb_document`;
SELECT CONCAT('Comment count: ', COUNT(*)) AS info FROM `kb_comment`;
