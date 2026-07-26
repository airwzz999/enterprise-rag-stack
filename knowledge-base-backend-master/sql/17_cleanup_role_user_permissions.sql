-- =====================================================
-- Clean up excess permissions for the ROLE_USER role
-- Newly registered users should only be assigned the ROLE_USER role,
-- and ROLE_USER should not have file management or system management menu permissions.
-- =====================================================

SET NAMES utf8mb4;
USE `kb_user`;

-- Look up the ROLE_USER role ID
SET @role_user_id = 2000000000000000005;

-- File management permission codes (including sub-permissions)
-- 'file', 'file:list', 'file:upload', 'file:delete'
DELETE rp FROM `kb_role_permission` rp
INNER JOIN `kb_permission` p ON rp.`permission_id` = p.`id`
WHERE rp.`role_id` = @role_user_id
  AND p.`permission_code` IN ('file', 'file:list', 'file:upload', 'file:delete');

-- System management permission codes (including sub-permissions)
-- 'system', 'system:user', 'system:role', 'system:permission',
-- 'system:permission:create', 'system:permission:edit', 'system:permission:delete',
-- 'system:team', 'system:statistics', 'system:settings',
-- 'system:config', 'system:dictionary', 'system:operation-log', 'system:notification-template',
-- 'api:permission:list', 'api:permission:maintain'
DELETE rp FROM `kb_role_permission` rp
INNER JOIN `kb_permission` p ON rp.`permission_id` = p.`id`
WHERE rp.`role_id` = @role_user_id
  AND p.`permission_code` IN (
    'system', 'system:user', 'system:role', 'system:permission',
    'system:permission:create', 'system:permission:edit', 'system:permission:delete',
    'system:team', 'system:statistics', 'system:settings',
    'system:config', 'system:dictionary', 'system:operation-log', 'system:notification-template',
    'api:permission:list', 'api:permission:maintain'
  );

-- Document management permission codes
-- Keep ROLE_USER's core document operation permissions: document:list, document:create, document:edit, document:delete
-- (Knowledge members need to be able to view, create, edit, and delete documents)
-- Remove the following management-only permissions (admin/reviewer only):
-- 'document:review', 'document:category', 'document:category:query', 'document:tag', 'document:version'
DELETE rp FROM `kb_role_permission` rp
INNER JOIN `kb_permission` p ON rp.`permission_id` = p.`id`
WHERE rp.`role_id` = @role_user_id
  AND p.`permission_code` IN (
    'document:review',
    'document:category', 'document:category:query', 'document:tag', 'document:version'
  );

-- Output result
SELECT CONCAT('ROLE_USER permission cleanup complete!') AS message;
SELECT CONCAT('ROLE_USER current role-permission count: ', COUNT(*)) AS info
FROM `kb_role_permission`
WHERE `role_id` = @role_user_id;
