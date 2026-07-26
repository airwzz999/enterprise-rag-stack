import { http } from './request';
import { EntityId, PageResponse } from '@/types';

// ==================== Type definitions ====================

/**
 * Notification-related types
 */
export interface FoundationNotification {
  id: EntityId;
  userId: EntityId;
  userName?: string;
  notificationType: 'system' | 'comment' | 'mention' | 'review' | 'like';
  title: string;
  content: string;
  link?: string;
  relatedType?: string;
  relatedId?: EntityId;
  isRead: 0 | 1;
  readTime?: string;
  createdAt: string;
}

export interface NotificationListParams {
  current?: number;
  size?: number;
  userId?: EntityId;
  isRead?: 0 | 1;
}

export interface SendNotificationData {
  userId: EntityId;
  notificationType: FoundationNotification['notificationType'];
  title: string;
  content: string;
  link?: string;
  relatedType?: string;
  relatedId?: EntityId;
}

/**
 * System configuration related types
 */
export interface SystemConfig {
  id: EntityId;
  configKey: string;
  configValue: string;
  configType: 'string' | 'number' | 'boolean' | 'json';
  category: 'AI' | 'STORAGE' | 'NOTIFICATION' | 'SECURITY' | 'SYSTEM' | string;
  description?: string;
  isPublic: 0 | 1;
  createdAt: string;
  updatedAt: string;
}

export interface SystemConfigListParams {
  current?: number;
  size?: number;
  category?: string;
}

export interface CreateSystemConfigData {
  configKey: string;
  configValue: string;
  configType: SystemConfig['configType'];
  category: string;
  description?: string;
  isPublic?: 0 | 1;
}

export interface PublicConfigs {
  [key: string]: string;
}

/** Public config item */
export interface PublicConfigItem {
  id: EntityId;
  configKey: string;
  configValue: string;
  configType: string;
  category: string;
  description?: string;
  isPublic: 0 | 1;
  createdAt: string;
  updatedAt: string;
}

/**
 * Operation log related types
 */
export interface OperationLog {
  id: EntityId;
  module: string;
  operationType: string;
  operationDesc: string;
  requestMethod: string;
  requestUrl: string;
  requestParams?: string;
  responseResult?: string;
  userId?: EntityId;
  username?: string;
  ipAddress?: string;
  location?: string;
  userAgent?: string;
  executeTime?: number;
  status: 0 | 1;
  errorMsg?: string;
  createdAt: string;
}

export interface OperationLogListParams {
  current?: number;
  size?: number;
  module?: string;
  operationType?: string;
  username?: string;
  startTime?: string;
  endTime?: string;
}

export interface LogStatistics {
  totalLogs: number;
  successLogs: number;
  failedLogs: number;
  operationTypeStats: Record<string, number>;
  moduleStats: Record<string, number>;
  userStats: Array<{ username: string; count: number }>;
  trendData: Array<{ date: string; count: number }>;
}

/**
 * Dictionary-related types
 */
export interface Dict {
  id: EntityId;
  dictCode: string;
  dictName: string;
  dictType: string;
  description?: string;
  sort: number;
  status: 0 | 1;
  createdAt: string;
  updatedAt: string;
}

export interface DictListParams {
  current?: number;
  size?: number;
  keyword?: string;
}

export interface CreateDictData {
  dictCode: string;
  dictName: string;
  dictType: string;
  description?: string;
  sort?: number;
  status?: 0 | 1;
}

export interface DictData {
  id: EntityId;
  dictId: EntityId;
  dictCode: string;
  dictLabel: string;
  dictValue: string;
  dictSort: number;
  cssClass?: string;
  listClass?: string;
  isDefault: 0 | 1;
  status: 0 | 1;
  createdAt: string;
}

export interface CreateDictDataData {
  dictLabel: string;
  dictValue: string;
  dictSort?: number;
  cssClass?: string;
  listClass?: string;
  isDefault?: 0 | 1;
  status?: 0 | 1;
}

// ==================== Notification APIs ====================

/**
 * Get notification list (paginated)
 */
export const getNotifications = (params?: NotificationListParams) => {
  return http.get<PageResponse<FoundationNotification>>('/notifications', { params });
};

