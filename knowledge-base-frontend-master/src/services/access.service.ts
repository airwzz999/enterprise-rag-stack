import { http } from './request';

/**
 * Document access record service
 *
 * @author Top-tier Company Standard
 * @since 1.0.0
 */
export const accessService = {
  /**
   * Record a document access
   * @param documentId Document ID
   * @param documentTitle Document title
   * @returns Whether the operation succeeded
   */
  recordAccess: (documentId: string, documentTitle: string) => {
    return http.post<boolean>('/document/access/record', {
      documentId,
      documentTitle,
    });
  },

  /**
   * Get the user's recent access records
   * @param limit Limit on the number of records to query
   * @returns List of access records
   */
  getRecentAccess: (limit?: number) => {
    const params = limit ? { limit } : {};
    return http.get<any[]>('/document/access/recent', { params });
  },

  /**
   * Delete a single access record
   * @param documentId Document ID
   * @returns Whether the operation succeeded
   */
  deleteAccess: (documentId: string) => {
    return http.delete<boolean>(`/document/access/remove/${documentId}`);
  },

  /**
   * Clear all of the user's access records
   * @returns Whether the operation succeeded
   */
  clearAllAccess: () => {
    return http.delete<boolean>('/document/access/clear');
  },
};

export default accessService;
