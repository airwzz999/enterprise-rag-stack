import { http } from './request';

export interface UploadProgress {
  loaded: number;
  total: number;
  percent: number;
}

export interface UploadOptions {
  onProgress?: (progress: UploadProgress) => void;
}

export interface FileUploadResponse {
  id: string;
  originalName: string;
  fileSize: number;
  fileSizeReadable: string;
  fileType: string;
  mimeType: string;
  fileUrl: string;
  previewUrl: string;
  uploaderId: number;
  accessLevel: number;
  downloadCount: number;
  storageType: string;
  createdAt: string;
}

export const fileService = {
  // Upload a single file
  upload: (file: File, options?: UploadOptions) => {
    const formData = new FormData();
    formData.append('file', file);

    return http.post<FileUploadResponse>('/file/files/upload', formData, {
      onUploadProgress: (progressEvent) => {
        if (options?.onProgress && progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          options.onProgress({
            loaded: progressEvent.loaded,
            total: progressEvent.total,
            percent,
          });
        }
      },
    });
  },

  // Batch upload files
  batchUpload: (files: File[], options?: UploadOptions) => {
    const formData = new FormData();
    files.forEach((file) => {
      formData.append('files', file);
    });

    return http.post<{ url: string; filename: string; size: number }[]>('/files/batch-upload', formData, {
      onUploadProgress: (progressEvent) => {
        if (options?.onProgress && progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          options.onProgress({
            loaded: progressEvent.loaded,
            total: progressEvent.total,
            percent,
          });
        }
      },
    });
  },

  // Download file
  download: (fileId: string) => {
    return http.get<Blob>(`/files/${fileId}/download`, {
      responseType: 'blob',
    }).then((data) => {
      const url = window.URL.createObjectURL(new Blob([data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileId);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    });
  },

  // Get file URL
  getFileUrl: (fileId: string) => {
    return `${import.meta.env.VITE_API_BASE_URL}/files/${fileId}/view`;
  },

  // Delete file
  delete: (fileId: string) => {
    return http.delete(`/files/${fileId}`);
  },

  // Get file info
  getFileInfo: (fileId: string) => {
    return http.get<{ id: string; filename: string; size: number; mimeType: string; url: string }>(`/files/${fileId}`);
  },

  // Upload image (with preview)
  uploadImage: (file: File, options?: UploadOptions) => {
    const formData = new FormData();
    formData.append('image', file);

    return http.post<{ url: string; thumbnail: string; width: number; height: number }>('/files/images', formData, {
      onUploadProgress: (progressEvent) => {
        if (options?.onProgress && progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          options.onProgress({
            loaded: progressEvent.loaded,
            total: progressEvent.total,
            percent,
          });
        }
      },
    });
  },

  // Upload image from URL (automatically downloads and uploads it)
  uploadFromUrl: (imageUrl: string) => {
    return http.post<{ originalUrl: string; convertedUrl: string }>('/document/files/upload-from-url', null, {
      params: { imageUrl },
      timeout: 60000,
    });
  },

  // Batch convert image URLs
  batchConvertUrls: (imageUrls: string[]) => {
    return http.post<{
      urlMappings: Record<string, string>;
      errorMappings: Record<string, string>;
      successCount: number;
      failureCount: number;
    }>('/document/files/batch-convert', imageUrls, {
      timeout: 120000,
    });
  },

  // Import document (supports multiple formats)
  importDocument: (file: File, options?: { categoryId?: string; tags?: string[] }) => {
    const formData = new FormData();
    formData.append('file', file);
    if (options?.categoryId) {
      formData.append('categoryId', options.categoryId);
    }
    if (options?.tags) {
      formData.append('tags', JSON.stringify(options.tags));
    }

    return http.post<{ documentId: string; title: string; content: string }>('/document/documents/import', formData);
  },

  // Export document
  exportDocument: (documentId: string, format: 'pdf' | 'word' | 'markdown' | 'html') => {
    return http.get<Blob>(`/document/documents/${documentId}/export`, {
      params: { format },
      responseType: 'blob',
    }).then((data) => {
      const url = window.URL.createObjectURL(new Blob([data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `document.${format}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    });
  },

  // Get file preview
  preview: (fileId: string) => {
    return http.get<{ content: string; format: string }>(`/files/${fileId}/preview`);
  },

  // Get file list
  getFileList: (params?: {
    page?: number;
    pageSize?: number;
    fileType?: string;
    keyword?: string;
  }) => {
    return http.get<{
      list: FileUploadResponse[];
      total: number;
      page: number;
      pageSize: number;
    }>('/file/files', { params });
  },

  // Get file details
  getFileDetail: (fileId: string) => {
    return http.get<FileUploadResponse>(`/file/files/${fileId}`);
  },
};

export default fileService;
