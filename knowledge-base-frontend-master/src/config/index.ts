/**
 * Application configuration
 */

// API base URL
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// WebSocket URL
export const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL || '/ws';

// Application config
export const APP_CONFIG = {
  // Application name
  name: import.meta.env.VITE_APP_NAME || 'Enterprise Knowledge Base',

  // Application description
  description: import.meta.env.VITE_APP_DESCRIPTION || 'Intelligent enterprise knowledge management platform',

  // Version
  version: import.meta.env.VITE_APP_VERSION || '1.0.0',

  // Environment
  env: import.meta.env.VITE_APP_ENV || 'development',

  // Whether debug mode is enabled
  debug: import.meta.env.VITE_APP_DEBUG === 'true',

  // Whether performance monitoring is enabled
  enablePerformance: import.meta.env.VITE_APP_PERFORMANCE === 'true',

  // Whether error reporting is enabled
  enableErrorReport: import.meta.env.VITE_APP_ERROR_REPORT === 'true',
} as const;

// File upload config
export const UPLOAD_CONFIG = {
  // Max file size (bytes)
  maxFileSize: parseInt(import.meta.env.VITE_UPLOAD_MAX_SIZE || '10485760'), // 10MB

  // Allowed file types
  allowedTypes: (import.meta.env.VITE_UPLOAD_ALLOWED_TYPES || '').split(',') || [
    '.doc', '.docx', '.pdf', '.txt', '.md',
    '.xls', '.xlsx', '.ppt', '.pptx',
    '.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg', '.ico',
    '.mp4', '.avi', '.mov', '.wmv', '.flv', '.mkv', '.webm',
    '.mp3', '.wav', '.flac', '.aac', '.ogg', '.m4a', '.wma',
  ],

  // Upload URL
  uploadUrl: `${API_BASE_URL}/files/upload`,

  // Batch upload URL
  batchUploadUrl: `${API_BASE_URL}/files/batch-upload`,
} as const;

// Storage config
export const STORAGE_CONFIG = {
  // Token storage key
  tokenKey: 'token',

  // User info storage key
  userKey: 'user',

  // Theme storage key
  themeKey: 'theme',

  // Language storage key
  languageKey: 'language',

  // Sidebar collapsed state storage key
  sidebarCollapsedKey: 'sidebar_collapsed',

  // Recent documents storage key
  recentDocumentsKey: 'recent_documents',

  // Search history storage key
  searchHistoryKey: 'search_history',

  // Max number of history items to store
  maxHistoryItems: 20,
} as const;

// Cache config
export const CACHE_CONFIG = {
  // Whether caching is enabled
  enabled: import.meta.env.VITE_CACHE_ENABLED !== 'false',

  // Cache TTL (ms)
  defaultTTL: 5 * 60 * 1000, // 5 minutes

  // Document cache TTL
  documentTTL: 10 * 60 * 1000, // 10 minutes

  // User info cache TTL
  userTTL: 30 * 60 * 1000, // 30 minutes

  // Category cache TTL
  categoryTTL: 60 * 60 * 1000, // 1 hour
} as const;

// Request config
export const REQUEST_CONFIG = {
  // Request timeout (ms)
  timeout: parseInt(import.meta.env.VITE_REQUEST_TIMEOUT || '30000'), // 30 seconds

  // Retry count
  retryTimes: parseInt(import.meta.env.VITE_REQUEST_RETRY || '3'),

  // Retry delay (ms)
  retryDelay: parseInt(import.meta.env.VITE_REQUEST_RETRY_DELAY || '1000'),

  // Whether request caching is enabled
  enableCache: import.meta.env.VITE_REQUEST_CACHE === 'true',

  // Whether to show request logs
  showLog: import.meta.env.VITE_APP_DEBUG === 'true',
} as const;

// Pagination config
export const PAGINATION_CONFIG = {
  // Default page number
  defaultPage: 1,

  // Default page size
  defaultPageSize: parseInt(import.meta.env.VITE_PAGE_SIZE || '12'),

  // Page size options
  pageSizeOptions: [12, 24, 48, 96],

  // Whether to show the quick-jump control
  showQuickJumper: true,

  // Whether to show the total count
  showTotal: true,
} as const;

// Editor config
export const EDITOR_CONFIG = {
  // Auto-save interval (ms)
  autoSaveInterval: parseInt(import.meta.env.VITE_EDITOR_AUTO_SAVE || '10000'), // 10 seconds

  // Whether auto-save is enabled
  enableAutoSave: import.meta.env.VITE_EDITOR_AUTO_SAVE !== 'false',

  // Max number of history entries
  maxHistory: 50,

  // Default editor height
  defaultHeight: 600,

  // Minimum editor height
  minHeight: 300,
} as const;

