import { http } from './request';
import { GraphContext } from '@/types';

/**
 * KAG knowledge graph service
 *
 * <p>Provides knowledge graph retrieval and graph-augmented conversation capabilities.</p>
 */
export const kagService = {
  /**
   * Graph retrieval (no conversation, returns only the graph context)
   */
  searchGraph: (query: string) => {
    return http.post<GraphContext>('/kag/search', { query });
  },

  /**
   * KAG-augmented conversation
   */
  kagChat: (content: string, conversationId?: string) => {
    return http.post<{ content: string; conversationId: string; messageId: string; graphContext?: GraphContext }>(
      '/kag/chat',
      { content, conversationId, enableKag: true },
    );
  },
};

export default kagService;
