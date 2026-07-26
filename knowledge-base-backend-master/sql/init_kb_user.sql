-- =====================================================
-- kb_user database - seed data
-- =====================================================

SET NAMES utf8mb4;
USE `kb_user`;

-- Seed user data
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

-- Seed role data
INSERT INTO `kb_role` (`id`, `role_name`, `role_code`, `description`, `sort`, `status`) VALUES
(2000000000000000001, 'Super Admin', 'ROLE_SUPER_ADMIN', 'Has all system permissions', 1, 1),
(2000000000000000002, 'Admin', 'ROLE_ADMIN', 'Has system administration permissions', 2, 1),
(2000000000000000003, 'Editor', 'ROLE_EDITOR', 'Can edit and manage documents', 3, 1),
(2000000000000000004, 'Reviewer', 'ROLE_REVIEWER', 'Can review documents', 4, 1),
(2000000000000000005, 'Regular User', 'ROLE_USER', 'Regular user permissions', 5, 1),
(2000000000000000006, 'Guest', 'ROLE_GUEST', 'Read-only guest access', 6, 1);

-- Seed permission data
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`) VALUES
(3000000000000000001, 0, 'Dashboard', 'dashboard', 1, '/dashboard', 'DashboardOutlined', 1, 1),
(3000000000000000002, 0, 'Document Center', 'document', 1, '/documents', 'FileTextOutlined', 2, 1),
(3000000000000000003, 0, 'Knowledge Graph', 'graph', 1, '/knowledge-graph', 'NodeIndexOutlined', 3, 1),
(3000000000000000004, 0, 'AI Assistant', 'ai', 1, '/ai', 'RobotOutlined', 4, 1),
(3000000000000000005, 0, 'Search', 'search', 1, '/search', 'SearchOutlined', 5, 1),
(3000000000000000006, 0, 'Notification Center', 'notification', 1, '/notifications', 'BellOutlined', 6, 1),
(3000000000000000007, 0, 'Personal Center', 'profile', 1, '/profile', 'UserOutlined', 7, 1),
(3000000000000000008, 0, 'System Management', 'system', 1, '/admin', 'SettingOutlined', 8, 1);

-- User-role associations
INSERT INTO `kb_user_role` (`id`, `user_id`, `role_id`, `create_by`) VALUES
(4000000000000000001, 1000000000000000001, 2000000000000000001, 1000000000000000001),
(4000000000000000011, 1000000000000000001, 2000000000000000002, 1000000000000000001),
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

-- Seed team data
INSERT INTO `kb_team` (`id`, `team_name`, `team_code`, `description`, `leader_id`, `parent_id`, `sort`) VALUES
(8000000000000000001, 'Technology Center', 'TECH_CENTER', 'Responsible for all company technical R&D work', 1000000000000000004, 0, 1),
(8000000000000000002, 'Product Center', 'PRODUCT_CENTER', 'Responsible for product planning and design', 1000000000000000005, 0, 2),
(8000000000000000003, 'Operations Center', 'OPS_CENTER', 'Responsible for business operations and marketing', 1000000000000000007, 0, 3),
(8000000000000000004, 'Administrative Center', 'ADMIN_CENTER', 'Responsible for company administrative, HR, and finance work', 1000000000000000008, 0, 4),
(8000000000000000005, 'Backend Development Team', 'BACKEND_TEAM', 'Backend system development', 1000000000000000004, 8000000000000000001, 1),
(8000000000000000006, 'Frontend Development Team', 'FRONTEND_TEAM', 'Frontend system development', 1000000000000000004, 8000000000000000001, 2),
(8000000000000000007, 'QA Team', 'QA_TEAM', 'Quality assurance and testing', 1000000000000000003, 8000000000000000001, 3);

-- Team members
INSERT INTO `kb_team_member` (`id`, `team_id`, `user_id`, `member_role`) VALUES
(9000000000000000001, 8000000000000000005, 1000000000000000004, 'leader'),
(9000000000000000002, 8000000000000000005, 1000000000000000001, 'member'),
(9000000000000000003, 8000000000000000006, 1000000000000000006, 'member'),
(9000000000000000004, 8000000000000000007, 1000000000000000003, 'leader');

SELECT 'kb_user seed data initialization complete!' AS message;
SELECT CONCAT('User count: ', COUNT(*)) AS info FROM `kb_user`;
SELECT CONCAT('Role count: ', COUNT(*)) AS info FROM `kb_role`;
SELECT CONCAT('Permission count: ', COUNT(*)) AS info FROM `kb_permission`;
