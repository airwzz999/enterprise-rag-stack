export type EntityId = string | number;

// User-related types
export interface User {
  id: EntityId;
  username: string;
  nickname?: string;
  realName?: string;
  email: string;
  phone?: string;
  avatar?: string;
  gender?: number;
  status: number | 'active' | 'inactive';
  remark?: string;
  department?: string;
  position?: string;
  role?: string;
  roles?: string[];
  permissions?: string[];
  createdAt?: string;
  updatedAt?: string;
  lastLoginTime?: string;
  lastLoginIp?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

export interface RegisterRequest {
  username: string;
  password: string;
  confirmPassword: string;
  email: string;
  realName: string;
  teamId: string;
  phone?: string;
}

/** Registration response (supports the email verification flow) */
export interface RegisterResponse {
  emailVerificationRequired: boolean;
  message: string;
  loginInfo?: {
    accessToken: string;
    refreshToken: string;
    tokenType: string;
    expiresIn: number;
    userInfo: {
      userId: string;
      username: string;
      nickname?: string;
      email?: string;
      phone?: string;
      avatar?: string;
      roles: string[];
      permissions: string[];
    };
  };
}

// Document-related types
export interface Document {
  id: string;
  title: string;
  content: string;
  summary?: string;
  categoryId?: string;
  teamId?: string;
  teamName?: string;
  tags?: string[];  // Optional; the backend may return a string or null
  author?: User;   // Optional; the backend may only return authorId and authorName
  status: 'draft' | 'pending_review' | 'published' | 'archived' | number;  // Supports both string and number
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
  favoriteCount?: number;
  isPublic?: boolean;
  isLiked?: boolean;
  createdAt?: string;
  updatedAt?: string;
  authorId?: string;     // The backend may only return authorId
  authorName?: string;   // The backend may only return authorName
  documentType?: number;
  isTop?: number;
  isRecommend?: number;
  publishTime?: string;
  fileSize?: number;        // File size (bytes), returned by the backend DocumentVO
  contentLength?: number;   // Content length (characters), returned by the backend DocumentVO
  autoSaveDismissed?: number; // Auto-save prompt dismissal state: null = not auto-saved, 0 = not dismissed, 1 = dismissed
}

export interface DocumentCategory {
  id: string;
  name: string;
  description?: string;
  icon?: string;
  parentId?: string;
  sort: number;
  documentCount: number;
}

export interface CategoryTree extends Omit<DocumentCategory, 'documentCount'> {
  children?: CategoryTree[];
  documentCount?: number;
  level?: number;
  sortOrder?: number;
}

export interface DocumentFilter {
  keyword?: string;
  categoryId?: string | number;  // Supports either a string or a number
  teamId?: string | number;  // Filter by team ID
  tags?: string[];
  status?: Document['status'];  // Supports either a string or a number
  authorId?: string;
  sortBy?: 'createdAt' | 'updatedAt' | 'viewCount' | 'likeCount' | 'publishTime';
  sortOrder?: 'asc' | 'desc';
  page?: number;
  pageSize?: number;
}

export interface DocumentListResponse {
  list: Document[];
  total: number;
  page: number;
  pageSize: number;
}

// Comment-related types
export interface Comment {
  id: string | number;
  documentId: string | number;
  parentId?: string | number;
  rootId?: string | number;
  content: string;
  commenterId: string | number;
  commenterName: string;
  commenterAvatar?: string;
  replyToUserId?: string | number;
  replyToUserName?: string;
  status?: number;
  likeCount: number;
  replyCount: number;
  isLiked: boolean;
  createdAt: string;
  replies?: Comment[];
}

// AI-related types
export interface AIMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: string;
  citations?: Citation[];
  fromKnowledgeBase?: boolean;
  graphContext?: GraphContext;
}

/** KAG knowledge graph context */
export interface GraphContext {
  entities?: GraphEntity[];
  paths?: GraphPath[];
  chunks?: GraphChunk[];
  hasResults?: boolean;
}

export interface GraphEntity {
  name: string;
  type: string;
  description?: string;
  connectionCount?: number;
}

export interface GraphPath {
  nodes: string[];
  relations: string[];
  hops: number;
}

export interface GraphChunk {
  chunkId: string;
  docId: EntityId;
  docTitle: string;
  content: string;
  heading?: string;
}

/** RAG citation source */
export interface Citation {
  index: number;
  documentId: EntityId;
  documentTitle: string;
  excerpt: string;
  relevanceScore: number;
}

