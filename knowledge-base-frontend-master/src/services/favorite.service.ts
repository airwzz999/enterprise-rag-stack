import { http } from './request';

/**
 * User favorites service
 *
 * @author Top-tier Company Standard
 * @since 1.0.0
 */
export const favoriteService = {
  /**
   * Toggle favorite status
   * @param documentId Document ID
   * @returns Favorite status
   */
  toggleFavorite: (documentId: string) => {
    return http.post<boolean>(`/document/favorite/toggle/${documentId}`);
  },

  /**
   * Add favorite
   * @param documentId Document ID
   * @returns Whether the operation succeeded
   */
  addFavorite: (documentId: string) => {
    return http.post<boolean>(`/document/favorite/add/${documentId}`);
  },

  /**
   * Remove favorite
   * @param documentId Document ID
   * @returns Whether the operation succeeded
   */
  removeFavorite: (documentId: string) => {
    return http.delete<boolean>(`/document/favorite/remove/${documentId}`);
  },

  /**
   * Check whether a document is favorited
   * @param documentId Document ID
   * @returns Whether it is favorited
   */
  checkFavorite: (documentId: string) => {
    return http.get<boolean>(`/document/favorite/check/${documentId}`);
  },

  /**
   * Get the user's favorite list
   * @returns Favorite list
   */
  getFavorites: () => {
    return http.get<any[]>('/document/favorite/list');
  },

  /**
   * Get the favorite count for a document
   * @param documentId Document ID
   * @returns Favorite count
   */
  getFavoriteCount: (documentId: string) => {
    return http.get<number>(`/document/favorite/count/${documentId}`);
  },
};

export default favoriteService;