/**
 * Get notification details
 */
export const getNotificationDetail = (id: EntityId) => {
  return http.get<FoundationNotification>(`/notifications/${id}`);
};

/**
 * Send notification
 */
export const sendNotification = (data: SendNotificationData) => {
  return http.post<boolean>('/notifications', data);
};

/**
 * Mark as read
 */
export const markAsRead = (id: EntityId) => {
  return http.put<boolean>(`/notifications/${id}/read`);
};

/**
 * Mark all as read
 */
export const markAllAsRead = (userId: EntityId) => {
  return http.put<boolean>('/notifications/read-all', null, { params: { userId } });
};

/**
 * Delete notification
 */
export const deleteNotification = (id: EntityId) => {
  return http.delete<boolean>(`/notifications/${id}`);
};

/**
 * Get unread count
 */
export const getUnreadCount = (userId: EntityId) => {
  return http.get<number>('/notifications/unread-count', { params: { userId } });
};

// ==================== System config APIs ====================

/**
 * Get config list (paginated)
 */
export const getSystemConfigs = (params?: SystemConfigListParams) => {
  return http.get<PageResponse<SystemConfig>>('/config', { params });
};

/**
 * Get config item
 */
export const getSystemConfig = (key: string) => {
  return http.get<SystemConfig>(`/config/${key}`);
};

/**
 * Create config
 */
export const createSystemConfig = (data: CreateSystemConfigData) => {
  return http.post<boolean>('/config', data);
};

/**
 * Update config
 */
export const updateSystemConfig = (key: string, data: CreateSystemConfigData) => {
  return http.put<boolean>(`/config/${key}`, data);
};

/**
 * Delete config
 */
export const deleteSystemConfig = (key: string) => {
  return http.delete<boolean>(`/config/${key}`);
};

/**
 * Get config by category
 */
export const getConfigsByCategory = (category: string) => {
  return http.get<SystemConfig[]>(`/config/category/${category}`);
};

/**
 * Get public config
 *
 * <p>Supports two backend response formats:</p>
 * <ul>
 *   <li>Old format: {@code {key: value}} Map</li>
 *   <li>New format: {@code [{configKey, configValue}]} array</li>
 * </ul>
 */
export const getPublicConfigs = () => {
  return http.get<any>('/config/public').then((data): PublicConfigs => {
    // New format: array [{configKey, configValue}, ...]
    if (Array.isArray(data)) {
      const configs: PublicConfigs = {};
      data.forEach((item: PublicConfigItem) => {
        configs[item.configKey] = item.configValue;
      });
      return configs;
    }
    // Old format: already a {key: value} Map
    return data;
  });
};

// ==================== Operation log APIs ====================

/**
 * Get log list (paginated)
 */
export const getOperationLogs = (params?: OperationLogListParams) => {
  return http.get<PageResponse<OperationLog>>('/logs', { params });
};

/**
 * Get log details
 */
export const getOperationLogDetail = (id: EntityId) => {
  return http.get<OperationLog>(`/logs/${id}`);
};

/**
 * Get log statistics
 */
export const getLogStatistics = (params?: { startTime?: string; endTime?: string }) => {
  return http.get<LogStatistics>('/logs/statistics', { params });
};

/**
 * Delete logs before a specified date
 */
export const deleteLogsBeforeDate = (date: string) => {
  return http.delete<number>('/logs/before-date', { params: { beforeDate: date } });
};

// ==================== Dictionary APIs ====================

/**
 * Get dictionary type list
 */
export const getDicts = (params?: DictListParams) => {
  return http.get<PageResponse<Dict>>('/dicts', { params });
};

/**
 * Get dictionary details
 */
export const getDictByCode = (code: string) => {
  return http.get<Dict>(`/dicts/${code}`);
};

/**
 * Create dictionary
 */
export const createDict = (data: CreateDictData) => {
  return http.post<boolean>('/dicts', data);
};

/**
 * Update dictionary
 */
export const updateDict = (code: string, data: CreateDictData) => {
  return http.put<boolean>(`/dicts/${code}`, data);
};

/**
 * Delete dictionary
 */
export const deleteDict = (code: string) => {
  return http.delete<boolean>(`/dicts/${code}`);
};