export interface AIConversation {
  id: string | number;
  title: string;
  messages: AIMessage[];
  messageCount?: number;
  model?: string;
  status?: number;
  tokensUsed?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AIRequest {
  question: string;
  conversationId?: string;
  model?: string;
  context?: {
    documentIds?: string[];
    knowledgeBase?: boolean;
    enableKag?: boolean;
  };
}

export interface AIModelOption {
  key: string;
  displayName: string;
  description: string;
  isDefault?: boolean;
}

// Common types
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

export interface PageParams {
  page: number;
  pageSize: number;
}

export interface PageResponse<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

// Route-related types
export interface RouteItem {
  path: string;
  title: string;
  icon?: React.ReactNode;
  children?: RouteItem[];
  hidden?: boolean;
  requiresAuth?: boolean;
}

// Form-related types
export interface FormField {
  name: string;
  label: string;
  type: 'text' | 'password' | 'email' | 'textarea' | 'select' | 'checkbox' | 'radio';
  required?: boolean;
  placeholder?: string;
  options?: { label: string; value: string | number }[];
  rules?: any[];
}

export interface FormData {
  [key: string]: any;
}

// Notification types
export type NotificationType = 'success' | 'info' | 'warning' | 'error';

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
}

// Statistics-related types
export interface DashboardStats {
  overview: {
    totalDocuments: number;
    totalUsers: number;
    todayDocuments: number;
    todayUsers: number;
    totalViews: number;
    todayViews: number;
    totalLikes: number;
    totalFavorites: number;
    totalComments: number;
    pendingReviews: number;
    aiSearchCount: number;
    aiQaCount: number;
    activeUserCount: number;
  };
  documentTrend: Array<{ date: string; count: number }>;
  categoryDistribution: Array<{ name: string; value: number }>;
  hotDocuments: Array<{
    documentId: EntityId;
    title: string;
    viewCount: number;
    likeCount: number;
  }>;
  activeUsers: Array<{ name: string; count: number }>;
}

// Knowledge-graph-related types
export interface KnowledgeGraphNode {
  id: string;
  label: string;
  type: 'KnowledgeDocument' | 'KnowledgeEntity' | 'DocumentChunk';
  value?: number;
  description?: string;
  properties?: Record<string, any>;
  [key: string]: any;
}

export interface KnowledgeGraphLink {
  source: string;
  target: string;
  relation: string;
  label?: string;
  value?: number;
}

export interface KnowledgeGraphData {
  nodes: KnowledgeGraphNode[];
  links: KnowledgeGraphLink[];
}

// Search-related types
export interface ChunkResult {
  chunkId: string;
  content: string;
  heading?: string;
  score: number;
  bm25Score: number;
  vectorScore: number;
}

export interface SearchResult {
  id: string;
  title: string;
  summary?: string;
  highlights?: string[];
  categoryName?: string;
  tagNames?: string[];
  creatorName?: string;
  teamName?: string;
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
  publishAt?: string;
  score: number;
  bm25Score?: number;
  vectorScore?: number;
  rerankScore?: number;
  chunks?: ChunkResult[];
}

export interface SearchResponse {
  records: SearchResult[];
  total: number;
  current: number;
  size: number;
}

// Version-management-related types
export interface DocumentVersion {
  id: string;
  documentId: string;
  version: string;
  title: string;
  content: string;
  changeLog?: string;
  author: User;
  createdAt: string;
  isCurrent: boolean;
}

// Role-related types
export interface Role {
  id: string;
  name: string;
  code: string;
  description?: string;
  status?: number;
  permissions: string[];
  userCount: number;
  createdAt: string;
  updatedAt?: string;
}

export interface Permission {
  id: string;
  name: string;
  code: string;
  module: string;
  description?: string;
}

// Team-related types
export interface Team {
  id: string;
  teamName: string;
  name?: string; // Kept for backward compatibility with legacy data
  teamCode?: string;
  description?: string;
  icon?: string;
  leaderId?: EntityId;
  leaderName?: string;
  leader?: User;
  parentId?: EntityId;
  parentName?: string;
  level?: number;
  path?: string;
  memberCount: number;
  docCount?: number;
  status?: number;
  sort?: number;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  children?: Team[];
}

// Team-member-related types
export interface TeamMember {
  userId: string;
  username: string;
  realName?: string;
  avatar?: string;
  role: string;
  joinedAt: string;
}

// Review-related types
export interface ReviewTask {
  id: string;
  documentId: string;
  documentTitle: string;
  documentAuthor: User;
  reviewerId: string;
  reviewer?: User;
  status: 'pending' | 'approved' | 'rejected';
  comment?: string;
  reviewRound?: number;
  createdAt: string;
  reviewedAt?: string;
  categoryId?: string;
  categoryName?: string;
}

// Notification-related types
export interface SystemNotification {
  id: string;
  type: 'system' | 'comment' | 'mention' | 'review' | 'like';
  title: string;
  content: string;
  link?: string;
  documentId?: string;
  read: boolean;
  createdAt: string;
}

// AI-feedback-related types
export interface AIFeedback {
  messageId: string;
  conversationId: string;
  type: 'like' | 'dislike';
  comment?: string;
}

