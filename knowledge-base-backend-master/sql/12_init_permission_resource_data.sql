-- Purpose:
-- Supplement the permission management page with "menu sub-resource" seed data.
-- This script uses NOT EXISTS to avoid duplicates; re-running it will not insert the same permission_code twice.

-- Document Center sub-resources
INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000011, 3000000000000000002, 'Document List', 'document:list', 1, '/documents', NULL, NULL, NULL, 1, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'document:list' AND deleted = 0
);

INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000012, 3000000000000000002, 'Create Document', 'document:create', 2, NULL, NULL, NULL, NULL, 2, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'document:create' AND deleted = 0
);

INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000013, 3000000000000000002, 'Edit Document', 'document:edit', 2, NULL, NULL, NULL, NULL, 3, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'document:edit' AND deleted = 0
);

INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000014, 3000000000000000002, 'Delete Document', 'document:delete', 2, NULL, NULL, NULL, NULL, 4, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'document:delete' AND deleted = 0
);

INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000015, 3000000000000000002, 'Document Review', 'document:review', 2, NULL, NULL, NULL, NULL, 5, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'document:review' AND deleted = 0
);

INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000016, 3000000000000000002, 'Document Category', 'document:category', 1, '/admin/categories', NULL, NULL, NULL, 6, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'document:category' AND deleted = 0
);

INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000018, 3000000000000000016, 'Query Categories', 'document:category:query', 3, NULL, '/api/document/categories/**', 'GET', NULL, 1, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'document:category:query' AND deleted = 0
);

INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000017, 3000000000000000002, 'Document Tags', 'document:tag', 1, '/admin/tags', NULL, NULL, NULL, 7, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'document:tag' AND deleted = 0
);

-- System Management submenus
INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000021, 3000000000000000008, 'User Management', 'system:user', 1, '/admin/users', NULL, NULL, NULL, 1, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'system:user' AND deleted = 0
);

INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000022, 3000000000000000008, 'Role Management', 'system:role', 1, '/admin/roles', NULL, NULL, NULL, 2, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'system:role' AND deleted = 0
);

INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000023, 3000000000000000008, 'Permission Management', 'system:permission', 1, '/admin/permissions', NULL, NULL, NULL, 3, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'system:permission' AND deleted = 0
);

-- Actual permission points under the Permission Management menu
INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000041, 3000000000000000023, 'Create Permission', 'system:permission:create', 2, NULL, NULL, NULL, NULL, 1, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'system:permission:create' AND deleted = 0
);

INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000042, 3000000000000000023, 'Edit Permission', 'system:permission:edit', 2, NULL, NULL, NULL, NULL, 2, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'system:permission:edit' AND deleted = 0
);

INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000043, 3000000000000000023, 'Delete Permission', 'system:permission:delete', 2, NULL, NULL, NULL, NULL, 3, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'system:permission:delete' AND deleted = 0
);

INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000044, 3000000000000000023, 'Query Permission List API', 'api:permission:list', 3, NULL, '/api/auth/permissions/**', 'GET', NULL, 4, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'api:permission:list' AND deleted = 0
);

INSERT INTO kb_permission (
  id, parent_id, permission_name, permission_code, permission_type, menu_url, api_url, method, icon, sort, status
)
SELECT 3000000000000000045, 3000000000000000023, 'Maintain Permission API', 'api:permission:maintain', 3, NULL, '/api/auth/permissions/**', 'POST', NULL, 5, 1
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM kb_permission WHERE permission_code = 'api:permission:maintain' AND deleted = 0
);
