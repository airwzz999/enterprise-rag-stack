import { http } from './request';
import { User, PageParams } from '@/types';

export interface UserFilter {
  keyword?: string;
  department?: string;
  position?: string;
  role?: string;
  status?: 'active' | 'inactive';
  page?: number;
  pageSize?: number;
}

export interface UserProfileUpdate {
  username?: string;
  email?: string;
  avatar?: string;
  department?: string;
  position?: string;
  phone?: string;
  bio?: string;
}

export interface PasswordChange {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface PageData<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}

export const userService = {
  // Get user list
  getUsers: async (filter: UserFilter): Promise<PageData<User>> => {
    const response = await http.get<any>('/auth/users/page', {
      params: {
        current: filter.page || 1,
        size: filter.pageSize || 10,
        keyword: filter.keyword,
        role: filter.role,
        status: filter.status,
      },
    });
    return {
      list: response?.records || [],
      total: response?.total || 0,
      page: response?.current || 1,
      pageSize: response?.size || 10,
    };
  },

  // Get user details
  getUser: (id: string) => {
    return http.get<User>(`/auth/users/${id}`);
  },

  // Get current user info
  getCurrentUser: () => {
    return http.get<User>('/auth/auth/me');
  },

  // Create user
  createUser: (data: {
    username: string;
    email: string;
    password?: string;
    realName?: string;
    department?: string;
    position?: string;
    status?: number;
  }) => {
    return http.post<number>('/auth/users', data);
  },

  // Update user info
  updateUser: (id: string, data: Partial<User>) => {
    return http.put<boolean>('/auth/users', { id, ...data });
  },

  // Update profile
  updateProfile: (data: UserProfileUpdate) => {
    return http.put<User>('/auth/users/me/profile', data);
  },

  // Change password
  changePassword: (data: PasswordChange) => {
    return http.put<void>('/auth/users/password/change', null, {
      params: {
        oldPassword: data.oldPassword,
        newPassword: data.newPassword,
      },
    });
  },

  // Upload avatar
  uploadAvatar: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return http.post<{ url: string }>('/auth/users/me/avatar', formData);
  },

  // Delete user
  deleteUser: (id: string) => {
    return http.delete<boolean>(`/auth/users/${id}`);
  },

  // Batch delete users
  batchDeleteUsers: (ids: string[]) => {
    return http.delete('/auth/users/batch', { data: { ids } });
  },

  // Enable/disable user
  toggleUserStatus: (id: string, status: 'active' | 'inactive') => {
    return http.patch(`/auth/users/${id}/status`, { status });
  },

  // Reset user password
  resetPassword: (id: string, newPassword: string) => {
    return http.put<boolean>(`/auth/users/${id}/password/reset`, null, {
      params: { newPassword },
    });
  },

  // Get user statistics
  getUserStats: (userId?: string) => {
    return http.get<{
      documentCount: number;
      viewCount: number;
      likeCount: number;
      commentCount: number;
    }>(userId ? `/auth/users/${userId}/stats` : '/auth/users/me/stats');
  },

  // Get user activity
  getUserActivity: (userId?: string, params?: PageParams) => {
    return http.get<{
      list: Array<{ type: string; description: string; createdAt: string }>;
      total: number;
    }>(userId ? `/auth/users/${userId}/activity` : '/auth/users/me/activity', { params });
  },

  // Search users
  searchUsers: (keyword: string) => {
    return http.get<User[]>('/auth/users/search', { params: { keyword } });
  },

  // Get online users
  getOnlineUsers: () => {
    return http.get<User[]>('/auth/users/online');
  },

  // Assign roles to user
  assignRoles: (userId: string, roleIds: string[], config?: any) => {
    return http.post<boolean>(
      `/auth/users/${userId}/roles`,
      roleIds.map((id) => String(id)),
      config
    );
  },

  // Get user role list
  getUserRoles: (userId: string, config?: any) => {
    return http.get<Array<string | number>>(`/auth/users/${userId}/roles`, config).then((res) =>
      (res || []).map((id) => String(id))
    );
  },

  // Assign permissions to user
  assignPermissions: (userId: string, permissionIds: string[], config?: any) => {
    return http.post<boolean>(
      `/auth/users/${userId}/permissions`,
      permissionIds.map((id) => String(id)),
      config
    );
  },

  // Get all permissions of a user
  getUserPermissions: (userId: string, config?: any) => {
    return http.get<string[]>(`/auth/users/${userId}/permissions`, config);
  },
};

export default userService;
