import { create } from 'zustand';
import { SystemNotification } from '@/types';
import { notificationService } from '@/services';
import { extractDocumentIdFromLink, resolveNotificationTarget } from '@/utils/notification-link';

/** Maps raw backend notification fields to the frontend SystemNotification type */
function mapNotification(raw: any): SystemNotification {
  const documentId = String(raw?.relatedId ?? extractDocumentIdFromLink(raw?.link) ?? '');
  const target = resolveNotificationTarget({
    type: raw?.notificationType,
    link: raw?.link,
    documentId,
  });
  return {
    id: String(raw?.id ?? ''),
    type: (raw?.notificationType as SystemNotification['type']) || 'system',
    title: (raw?.title as string) || '',
    content: (raw?.content as string) || '',
    link: target.url,
    documentId: documentId || undefined,
    read: Boolean(raw?.isRead ?? raw?.read ?? false),
    createdAt: (raw?.createdAt as string) || '',
  };
}

interface NotificationState {
  notifications: SystemNotification[];
  unreadCount: number;
  isLoading: boolean;

  // Actions
  fetchNotifications: (params?: { page?: number; pageSize?: number; type?: string }) => Promise<void>;
  fetchUnreadCount: () => Promise<void>;
  markAsRead: (id: string) => Promise<void>;
  markAllAsRead: () => Promise<void>;
  deleteNotification: (id: string) => Promise<void>;
  clearAll: () => Promise<void>;
  reset: () => void;

  /** WebSocket real-time push: inserts a new notification at the head of the list */
  addNotification: (notification: SystemNotification) => void;
  /** Atomically increment the unread count (used for WebSocket real-time push) */
  incrementUnreadCount: (delta?: number) => void;
}

export const useNotificationStore = create<NotificationState>((set) => ({
  notifications: [],
  unreadCount: 0,
  isLoading: false,

  fetchNotifications: async (params) => {
    set({ isLoading: true });
    try {
      const requestParams = {
        page: 1,
        pageSize: 1000,
        ...params,
      };
      const [response, unreadCount] = await Promise.all([
        notificationService.getNotifications(requestParams),
        notificationService.getUnreadCount(),
      ]);
      const records: any[] = (response as any).records ?? [];
      const mapped = records.map(mapNotification);
      set({
        notifications: mapped,
        unreadCount: Number(unreadCount) || mapped.filter((n) => !n.read).length,
        isLoading: false,
      });
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  fetchUnreadCount: async () => {
    try {
      const count = await notificationService.getUnreadCount();
      set({ unreadCount: Number(count) || 0 });
    } catch (error) {
      // Error handled
    }
  },

  markAsRead: async (id: string) => {
    await notificationService.markAsRead(id);
    set((state) => ({
      notifications: state.notifications.map((n) =>
        n.id === id ? { ...n, read: true } : n
      ),
      unreadCount: Math.max(0, state.unreadCount - 1),
    }));
  },

  markAllAsRead: async () => {
    await notificationService.markAllAsRead();
    set((state) => ({
      notifications: state.notifications.map((n) => ({ ...n, read: true })),
      unreadCount: 0,
    }));
  },

  deleteNotification: async (id: string) => {
    await notificationService.deleteNotification(id);
    set((state) => ({
      notifications: state.notifications.filter((n) => n.id !== id),
      unreadCount: state.notifications.find((n) => n.id === id)?.read
        ? state.unreadCount
        : Math.max(0, state.unreadCount - 1),
    }));
  },

  clearAll: async () => {
    await notificationService.clearAll();
    set({ notifications: [], unreadCount: 0 });
  },

  reset: () => {
    set({ notifications: [], unreadCount: 0 });
  },

  addNotification: (notification: SystemNotification) => {
    set((state) => ({
      notifications: [notification, ...state.notifications],
      unreadCount: notification.read ? Number(state.unreadCount) : Number(state.unreadCount) + 1,
    }));
  },

  incrementUnreadCount: (delta: number = 1) => {
    set((state) => ({
      unreadCount: Math.max(0, Number(state.unreadCount) + delta),
    }));
  },
}));
