import dayjs from 'dayjs';
import { http } from './request';
import { SystemStatistics, DashboardStats } from '@/types';

// Compute the date range based on the period
const getDateRange = (period: 'week' | 'month' | 'year'): { startDate: string; endDate: string } => {
  const endDate = dayjs().format('YYYY-MM-DD');
  const dayMap = { week: 6, month: 29, year: 364 };
  const startDate = dayjs().subtract(dayMap[period], 'days').format('YYYY-MM-DD');
  return { startDate, endDate };
};

export const statisticsService = {
  // Get system statistics overview
  getSystemStatistics: (params?: { startDate?: string; endDate?: string }) => {
    return http.get<SystemStatistics>('/statistics/overview', { params });
  },

  // Get dashboard statistics
  getDashboardStats: () => {
    return http.get<DashboardStats>('/statistics/dashboard');
  },

  // Get admin dashboard overview
  getAdminOverview: () => {
    return http.get('/statistics/admin-overview');
  },

  // Get document trend
  getDocumentTrend: (params: { period: 'week' | 'month' | 'year' }) => {
    const { startDate, endDate } = getDateRange(params.period);
    return http.get<Array<{ date: string; count: number }>>('/statistics/trend/document', {
      params: { startDate, endDate, type: 'create' },
    });
  },

  // Get user activity
  getUserActivity: (params: { period: 'week' | 'month' | 'year' }) => {
    const { startDate, endDate } = getDateRange(params.period);
    return http.get<Array<{ date: string; count: number }>>('/statistics/activity/user', {
      params: { startDate, endDate },
    });
  },

  // Get category distribution
  getCategoryDistribution: () => {
    return http.get<Array<{ name: string; value: number }>>('/statistics/distribution/category');
  },

  // Get popular documents (composite popularity ranking)
  getPopularDocuments: (params?: { limit?: number; period?: 'week' | 'month' | 'all' }) => {
    return http.get<Array<{
      documentId: string;
      title: string;
      authorName?: string;
      categoryName?: string;
      viewCount: number;
      likeCount: number;
      favoriteCount: number;
      summary?: string;
      createdAt?: string;
    }>>(
      '/statistics/hot/document',
      { params: { type: 'composite', size: params?.limit || 6 } }
    );
  },

  // Get latest documents
  getLatestDocuments: (params?: { limit?: number }) => {
    return http.get<Array<{
      documentId: string;
      title: string;
      authorName?: string;
      categoryName?: string;
      viewCount: number;
      likeCount: number;
      favoriteCount: number;
      summary?: string;
      createdAt?: string;
    }>>(
      '/statistics/latest/documents',
      { params: { size: params?.limit || 6 } }
    );
  },

  // Get active users
  getActiveUsers: (params?: { limit?: number }) => {
    return http.get<Array<{ userId: string; username: string; contribution: number }>>(
      '/statistics/active/user',
      { params: { type: 'create', size: params?.limit || 10 } }
    );
  },
};

export default statisticsService;
