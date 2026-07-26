-- =====================================================
-- kb_user database - fix role-permission associations
-- =====================================================

SET NAMES utf8mb4;
USE `kb_user`;

-- Clear and recreate role-permission associations
DELETE FROM `kb_role_permission` WHERE role_id = 2000000000000000001;

-- Super admin has all permissions
INSERT INTO `kb_role_permission` (`id`, `role_id`, `permission_id`)
SELECT
    5000000000000000000 + ROW_NUMBER() OVER (ORDER BY `id`),
    2000000000000000001,
    `id`
FROM `kb_permission`;

SELECT 'Role-permission association fix complete!' AS message;
SELECT CONCAT('Role-permission association count: ', COUNT(*)) AS info FROM `kb_role_permission`;
