import { http } from './request';
import { Role, Permission } from '@/types';

export const roleService = {
  // Get role list
  getRoles: (config?: any) => http.get<Role[]>('/auth/roles/list', config),

  // Get role details
  getRole: (id: string) => {
    return http.get<Role>(`/auth/roles/${id}`);
  },

  // Create role
  createRole: (data: { name: string; code: string; description?: string; permissions: string[] }) => {
    const { permissions: _permissions, ...payload } = data;
    return http.post<string | number>('/auth/roles', payload);
  },

  // Update role
  updateRole: (
    id: string,
    data: { name?: string; code?: string; description?: string; permissions?: string[] }
  ) => {
    const { permissions: _permissions, ...payload } = data;
    return http.put<boolean>('/auth/roles', { id: String(id), ...payload });
  },

  // Delete role
  deleteRole: (id: string) => http.delete<boolean>(`/auth/roles/${id}`),

  // Get all permissions
  getPermissions: () => http.get<Permission[]>('/auth/permissions'),

  // Get role permissions
  getRolePermissions: (roleId: string, config?: any) =>
    http.get<Array<string | number>>(`/auth/roles/${roleId}/permissions`, config).then((res) =>
      (res || []).map((id) => String(id))
    ),

  // Assign role permissions
  assignPermissions: (roleId: string, permissionIds: string[], config?: any) =>
    http.post<boolean>(
      `/auth/roles/${roleId}/permissions`,
      permissionIds.map((id) => String(id)),
      config
    ),
};

export default roleService;
