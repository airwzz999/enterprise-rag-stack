import { http } from './request';
import { Comment } from '@/types';

export interface CommentQueryDTO {
  current?: number;
  size?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

/**
 * Comment service
 *
 * <p>Backend API path: /api/comments</p>
 * <p>Note: the comment endpoints are independent of the document endpoints and are not under /documents</p>
 */
export const commentService = {
  // Create comment
  createComment: (data: {
    documentId: string | number;
    content: string;
    parentId?: string | number;
  }) => {
    return http.post<number>('/document/comments', data);
  },

  // Delete comment
  deleteComment: (commentId: string | number) => {
    return http.delete(`/document/comments/${commentId}`);
  },

  // Like comment
  likeComment: (commentId: string | number) => {
    return http.post(`/document/comments/${commentId}/like`);
  },

  // Unlike comment
  unlikeComment: (commentId: string | number) => {
    return http.delete(`/document/comments/${commentId}/like`);
  },

  // Paginated query of document comments (note: backend uses POST request)
  pageDocumentComments: (
    documentId: string | number,
    query: CommentQueryDTO = {}
  ) => {
    const params = {
      current: query.current || 1,
      size: query.size || 10,
      sortBy: query.sortBy || 'createdAt',
      sortOrder: query.sortOrder || 'desc',
    };
    return http.post<PageResult<Comment>>(
      `/document/comments/document/${documentId}`,
      params
    );
  },

  // Get comment reply list
  getCommentReplies: (parentCommentId: string | number) => {
    return http.get<Comment[]>(`/document/comments/${parentCommentId}/replies`);
  },
};

export default commentService;
