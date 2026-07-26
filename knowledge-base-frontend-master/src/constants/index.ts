/**
 * System constant configuration
 */

// Document-related constants
export const DOCUMENT_STATUS = {
  DRAFT: 'draft',
  PENDING_REVIEW: 'pending_review',
  PUBLISHED: 'published',
  ARCHIVED: 'archived',
} as const;

export const DOCUMENT_STATUS_TEXT = {
  [DOCUMENT_STATUS.DRAFT]: 'Draft',
  [DOCUMENT_STATUS.PENDING_REVIEW]: 'Pending Review',
  [DOCUMENT_STATUS.PUBLISHED]: 'Published',
  [DOCUMENT_STATUS.ARCHIVED]: 'Archived',
} as const;

export const DOCUMENT_STATUS_COLORS = {
  [DOCUMENT_STATUS.DRAFT]: 'default',
  [DOCUMENT_STATUS.PENDING_REVIEW]: 'processing',
  [DOCUMENT_STATUS.PUBLISHED]: 'success',
  [DOCUMENT_STATUS.ARCHIVED]: 'warning',
} as const;

// User role constants
export const USER_ROLES = {
  ADMIN: 'admin',
  USER: 'user',
  GUEST: 'guest',
} as const;

export const USER_ROLE_TEXT = {
  [USER_ROLES.ADMIN]: 'Admin',
  [USER_ROLES.USER]: 'User',
  [USER_ROLES.GUEST]: 'Guest',
} as const;

// Notification type constants
export const NOTIFICATION_TYPES = {
  SYSTEM: 'system',
  COMMENT: 'comment',
  MENTION: 'mention',
  REVIEW: 'review',
  LIKE: 'like',
} as const;

export const NOTIFICATION_TYPE_TEXT = {
  [NOTIFICATION_TYPES.SYSTEM]: 'System Notification',
  [NOTIFICATION_TYPES.COMMENT]: 'Comment Notification',
  [NOTIFICATION_TYPES.MENTION]: 'Mention Notification',
  [NOTIFICATION_TYPES.REVIEW]: 'Review Notification',
  [NOTIFICATION_TYPES.LIKE]: 'Like Notification',
} as const;

// Review status constants
export const REVIEW_STATUS = {
  PENDING: 'pending',
  APPROVED: 'approved',
  REJECTED: 'rejected',
} as const;

export const REVIEW_STATUS_TEXT = {
  [REVIEW_STATUS.PENDING]: 'Pending Review',
  [REVIEW_STATUS.APPROVED]: 'Approved',
  [REVIEW_STATUS.REJECTED]: 'Rejected',
} as const;

export const REVIEW_STATUS_COLORS = {
  [REVIEW_STATUS.PENDING]: 'processing',
  [REVIEW_STATUS.APPROVED]: 'success',
  [REVIEW_STATUS.REJECTED]: 'error',
} as const;

// File type constants
export const FILE_TYPES = {
  DOCUMENT: '.doc,.docx,.pdf,.txt,.md,.markdown',
  IMAGE: '.jpg,.jpeg,.png,.gif,.webp,.svg,.bmp',
  VIDEO: '.mp4,.avi,.mov,.wmv,.flv,.mkv',
  AUDIO: '.mp3,.wav,.flac,.aac,.ogg',
  ARCHIVE: '.zip,.rar,.7z,.tar,.gz',
} as const;

// Pagination config
export const PAGINATION = {
  DEFAULT_PAGE: 1,
  DEFAULT_PAGE_SIZE: 12,
  PAGE_SIZE_OPTIONS: [12, 24, 48, 96],
} as const;

// Auto-save config
export const AUTO_SAVE = {
  INTERVAL: 10000, // 10 seconds
  DEBOUNCE_DELAY: 2000, // 2-second debounce
} as const;

// Upload config
export const UPLOAD = {
  MAX_FILE_SIZE: 10 * 1024 * 1024, // 10MB
  MAX_IMAGE_SIZE: 5 * 1024 * 1024, // 5MB
  ALLOWED_FILE_TYPES: FILE_TYPES.DOCUMENT,
  ALLOWED_IMAGE_TYPES: FILE_TYPES.IMAGE,
} as const;

// Storage key names
export const STORAGE_KEYS = {
  TOKEN: 'token',
  USER: 'user',
  THEME: 'theme',
  LANGUAGE: 'language',
  SIDEBAR_COLLAPSED: 'sidebar_collapsed',
  RECENT_DOCUMENTS: 'recent_documents',
  SEARCH_HISTORY: 'search_history',
} as const;

