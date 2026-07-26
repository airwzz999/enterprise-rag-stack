import { http } from './request';
import { Team, TeamMember } from '@/types';

export const teamService = {
  // Paginated query of teams
  getTeams: (params?: { current?: number; size?: number; teamName?: string; status?: number }) => {
    return http.post<{
      current: number;
      size: number;
      total: number;
      pages: number;
      records: Team[];
    }>('/auth/teams/page', params || {});
  },

  // Get team tree
  getTeamTree: (rootOnly = false) => {
    return http.get<Team[]>('/auth/teams/tree', { params: { rootOnly }, skipAuth: true } as any);
  },

  // Get team details
  getTeam: (id: string) => {
    return http.get<Team>(`/auth/teams/${id}`);
  },

  // Create team
  createTeam: (data: { teamName: string; teamCode: string; description?: string; icon?: string; leaderId: string; parentId?: string }) => {
    return http.post<number>('/auth/teams', data);
  },

  // Update team
  updateTeam: (data: { id: string; teamName?: string; teamCode?: string; description?: string; icon?: string; leaderId?: string; status?: number }) => {
    return http.put<boolean>('/auth/teams', data);
  },

  // Delete team
  deleteTeam: (id: string) => {
    return http.delete<boolean>(`/auth/teams/${id}`);
  },

  // Add team members (batch)
  addMembers: (teamId: string, userIds: string[]) => {
    return http.post<boolean>(`/auth/teams/${teamId}/members`, userIds);
  },

  // Remove team members (batch)
  removeMembers: (teamId: string, userIds: string[]) => {
    return http.delete<boolean>(`/auth/teams/${teamId}/members`, { data: userIds } as any);
  },

  // Get team member list
  getTeamMembers: (teamId: string) => {
    return http.get<TeamMember[]>(`/auth/teams/${teamId}/members`);
  },
};

export default teamService;
