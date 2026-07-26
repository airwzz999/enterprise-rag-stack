import type { User } from '@/types';

/**
 * Frontend permission code constants.
 *
 * <p>Centralizes the permission codes used by page routes, menus, and buttons,
 * avoiding scattered hardcoded values.</p>
 */
export const PERMISSIONS = {
  documentList: 'document:list',
  documentCreate: 'document:create',
  documentEdit: 'document:edit',
  documentDelete: 'document:delete',
  documentReview: 'document:review',
  documentCategory: 'document:category',
  documentCategoryQuery: 'document:category:query',
  documentTag: 'document:tag',
  documentVersion: 'document:version',
  systemUser: 'system:user',
  systemRole: 'system:role',
  systemPermission: 'system:permission',
  systemPermissionCreate: 'system:permission:create',
  systemPermissionEdit: 'system:permission:edit',
  systemPermissionDelete: 'system:permission:delete',
  systemTeam: 'system:team',
  systemStatistics: 'system:statistics',
  systemSettings: 'system:settings',
} as const;

const SUPER_ADMIN_ROLES = new Set(['ROLE_SUPER_ADMIN', 'SUPER_ADMIN']);

export const ADMIN_PERMISSION_CODES = [
  PERMISSIONS.systemUser,
  PERMISSIONS.systemRole,
  PERMISSIONS.systemPermission,
  PERMISSIONS.systemTeam,
  PERMISSIONS.systemStatistics,
  PERMISSIONS.systemSettings,
  PERMISSIONS.documentCategory,
  PERMISSIONS.documentCategoryQuery,
  PERMISSIONS.documentReview,
];

/**
 * Gets the set of the user's role codes.
 *
 * @param user The current logged-in user
 * @returns An array of role codes
 */
export function getUserRoles(user?: User | null): string[] {
  if (!user) {
    return [];
  }
  const roles = user.roles && user.roles.length > 0
    ? user.roles
    : user.role
      ? [user.role]
      : [];
  return Array.from(new Set(roles.filter(Boolean)));
}

/**
 * Gets the set of the user's permission codes.
 *
 * @param user The current logged-in user
 * @returns An array of permission codes
 */
export function getUserPermissions(user?: User | null): string[] {
  if (!user?.permissions?.length) {
    return [];
  }
  return Array.from(new Set(user.permissions.filter(Boolean)));
}

/**
 * Determines whether the user is a super admin.
 *
 * @param user The current logged-in user
 * @returns Whether the user is a super admin
 */
export function isSuperAdmin(user?: User | null): boolean {
  return getUserRoles(user).some((role) => SUPER_ADMIN_ROLES.has(role));
}

/**
 * Determines whether the user has a specific permission.
 *
 * @param user The current logged-in user
 * @param permission Permission code
 * @returns Whether the user has the permission
 */
export function hasPermission(user: User | null | undefined, permission?: string | null): boolean {
  if (!permission) {
    return true;
  }
  if (isSuperAdmin(user)) {
    return true;
  }
  return getUserPermissions(user).includes(permission);
}

/**
 * Determines whether the user has any of the given permissions.
 *
 * @param user The current logged-in user
 * @param permissions List of permission codes
 * @returns Whether the user has at least one of the permissions
 */
export function hasAnyPermission(user: User | null | undefined, permissions: string[]): boolean {
  if (!permissions.length) {
    return true;
  }
  if (isSuperAdmin(user)) {
    return true;
  }
  const userPermissions = new Set(getUserPermissions(user));
  return permissions.some((permission) => userPermissions.has(permission));
}

/**
 * Determines whether the user has all of the given permissions.
 *
 * @param user The current logged-in user
 * @param permissions List of permission codes
 * @returns Whether the user has all of the permissions
 */
export function hasAllPermissions(user: User | null | undefined, permissions: string[]): boolean {
  if (!permissions.length) {
    return true;
  }
  if (isSuperAdmin(user)) {
    return true;
  }
  const userPermissions = new Set(getUserPermissions(user));
  return permissions.every((permission) => userPermissions.has(permission));
}

/**
 * Determines whether the user has access to the admin backend entry point.
 *
 * @param user The current logged-in user
 * @returns Whether the user can access the admin backend
 */
export function hasAdminAccess(user?: User | null): boolean {
  return hasAnyPermission(user, ADMIN_PERMISSION_CODES);
}

/**
 * Gets the display-friendly role name shown in the UI.
 *
 * @param user The current logged-in user
 * @returns The display label for the role
 */
export function getPrimaryRoleLabel(user?: User | null): string {
  const roles = getUserRoles(user);
  if (roles.some((role) => role.includes('SUPER_ADMIN'))) {
    return 'Super Admin';
  }
  if (roles.some((role) => role.includes('ADMIN'))) {
    return 'Admin';
  }
  if (roles.some((role) => role.includes('REVIEWER'))) {
    return 'Reviewer';
  }
  if (roles.some((role) => role.includes('EDITOR'))) {
    return 'Editor';
  }
  return 'Member';
}
