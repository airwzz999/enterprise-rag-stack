import { EntityId } from '@/types';
import { http } from './request';
import { SearchResponse } from '@/types';

export interface SearchParams {
  keyword: string;
  searchMode?: 'keyword' | 'hybrid';
  topK?: number;
  enableRerank?: boolean;
  page?: number;
  pageSize?: number;
  sortBy?: 'relevance' | 'time' | 'views';
}

export interface SearchSuggestVO {
  text: string;
  type: string;
  documentId: EntityId;
}

export interface SearchHistoryVO {
  id: EntityId;
  keyword: string;
  count: number;
  createdAt?: string;
}

export const searchService = {
  // Smart search (keyword / hybrid)
  search: (params: SearchParams) => {
    return http.post<SearchResponse>('/search', {
      keyword: params.keyword,
      searchMode: params.searchMode || 'keyword',
      topK: params.topK || 10,
      enableRerank: params.enableRerank ?? true,
      current: params.page || 1,
      size: params.pageSize || 10,
      sortField: params.sortBy,
    });
  },

  // Search suggestions (autocomplete)
  suggestions: (keyword: string) => {
    return http.get<SearchSuggestVO[]>('/search/suggest', {
      params: { keyword, size: 8 },
    });
  },

  // Hot searches
  hotSearch: () => {
    return http.get<string[]>('/search/hot');
  },

  // Search history
  history: () => {
    return http.get<SearchHistoryVO[]>('/search/history');
  },

  // Clear search history
  clearHistory: () => {
    return http.delete('/search/history');
  },
};

export default searchService;