// AI config
export const AI_CONFIG = {
  // API base URL
  baseUrl: import.meta.env.VITE_AI_API_URL || `${API_BASE_URL}/ai`,

  // Model name
  model: import.meta.env.VITE_AI_MODEL || 'gpt-3.5-turbo',

  // Max tokens
  maxTokens: parseInt(import.meta.env.VITE_AI_MAX_TOKENS || '2000'),

  // Temperature
  temperature: parseFloat(import.meta.env.VITE_AI_TEMPERATURE || '0.7'),

  // Whether streaming responses are enabled
  enableStream: import.meta.env.VITE_AI_STREAM !== 'false',

  // Timeout (ms)
  timeout: parseInt(import.meta.env.VITE_AI_TIMEOUT || '60000'), // 60 seconds
} as const;

// WebSocket config
export const WS_CONFIG = {
  // Whether WebSocket is enabled
  enabled: import.meta.env.VITE_WS_ENABLED === 'true',

  // Reconnect interval (ms)
  reconnectInterval: parseInt(import.meta.env.VITE_WS_RECONNECT_INTERVAL || '5000'),

  // Max reconnect attempts
  maxReconnectTimes: parseInt(import.meta.env.VITE_WS_MAX_RECONNECT || '5'),

  // Heartbeat interval (ms)
  heartbeatInterval: parseInt(import.meta.env.VITE_WS_HEARTBEAT || '30000'), // 30 seconds
} as const;

// Performance monitoring config
export const PERFORMANCE_CONFIG = {
  // Whether performance monitoring is enabled
  enabled: APP_CONFIG.enablePerformance,

  // Sample rate (0-1)
  sampleRate: parseFloat(import.meta.env.VITE_PERFORMANCE_SAMPLE_RATE || '0.1'),

  // Report URL
  reportUrl: import.meta.env.VITE_PERFORMANCE_REPORT_URL || `${API_BASE_URL}/performance/report`,

  // Batch report size
  batchReportSize: parseInt(import.meta.env.VITE_PERFORMANCE_BATCH_SIZE || '10'),
} as const;

// Error reporting config
export const ERROR_REPORT_CONFIG = {
  // Whether error reporting is enabled
  enabled: APP_CONFIG.enableErrorReport,

  // Report URL
  reportUrl: import.meta.env.VITE_ERROR_REPORT_URL || `${API_BASE_URL}/error/report`,

  // Whether to report console errors
  reportConsoleError: import.meta.env.VITE_ERROR_REPORT_CONSOLE === 'true',

  // Whether to report unhandled promise rejections
  reportUnhandledRejection: import.meta.env.VITE_ERROR_REPORT_REJECTION === 'true',

  // Whether to report resource loading errors
  reportResourceError: import.meta.env.VITE_ERROR_REPORT_RESOURCE === 'true',
} as const;

// Third-party service config
export const THIRD_PARTY_CONFIG = {
  // Baidu Analytics
  baiduAnalytics: {
    enabled: !!import.meta.env.VITE_BAIDU_ANALYTICS_ID,
    id: import.meta.env.VITE_BAIDU_ANALYTICS_ID || '',
  },

  // Google Analytics
  googleAnalytics: {
    enabled: !!import.meta.env.VITE_GA_MEASUREMENT_ID,
    id: import.meta.env.VITE_GA_MEASUREMENT_ID || '',
  },

  // Sentry
  sentry: {
    enabled: !!import.meta.env.VITE_SENTRY_DSN,
    dsn: import.meta.env.VITE_SENTRY_DSN || '',
  },
} as const;

// Export all config
export const config = {
  api: API_BASE_URL,
  wsUrl: WS_BASE_URL,
  app: APP_CONFIG,
  upload: UPLOAD_CONFIG,
  storage: STORAGE_CONFIG,
  cache: CACHE_CONFIG,
  request: REQUEST_CONFIG,
  pagination: PAGINATION_CONFIG,
  editor: EDITOR_CONFIG,
  ai: AI_CONFIG,
  ws: WS_CONFIG,
  performance: PERFORMANCE_CONFIG,
  errorReport: ERROR_REPORT_CONFIG,
  thirdParty: THIRD_PARTY_CONFIG,
} as const;

export default config;
