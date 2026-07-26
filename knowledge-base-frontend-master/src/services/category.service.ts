import { http } from './request';
import { DocumentCategory, CategoryTree, PageParams } from '@/types';

export interface CategoryMoveParams {
  categoryId: string;
  targetParentId?: string;
  position?: number;
}

export const categoryService = {
  // Get category list (flat)
  getCategories: () => {
    return http.get<DocumentCategory[]>('/document/categories');
  },

  // Get category tree
  getCategoryTree: () => {
    return http.get<CategoryTree[]>('/document/categories/tree');
  },

  // Get top-level categories (rootOnly, used for the sidebar)
  getRootCategories: () => {
    return http.get<CategoryTree[]>('/document/categories/children/0');
  },

  // Get category details
  getCategory: (id: string) => {
    return http.get<DocumentCategory>(`/document/categories/${id}`);
  },

  // Create category
  createCategory: (data: {
    name: string;
    description?: string;
    icon?: string;
    parentId?: string;
    sort?: number;
  }) => {
    return http.post<DocumentCategory>('/document/categories', data);
  },

  // Update category
  updateCategory: (id: string, data: Partial<DocumentCategory>) => {
    return http.put<DocumentCategory>(`/document/categories/${id}`, data);
  },

  // Delete category
  deleteCategory: (id: string) => {
    return http.delete(`/document/categories/${id}`);
  },

  // Move category
  moveCategory: (params: CategoryMoveParams) => {
    return http.post('/document/categories/move', params);
  },

  // Batch delete categories
  batchDeleteCategories: (ids: string[]) => {
    return http.delete('/document/categories/batch', { data: { ids } });
  },

  // Get documents under a category
  getCategoryDocuments: (categoryId: string, params?: PageParams) => {
    return http.get(`/document/categories/${categoryId}/documents`, { params });
  },

  // Get category statistics
  getCategoryStats: () => {
    return http.get<Array<{
      categoryId: string;
      categoryName: string;
      documentCount: number;
      viewCount: number;
    }>>('/document/categories/stats');
  },

  // Search categories
  searchCategories: (keyword: string) => {
    return http.get<DocumentCategory[]>('/document/categories/search', { params: { keyword } });
  },
};

export default categoryService;
