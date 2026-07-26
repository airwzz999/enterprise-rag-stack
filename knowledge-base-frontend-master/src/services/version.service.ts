import { http } from './request';
import { DocumentVersion } from '@/types';

export const versionService = {
  // Get document version list
  getVersions: (documentId: string) => {
    return http.get<DocumentVersion[]>(`/document/documents/${documentId}/versions`);
  },

  // Get version details
  getVersion: (documentId: string, versionId: string) => {
    return http.get<DocumentVersion>(`/document/documents/${documentId}/versions/${versionId}`);
  },

  // Restore to a specific version
  restoreVersion: (documentId: string, versionId: string) => {
    return http.post<DocumentVersion>(`/document/documents/${documentId}/versions/${versionId}/restore`, {});
  },

  // Compare two versions
  compareVersions: (documentId: string, versionId1: string, versionId2: string) => {
    return http.get<{ old: DocumentVersion; new: DocumentVersion; diff: string }>(
      `/document/documents/${documentId}/versions/compare`,
      { params: { v1: versionId1, v2: versionId2 } }
    );
  },

  // Create version snapshot
  createSnapshot: (documentId: string, changeLog?: string) => {
    return http.post<DocumentVersion>(`/document/documents/${documentId}/versions`, { changeLog });
  },
};

export default versionService;