/**
 * Get dictionary data
 */
export const getDictData = (code: string) => {
  return http.get<DictData[]>(`/dicts/${code}/data`);
};

/**
 * Add dictionary data
 */
export const addDictData = (code: string, data: CreateDictDataData) => {
  return http.post<boolean>(`/dicts/${code}/data`, data);
};

/**
 * Update dictionary data
 */
export const updateDictData = (code: string, data: CreateDictDataData & { id: EntityId }) => {
  return http.put<boolean>(`/dicts/${code}/data`, data);
};

/**
 * Delete dictionary data
 */
export const deleteDictData = (code: string, id: EntityId) => {
  return http.delete<boolean>(`/dicts/${code}/data/${id}`);
};

// ==================== Notification template types ====================

export interface NotificationTemplate {
  id?: EntityId;
  templateCode: string;
  templateName: string;
  notificationType: 'EMAIL' | 'SMS' | 'WECHAT' | 'SYSTEM' | 'BROWSER';
  title: string;
  content: string;
  variables?: string;
  description?: string;
  isActive: 0 | 1;
  createdAt?: string;
  updatedAt?: string;
}

export interface TemplateListParams {
  current?: number;
  size?: number;
  notificationType?: string;
}

// ==================== Notification template APIs ====================

/**
 * Get notification template list (paginated)
 */
export const getNotificationTemplates = (params?: TemplateListParams) => {
  return http.get<PageResponse<NotificationTemplate>>('/notifications/templates', { params });
};

/**
 * Get all active templates
 */
export const getActiveTemplates = () => {
  return http.get<NotificationTemplate[]>('/notifications/templates/active');
};

/**
 * Get template details
 */
export const getNotificationTemplateDetail = (id: EntityId) => {
  return http.get<NotificationTemplate>(`/notifications/templates/${id}`);
};

/**
 * Create template
 */
export const createNotificationTemplate = (data: NotificationTemplate) => {
  return http.post<boolean>('/notifications/templates', data);
};

/**
 * Update template
 */
export const updateNotificationTemplate = (id: EntityId, data: NotificationTemplate) => {
  return http.put<boolean>(`/notifications/templates/${id}`, data);
};

/**
 * Delete template
 */
export const deleteNotificationTemplate = (id: EntityId) => {
  return http.delete<boolean>(`/notifications/templates/${id}`);
};

/**
 * Test-send a template
 */
export const testNotificationTemplate = (id: EntityId, target: string) => {
  return http.post<boolean>(`/notifications/templates/${id}/test`, null, { params: { target } });
};

// ==================== Export service object ====================

/**
 * Foundation base service
 *
 * Provides API interfaces for foundational functionality such as notification management,
 * system configuration, operation logs, and dictionary management
 */
export const foundationService = {
  // Notification-related
  notification: {
    list: getNotifications,
    detail: getNotificationDetail,
    send: sendNotification,
    markAsRead,
    markAllAsRead,
    delete: deleteNotification,
    unreadCount: getUnreadCount,
  },

  // System config-related
  config: {
    list: getSystemConfigs,
    get: getSystemConfig,
    create: createSystemConfig,
    update: updateSystemConfig,
    delete: deleteSystemConfig,
    getByCategory: getConfigsByCategory,
    getPublic: getPublicConfigs,
  },

  // Operation log-related
  log: {
    list: getOperationLogs,
    detail: getOperationLogDetail,
    statistics: getLogStatistics,
    deleteBeforeDate: deleteLogsBeforeDate,
  },

  // Notification template-related
  notificationTemplate: {
    list: getNotificationTemplates,
    listActive: getActiveTemplates,
    detail: getNotificationTemplateDetail,
    create: createNotificationTemplate,
    update: updateNotificationTemplate,
    delete: deleteNotificationTemplate,
    test: testNotificationTemplate,
  },

  // Dictionary-related
  dict: {
    list: getDicts,
    getByCode: getDictByCode,
    create: createDict,
    update: updateDict,
    delete: deleteDict,
    getData: getDictData,
    addData: addDictData,
    updateData: updateDictData,
    deleteData: deleteDictData,
  },
};

export default foundationService;
