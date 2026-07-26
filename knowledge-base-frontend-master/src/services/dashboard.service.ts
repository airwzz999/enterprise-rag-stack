import { http } from './request';
import { DashboardStats } from '@/types';

export const dashboardService = {
  // Get dashboard statistics (connects to kb-statistics /statistics/dashboard)
  getStats: () => {
    return http.get<DashboardStats>('/statistics/dashboard');
  },
};

export default dashboardService;
