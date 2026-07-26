-- =====================================================
-- Enterprise Knowledge Base System - supplementary menu permission data
-- Adds missing top-level menus (AI Writing, File Management) and system management submenus
-- Uses NOT EXISTS to avoid duplicates; safe to re-run
-- =====================================================

SET NAMES utf8mb4;
USE `kb_user`;

-- =====================================================
-- 1. Adjust existing top-level menu sort order to make room for new menus
-- =====================================================

-- Knowledge Graph: 3 → 4
UPDATE `kb_permission` SET `sort` = 4 WHERE `permission_code` = 'graph' AND `deleted` = 0;

-- AI Assistant: 4 → 6
UPDATE `kb_permission` SET `sort` = 6 WHERE `permission_code` = 'ai' AND `deleted` = 0;

-- Notification Center: 6 → 8
UPDATE `kb_permission` SET `sort` = 8 WHERE `permission_code` = 'notification' AND `deleted` = 0;

-- Personal Center: 7 → 9
UPDATE `kb_permission` SET `sort` = 9 WHERE `permission_code` = 'profile' AND `deleted` = 0;

-- System Management: 8 → 10
UPDATE `kb_permission` SET `sort` = 10 WHERE `permission_code` = 'system' AND `deleted` = 0;

-- =====================================================
-- 2. Add new top-level menus
-- =====================================================

-- File Management (after Document Center, before Knowledge Graph)
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`)
SELECT 3000000000000000046, 0, 'File Management', 'file', 1, '/files', 'FolderOpenOutlined', 3, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `kb_permission` WHERE `permission_code` = 'file' AND `deleted` = 0
);

-- AI Writing (after AI Assistant)
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`)
SELECT 3000000000000000047, 0, 'AI Writing', 'ai-writing', 1, '/ai-writing', 'EditOutlined', 7, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `kb_permission` WHERE `permission_code` = 'ai-writing' AND `deleted` = 0
);

-- =====================================================
-- 3. File Management submenus
-- =====================================================

INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`)
SELECT 3000000000000000048, 3000000000000000046, 'File List', 'file:list', 1, '/files', NULL, 1, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `kb_permission` WHERE `permission_code` = 'file:list' AND `deleted` = 0
);

INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`)
SELECT 3000000000000000049, 3000000000000000046, 'Upload File', 'file:upload', 2, NULL, NULL, 2, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `kb_permission` WHERE `permission_code` = 'file:upload' AND `deleted` = 0
);

INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`)
SELECT 3000000000000000050, 3000000000000000046, 'Delete File', 'file:delete', 2, NULL, NULL, 3, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `kb_permission` WHERE `permission_code` = 'file:delete' AND `deleted` = 0
);

-- =====================================================
-- 4. System Management supplementary submenus
-- =====================================================

-- System Configuration
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`)
SELECT 3000000000000000051, 3000000000000000008, 'System Configuration', 'system:config', 1, '/admin/system-config', NULL, 8, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `kb_permission` WHERE `permission_code` = 'system:config' AND `deleted` = 0
);

-- Dictionary Management
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`)
SELECT 3000000000000000052, 3000000000000000008, 'Dictionary Management', 'system:dictionary', 1, '/admin/dictionary', NULL, 9, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `kb_permission` WHERE `permission_code` = 'system:dictionary' AND `deleted` = 0
);

-- Operation Logs
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`)
SELECT 3000000000000000053, 3000000000000000008, 'Operation Logs', 'system:operation-log', 1, '/admin/operation-logs', NULL, 10, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `kb_permission` WHERE `permission_code` = 'system:operation-log' AND `deleted` = 0
);

-- Notification Templates
INSERT INTO `kb_permission` (`id`, `parent_id`, `permission_name`, `permission_code`, `permission_type`, `menu_url`, `icon`, `sort`, `status`)
SELECT 3000000000000000054, 3000000000000000008, 'Notification Templates', 'system:notification-template', 1, '/admin/notification-templates', NULL, 11, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `kb_permission` WHERE `permission_code` = 'system:notification-template' AND `deleted` = 0
);

-- =====================================================
-- 5. Assign the new permissions to the super admin role
-- =====================================================

INSERT IGNORE INTO `kb_role_permission` (`id`, `role_id`, `permission_id`)
SELECT 5000000000000000050 + (@row_num := @row_num + 1), 2000000000000000001, p.`id`
FROM `kb_permission` p, (SELECT @row_num := 0) r
WHERE p.`permission_code` IN (
  'file', 'file:list', 'file:upload', 'file:delete',
  'ai-writing',
  'system:config', 'system:dictionary', 'system:operation-log', 'system:notification-template'
)
AND NOT EXISTS (
  SELECT 1 FROM `kb_role_permission` rp
  WHERE rp.`role_id` = 2000000000000000001 AND rp.`permission_id` = p.`id`
);

SELECT 'Menu permission supplementary data initialization complete!' AS message;
