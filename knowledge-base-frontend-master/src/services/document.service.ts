import { http } from './request';
import request from './request';
import { Document, DocumentFilter } from '@/types';

export const documentService = {
  // Get document list (paginated)
  // Backend returns IPage<DocumentVO> structure: { records, total, current, size }
  // Frontend expects DocumentListResponse structure: { list, total, page, pageSize }
  getDocuments: (filter: DocumentFilter) => {
    // Convert frontend parameter names to the names expected by the backend
    const params: Record<string, any> = {
      current: filter.page || 1,
      size: filter.pageSize || 10,
    };

    // Only add parameters supported by the backend
    if (filter.categoryId) params.categoryId = filter.categoryId;
    if (filter.teamId) params.teamId = filter.teamId;
    if (filter.keyword) params.keyword = filter.keyword;
    if (filter.status !== undefined) params.status = filter.status;
    if (filter.authorId) params.authorId = filter.authorId;

    // Add sort parameters
    if (filter.sortBy) params.sortBy = filter.sortBy;
    if (filter.sortOrder) params.sortOrder = filter.sortOrder;

    return http.get<any>('/document/documents/page', { params }).then((data) => ({
      list: data.records || [],
      total: data.total || 0,
      page: data.current || 1,
      pageSize: data.size || 10,
    }));
  },

  // Get document details
  getDocument: (id: string) => {
    return http.get<Document>(`/document/documents/${id}`);
  },

  // Get the previous and next document
  getDocumentNeighbors: (id: string) => {
    return http.get<{ prevId: string | null; prevTitle: string | null; nextId: string | null; nextTitle: string | null }>(`/document/documents/${id}/neighbors`);
  },

  // Create document
  // Backend returns Result<Long>, i.e. the document ID
  createDocument: (data: any) => {
    return http.post<{ id: string }>('document/documents', data);
  },

  // Update document (note: backend endpoint is PUT /documents, with id in the request body)
  updateDocument: (id: string, data: Partial<Document>) => {
    return http.put<Document>('/document/documents', { ...data, id });
  },

  // Auto-save document (create or update a draft, allows empty title, does not trigger indexing)
  autoSaveDocument: (data: {
    id?: number | string;
    title?: string;
    content?: string;
    summary?: string;
    categoryId?: number | string;
    teamId?: number | string;
    tags?: string;
  }) => {
    return http.post<{ id: string }>('/document/documents/autosave', data);
  },

  // Update only the document summary (uses a dedicated PATCH endpoint, skips full validation)
  updateSummary: (id: string, summary: string) => {
    return http.patch<boolean>(`/document/documents/${id}/summary`, { summary });
  },

  // Delete document
  deleteDocument: (id: string) => {
    return http.delete(`/document/documents/${id}`);
  },

  // Publish document (note: backend endpoint is PUT /documents/{id}/publish)
  publishDocument: (id: string) => {
    return http.put<Document>(`/document/documents/${id}/publish`);
  },

  // Archive document (note: backend endpoint is PUT /documents/{id}/archive)
  archiveDocument: (id: string) => {
    return http.put<Document>(`/document/documents/${id}/archive`);
  },

  // Like document
  likeDocument: (id: string) => {
    return http.post(`/document/documents/${id}/like`);
  },

  // Unlike document
  unlikeDocument: (id: string) => {
    return http.delete(`/document/documents/${id}/like`);
  },

  // Favorite document
  favoriteDocument: (id: string) => {
    return http.post(`/document/documents/${id}/favorite`);
  },

  // View document (increments view count)
  viewDocument: (id: string) => {
    return http.get<Document>(`/document/documents/${id}/view`);
  },

  // Note: category-related methods have been moved to categoryService, use categoryService instead

  // Upload document file
  uploadDocumentFile: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    // Use the postForm method to handle file upload
    return (http as any).postForm<string>('/document/documents/upload', formData);
  },

  // Upload a file and parse it to create a document
  uploadAndParseDocument: (file: File): Promise<{
    documentId: string;
    title: string;
    fileUrl: string;
    fileSize: number;
    contentLength: number;
    contentPreview: string;
  }> => {
    const formData = new FormData();
    formData.append('file', file);
    return (http as any).postForm('/document/documents/upload/parse', formData);
  },

  // Export document to PDF (get download link)
  exportDocumentToPdf: (documentId: string) => {
    return http.get<string>(`/document/documents/${documentId}/export-pdf`);
  },

  // Download document PDF (direct download)
  downloadDocumentPdf: (documentId: string) => {
    return (http as any).download(`/document/documents/${documentId}/download-pdf`);
  },

  // Batch export documents
  batchExportDocuments: async (documentIds: string[], format: 'pdf' | 'markdown') => {
    const response: any = await request.post('/document/documents/batch-export', { documentIds, format }, {
      responseType: 'blob',
      _download: true,
    } as any);

    // Check whether the response is a JSON error (rather than a ZIP file)
    const contentType = response.headers?.['content-type'] || '';
    if (contentType.includes('application/json')) {
      // Read the error message
      const text = await response.data.text();
      const errorData = JSON.parse(text);
      throw new Error(errorData.message || 'Export failed');
    }

    const contentDisposition = response.headers?.['content-disposition'] || '';
    let filename = `documents_export_${new Date().toISOString().slice(0, 10)}.zip`;
    const match = contentDisposition.match(/filename\*=UTF-8''([^"';]+)/);
    if (match) {
      try { filename = decodeURIComponent(match[1]); } catch { filename = match[1]; }
    }
    const blob = new Blob([response.data], { type: 'application/zip' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(link.href);
  },

  // Create share link
  createShare: (data: {
    documentId: string;
    shareType?: number;
    expireType?: number;
    expireTime?: string;
    accessLimit?: number;
    requirePassword?: number;
    password?: string;
    description?: string;
  }) => {
    return http.post<ShareVO>('/document/documents/share', data);
  },

  // Get share info
  getShareInfo: (shareId: string) => {
    return http.get<ShareVO>(`/document/documents/share/${shareId}`);
  },

  // Access share link
  accessShare: (shareId: string, password?: string) => {
    const params = password ? { password } : {};
    return http.post<number>(`/document/documents/share/${shareId}/access`, null, { params });
  },

  // Get all shares of a document
  getDocumentShares: (documentId: string) => {
    return http.get<ShareVO[]>(`/document/documents/${documentId}/shares`);
  },

  // Get my share list
  getMyShares: () => {
    return http.get<ShareVO[]>('/document/documents/share/my');
  },

  // Delete share link
  deleteShare: (shareId: string) => {
    return http.delete(`/document/documents/share/${shareId}`);
  },

  // Update share settings
  updateShare: (shareId: string, data: any) => {
    return http.put(`/document/documents/share/${shareId}`, data);
  },

  // ========== Public share endpoints (no login required, skipAuth) ==========

  // Publicly get share info
  getPublicShareInfo: (shareId: string) => {
    return http.get<ShareVO>(`/document/share/${shareId}`, { skipAuth: true } as any);
  },

  // Publicly verify share password
  verifyPublicShare: (shareId: string, password?: string) => {
    const params: Record<string, string> = {};
    if (password) params.password = password;
    return http.post<boolean>(`/document/share/${shareId}/verify`, null, {
      params,
      skipAuth: true,
    } as any);
  },

  // Publicly access share (get document content)
  accessPublicShare: (shareId: string, password?: string) => {
    const params: Record<string, string> = {};
    if (password) params.password = password;
    return http.post<any>(`/document/share/${shareId}/access`, null, {
      params,
      skipAuth: true,
    } as any);
  },

  // ========== Auto-save history ==========

  // Get the auto-save history snapshot list of a document
  getAutoSaveHistory: (documentId: string, page = 1, pageSize = 20) => {
    return http.get<any>(`/document/documents/${documentId}/autosave-history`, {
      params: { current: page, size: pageSize },
    }).then((data: any) => ({
      list: data.records || [],
      total: data.total || 0,
      page: data.current || 1,
      pageSize: data.size || 20,
    }));
  },

  // Get a single auto-save snapshot's details (including full content)
  getAutoSaveSnapshot: (documentId: string, snapshotId: string) => {
    return http.get<any>(`/document/documents/${documentId}/autosave-history/${snapshotId}`);
  },

  // Dismiss auto-saved drafts (marks all of the current user's drafts as acknowledged, so the restore prompt no longer appears)
  dismissAutoSaveDrafts: () => {
    return http.put<boolean>('/document/documents/autosave/dismiss');
  },
};

export interface ShareVO {
  shareId: string;
  shareUrl: string;
  documentId: string | number;
  title: string;
  shareType: number;
  shareTypeDesc: string;
  expireType: number;
  expireTime: string;
  expired: boolean;
  requirePassword: boolean;
  sharerName: string;
  shareTime: string;
  accessCount: number;
  description: string;
}

export default documentService;