// API error codes
export const API_ERROR_CODES = {
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  SERVER_ERROR: 500,
  NETWORK_ERROR: 0,
} as const;

// Theme config
export const THEMES = {
  LIGHT: 'light',
  DARK: 'dark',
  AUTO: 'auto',
} as const;

// Language config
export const LANGUAGES = {
  ZH_CN: 'zh-CN',
  EN_US: 'en-US',
} as const;

export const LANGUAGE_TEXT = {
  [LANGUAGES.ZH_CN]: 'Simplified Chinese',
  [LANGUAGES.EN_US]: 'English',
} as const;

// Route paths
export const ROUTES = {
  LOGIN: '/login',
  HOME: '/',
  DOCUMENTS: '/documents',
  DOCUMENT_CREATE: '/documents/new',
  DOCUMENT_IMPORT: '/documents/import',
  DOCUMENT_DETAIL: (id: string) => `/documents/${id}`,
  DOCUMENT_EDIT: (id: string) => `/documents/${id}/edit`,
  DOCUMENT_VERSIONS: (id: string) => `/documents/${id}/versions`,
  SEARCH: '/search',
  AI_ASSISTANT: '/ai',
  KNOWLEDGE_GRAPH: '/knowledge-graph',
  PROFILE: '/profile',
  NOTIFICATIONS: '/notifications',
  ADMIN: '/admin',
  ADMIN_USERS: '/admin/users',
  ADMIN_ROLES: '/admin/roles',
  ADMIN_TEAMS: '/admin/teams',
  ADMIN_CATEGORIES: '/admin/categories',
  ADMIN_REVIEW: '/admin/review',
  ADMIN_SETTINGS: '/admin/settings',
  ADMIN_STATISTICS: '/admin/statistics',
  SHARE_VIEW: (shareId: string) => `/share/${shareId}`,
} as const;

// Keyboard shortcuts
export const KEYBOARD_SHORTCUTS = {
  SAVE: 'Ctrl+S',
  SEARCH: 'Ctrl+K',
  NEW_DOCUMENT: 'Ctrl+N',
  BOLD: 'Ctrl+B',
  ITALIC: 'Ctrl+I',
  UNDERLINE: 'Ctrl+U',
} as const;

// Regular expressions
export const REGEX = {
  EMAIL: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  PHONE: /^1[3-9]\d{9}$/,
  URL: /^https?:\/\/.+/,
  PASSWORD: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d]{8,}$/,
} as const;

// Date formats
export const DATE_FORMATS = {
  DATE: 'YYYY-MM-DD',
  TIME: 'HH:mm:ss',
  DATETIME: 'YYYY-MM-DD HH:mm:ss',
  MONTH: 'YYYY-MM',
  YEAR: 'YYYY',
} as const;

// Chart colors
export const CHART_COLORS = [
  '#1890ff',
  '#52c41a',
  '#faad14',
  '#f5222d',
  '#722ed1',
  '#fa8c16',
  '#13c2c2',
  '#eb2f96',
] as const;

// Default config
export const DEFAULT_CONFIG = {
  SITE_NAME: 'Enterprise Knowledge Base',
  SITE_DESCRIPTION: 'Intelligent enterprise knowledge management platform',
  LOGO: '/logo.svg',
  FAVICON: '/favicon.ico',
} as const;

// AI config
export const AI_CONFIG = {
  MAX_TOKENS: 4000,
  TEMPERATURE: 0.3,
  DEFAULT_MODEL: 'qwen',
  MODELS: {
    qwen: {
      key: 'qwen',
      displayName: 'Qwen',
      description: 'Alibaba Cloud LLM with support for multi-turn dialogue, text generation, and more',
      color: '#2563eb',
    },
    deepseek: {
      key: 'deepseek',
      displayName: 'DeepSeek',
      description: 'DeepSeek LLM, strong at code generation and deep reasoning',
      color: '#10b981',
    },
  },
} as const;

// Knowledge graph config
export const GRAPH_CONFIG = {
  NODE_SIZE_RANGE: [10, 50],
  LINK_WIDTH_RANGE: [1, 5],
  REPULSION: 200,
  EDGE_LENGTH: 120,
} as const;

// Search config
export const SEARCH_CONFIG = {
  MIN_KEYWORD_LENGTH: 2,
  MAX_RESULTS: 100,
  SUGGESTIONS_LIMIT: 10,
  HISTORY_LIMIT: 20,
} as const;
