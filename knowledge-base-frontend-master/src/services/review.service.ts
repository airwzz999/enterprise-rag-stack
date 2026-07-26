import { http } from './request';
import { EntityId, ReviewTask, User } from '@/types';

/**
 * Raw review record format returned by the backend
 */
interface BackendReviewRecord {
  id: EntityId;
  documentId: EntityId;
  documentTitle: string;
  authorId: EntityId | null;
  authorName: string | null;
  reviewerId: EntityId | null;
  reviewerName: string | null;
  reviewResult: number | null;  // null=pending, 1=approved, 2=rejected
  reviewComment: string | null;
  beforeStatus: number;
  reviewedAt: string | null;
  reviewRound: number;
  createdAt: string;
  categoryId: EntityId | null;
  categoryName: string | null;
}

interface BackendPageResult {
  records: BackendReviewRecord[];
  total: number;
  current: number;
  size: number;
}

/**
 * Map a backend review record to the frontend ReviewTask format
 */
function mapToReviewTask(record: BackendReviewRecord): ReviewTask {
  // reviewResult: null→pending, 1→approved, 2→rejected
  let status: ReviewTask['status'] = 'pending';
  if (record.reviewResult === 1) status = 'approved';
  else if (record.reviewResult === 2) status = 'rejected';

  return {
    id: String(record.id),
    documentId: String(record.documentId),
    documentTitle: record.documentTitle,
    documentAuthor: record.authorName
      ? { id: String(record.authorId || ''), username: record.authorName, email: '', status: 'active' }
      : ({ id: '', username: 'Unknown', email: '', status: 'active' } as User),
    reviewerId: record.reviewerId ? String(record.reviewerId) : '',
    reviewer: record.reviewerName
      ? { id: String(record.reviewerId || ''), username: record.reviewerName, email: '', status: 'active' }
      : undefined,
    status,
    reviewRound: record.reviewRound,
    comment: record.reviewComment || undefined,
    createdAt: record.createdAt,
    reviewedAt: record.reviewedAt || undefined,
    categoryId: record.categoryId != null ? String(record.categoryId) : undefined,
    categoryName: record.categoryName || undefined,
  };
}

export const reviewService = {
  // Get review task list
  getReviewTasks: async (params?: { status?: string; page?: number; pageSize?: number; authorId?: string; keyword?: string }) => {
    const response = await http.get<BackendPageResult>('/document/review/tasks', { params });
    return {
      list: (response as unknown as BackendPageResult).records.map(mapToReviewTask),
      total: (response as unknown as BackendPageResult).total,
    };
  },

  // Get my rejected documents (failed review)
  getMyRejectedDocuments: async (params: { authorId: string; page?: number; pageSize?: number }) => {
    const requestParams: Record<string, any> = {
      status: 'rejected',
      authorId: params.authorId,
      page: params.page || 1,
      pageSize: params.pageSize || 12,
    };
    const response = await http.get<BackendPageResult>('/document/review/tasks', { params: requestParams });
    return {
      list: (response as unknown as BackendPageResult).records.map(mapToReviewTask),
      total: (response as unknown as BackendPageResult).total,
    };
  },

  // Get pending review task count
  getPendingCount: () => {
    return http.get<number>('/document/review/tasks/pending-count');
  },

  // Get the current review task for a document
  getCurrentReviewTask: async (documentId: string) => {
    const response = await http.get<BackendReviewRecord>(`/document/review/documents/${documentId}/current`);
    return mapToReviewTask(response as unknown as BackendReviewRecord);
  },

  // Get review statistics
  getReviewStats: () => {
    return http.get<{ pending: number; approved: number; rejected: number }>('/document/review/tasks/stats');
  },

  // Review a document (approve or reject)
  reviewDocument: (taskId: string, data: { status: 'approved' | 'rejected'; comment?: string }) => {
    return http.post(`/document/review/tasks/${taskId}/review`, data);
  },

  // Batch review
  batchReview: (taskIds: string[], data: { status: 'approved' | 'rejected'; comment?: string }) => {
    return http.post('/document/review/tasks/batch-review', { taskIds, ...data });
  },

  // Submit document for review
  submitForReview: (documentId: string) => {
    return http.post(`/document/review/submit/${documentId}`);
  },

  // Get review history
  getReviewHistory: async (documentId: string) => {
    const response = await http.get<BackendReviewRecord[]>(`/document/review/documents/${documentId}/history`);
    const records = response as unknown as BackendReviewRecord[];
    return records.map(mapToReviewTask);
  },
};

export default reviewService;