// Statistics-related types
export interface SystemStatistics {
  documentStats: {
    total: number;
    published: number;
    draft: number;
    todayUploads: number;
  };
  userStats: {
    total: number;
    active: number;
    newToday: number;
  };
  viewStats: {
    total: number;
    today: number;
    trend: Array<{ date: string; count: number }>;
  };
  categoryStats: Array<{ name: string; count: number }>;
}

// Collaboration-related types
export interface DocumentCollaborator {
  userId: string;
  user: User;
  role: 'owner' | 'editor' | 'viewer';
  isEditing: boolean;
  lastEditTime?: string;
}

// AI-writing-related types
export interface WritingRequest {
  topic: string;
  requirements?: string;
  contentType: 'article' | 'report' | 'documentation' | 'email' | 'announcement';
  style: 'formal' | 'casual' | 'technical' | 'creative' | 'academic';
  tone: 'neutral' | 'enthusiastic' | 'serious' | 'friendly' | 'authoritative';
  length?: number;
  existingContent?: string;
  actionType: 'generate' | 'expand' | 'optimize' | 'continue';
  templateId?: string;
  model?: string;
}

export interface WritingResult {
  content: string;
  tokens: number;
  wordCount: number;
  model: string;
}

export interface WritingTemplate {
  id: string;
  name: string;
  description: string;
  category: string;
  prompt: string;
  suggestedContentType?: string;
  suggestedStyle?: string;
}

export interface ContentTypeOption {
  value: string;
  label: string;
  description: string;
}

export interface StyleOption {
  value: string;
  label: string;
  description: string;
}

export interface ToneOption {
  value: string;
  label: string;
}

// AI quick-question types
export interface AIQuickQuestion {
  id: string;
  title: string;
  question: string;
  icon?: string;
  category?: string;
}

// AI knowledge-reference types
export interface AIKnowledgeReference {
  documentId: string;
  documentTitle: string;
  excerpt: string;
  relevance: number;
  url?: string;
}

// AI response extension types
export interface AIResponse {
  answer: string;
  conversationId: string;
  messageId: string;
  references?: AIKnowledgeReference[];
  suggestedQuestions?: string[];
}

// System-settings-related types
export interface SystemSettings {
  basic: BasicSettings;
  security: SecuritySettings;
  storage: StorageSettings;
  notification: NotificationSettings;
  ai: AISettings;
  status: SystemStatus;
}

export interface BasicSettings {
  systemName: string;
  systemDescription: string;
  systemVersion: string;
  defaultLanguage: string;
  timezone: string;
  allowRegistration: boolean;
  requireApproval: boolean;
  enableComments: boolean;
  enableAI: boolean;
  enableFullTextSearch: boolean;
}

export interface SecuritySettings {
  passwordPolicy: 'low' | 'medium' | 'high';
  sessionTimeout: number;
  enable2FA: boolean;
  ipRestriction: boolean;
  passwordMinLength: number;
  requireSpecialChar: boolean;
  loginMaxRetry: number;
}

export interface NotificationSettings {
  emailEnabled: boolean;
  emailHost: string;
  emailPort: number;
  websocketEnabled: boolean;
  notificationRetentionDays: number;
}

export interface StorageSettings {
  maxFileSize: number;
  allowedFileTypes: string;
  storageEndpoints: string;
  storageBucket: string;
}

export interface AISettings {
  aiModelName: string;
  embeddingModel: string;
  milvusHost: string;
  milvusPort: number;
}

export interface SystemStatus {
  version: string;
  runStatus: 'running' | 'stopped' | 'maintenance';
  dbStatus: 'connected' | 'disconnected';
  lastBackupTime: string;
  totalStorage: number;
  usedStorage: number;
  documentCount: number;
  userCount: number;
  startTime: string;
}

// Knowledge-graph-related types
export interface GraphData {
  nodes: GraphNode[];
  edges: GraphEdge[];
  nodeCount?: number;
  edgeCount?: number;
  timestamp?: string;
}

export interface GraphNode {
  id: string;
  name: string;
  type: 'document' | 'category' | 'tag' | 'user' | 'concept' | 'KnowledgeDocument' | 'KnowledgeEntity' | 'DocumentChunk';
  label?: string;
  labels?: string[];
  properties?: Record<string, any>;
  size?: number;
  color?: string;
  icon?: string;
  x?: number;
  y?: number;
  documentId?: string;
  userId?: string;
}

export interface GraphEdge {
  id: string;
  source: string;
  target: string;
  relationship: string;
  label?: string;
  weight?: number;
  properties?: Record<string, any>;
  color?: string;
  dashed?: boolean;
  createdAt?: string;
}

export interface GraphPathResult {
  nodes: GraphNode[];
  edges: GraphEdge[];
  pathLength: number;
  startNodeId: string;
  endNodeId: string;
}

// ==================== Foundation base service types ====================

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

/**
 * System-configuration-related types
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

/**
 * Operation-log-related types
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
