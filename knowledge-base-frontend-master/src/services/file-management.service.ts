import { http } from './request';
import { EntityId } from '@/types';

/**
 * File metadata interface
 */
export interface FileMetadata {
  id: EntityId;
  fileName: string;
  originalFileName: string;
  fileExtension: string;
  fileSize: number;
  fileSizeReadable: string;
  contentType: string;
  accessUrl: string;
  fileCategory: string;
  uploaderId: EntityId;
  uploaderName: string;
  isPublic: boolean;
  downloadCount: number;
  createdAt: string;
  updatedAt: string;
  lastAccessTime: string;
  width?: number;
  height?: number;
  thumbnailUrl?: string;
  /** Duration (seconds), for audio/video files */
  duration?: number;
  /** Resolution, e.g. "1920x1080" */
  resolution?: string;
  /** Bitrate (kbps) */
  bitrate?: number;
  /** Transcode status: PENDING/PROCESSING/DONE/FAILED */
  transcodeStatus?: string;
  /** HLS playback URL */
  playUrl?: string;
}

/**
 * File statistics interface
 */
export interface FileStatistics {
  totalCount: number;
  totalSize: number;
  totalSizeReadable: string;
  categoryCount: Record<string, number>;
  todayCount: number;
}

/**
 * File management service
 */
export const fileManagementService = {
  /**
   * Upload file
   */
  uploadFile: async (file: File, isPublic: boolean = false): Promise<FileMetadata> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('isPublic', String(isPublic));

    const response = await (http as any).postForm('/document/file-management/upload', formData);

    return response;
  },

  /**
   * Get file list
   */
  getFileList: async (): Promise<FileMetadata[]> => {
    const response = await http.get('/document/file-management/list');
    return response;
  },

  /**
   * Get file list by category
   */
  getFileListByCategory: async (category: string): Promise<FileMetadata[]> => {
    const response = await http.get(`/document/file-management/list/${category}`);
    return response;
  },

  /**
   * Get file details
   */
  getFileDetail: async (fileId: EntityId): Promise<FileMetadata> => {
    const response = await http.get(`/document/file-management/detail/${fileId}`);
    return response;
  },

  /**
   * Rename file
   */
  renameFile: async (fileId: EntityId, newFileName: string): Promise<boolean> => {
    const response = await http.put(`/document/file-management/rename/${fileId}`, null, {
      params: { newFileName },
    });
    return response;
  },

  /**
   * Delete file
   */
  deleteFile: async (fileId: EntityId): Promise<boolean> => {
    const response = await http.delete(`/document/file-management/delete/${fileId}`);
    return response;
  },

  /**
   * Batch delete files
   */
  batchDeleteFiles: async (fileIds: EntityId[]): Promise<number> => {
    const response = await http.delete('/document/file-management/batch-delete', {
      data: fileIds,
    });
    return response;
  },

  /**
   * Update file permission
   */
  updateFilePermission: async (fileId: EntityId, isPublic: boolean): Promise<boolean> => {
    const response = await http.put(`/document/file-management/permission/${fileId}`, null, {
      params: { isPublic },
    });
    return response;
  },

  /**
   * Download file
   */
  downloadFile: async (fileId: EntityId): Promise<boolean> => {
    const response = await http.post(`/document/file-management/download/${fileId}`);
    return response;
  },

  /**
   * Get file statistics
   */
  getFileStatistics: async (): Promise<FileStatistics> => {
    const response = await http.get('/document/file-management/statistics');
    return response;
  },

  /**
   * Copy file
   */
  copyFile: async (fileId: EntityId): Promise<FileMetadata> => {
    const response = await http.post(`/document/file-management/copy/${fileId}`);
    return response;
  },

  /**
   * Search files
   */
  searchFiles: async (keyword: string): Promise<FileMetadata[]> => {
    const response = await http.get('/document/file-management/search', {
      params: { keyword },
    });
    return response;
  },

  /**
   * Get the HLS playback URL (kb-file service)
   * Note: this URL is requested directly by ReactPlayer, not via axios, so it needs the full path including the /api prefix
   */
  getStreamUrl: (fileId: EntityId): string => {
    return `/api/files/stream/${fileId}/master.m3u8`;
  },

  /**
   * Get the media stream URL (kb-document proxy, for direct audio/video playback)
   * Note: this URL is requested directly by ReactPlayer, not via axios, so it needs the full path including the /api prefix
   */
  getMediaStreamUrl: (fileId: EntityId): string => {
    return `/api/document/file-management/stream/${fileId}`;
  },

  /**
   * Get thumbnail URL
   */
  getThumbnailUrl: (fileId: EntityId): string => {
    return `/api/files/thumbnail/${fileId}`;
  },

  /**
   * Get PPTX slide images (Base64 PNG)
   */
  getPptxSlideImages: async (fileId: EntityId): Promise<string[]> => {
    const response = await http.get(`/document/file-management/preview/${fileId}/slides`);
    return response;
  },
};

export default fileManagementService;
