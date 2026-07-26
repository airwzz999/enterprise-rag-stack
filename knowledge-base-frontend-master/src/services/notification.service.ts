import { http } from './request';
import { SystemNotification } from '@/types';

export const notificationService = {
  // Get notification list
  getNotifications: (params?: { page?: number; pageSize?: number; type?: string; isRead?: number }) => {
    const requestParams: Record<string, any> = {};
    if (params?.page !== undefined) {
      requestParams.current = params.page;
    }
    if (params?.pageSize !== undefined) {
      requestParams.size = params.pageSize;
    }
    if (params?.isRead !== undefined) {
      requestParams.isRead = params.isRead;
    }
    return http.get<{ records: SystemNotification[]; total: number; current: number; size: number }>('/notifications', {
      params: requestParams,
    });
  },

  // Get unread notification count
  getUnreadCount: () => {
    return http.get<number>('/notifications/unread-count');
  },

  // Mark as read
  markAsRead: (id: string) => {
    return http.put(`/notifications/${id}/read`);
  },

  // Mark all as read
  markAllAsRead: () => {
    return http.put('/notifications/read-all');
  },

  // Delete notification
  deleteNotification: (id: string) => {
    return http.delete(`/notifications/${id}`);
  },

  // Clear all notifications
  clearAll: () => {
    return http.delete('/notifications/all');
  },
};

export default notificationService;
