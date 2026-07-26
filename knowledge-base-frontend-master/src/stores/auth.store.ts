import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { User } from '@/types';
import { authService } from '@/services';
import { tokenStorage } from '@/utils/token-storage';
import type { LoginResponse } from '@/utils/token-storage';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  // Actions
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  updateUser: (user: User) => void;
  checkAuth: () => Promise<void>;
  clearAuth: () => void;
}

const normalizeLoginUser = (userInfo: any): User => ({
  id: userInfo?.id ?? userInfo?.userId,
  username: userInfo?.username || '',
  nickname: userInfo?.nickname,
  email: userInfo?.email || '',
  phone: userInfo?.phone,
  avatar: userInfo?.avatar,
  role: userInfo?.role || userInfo?.roles?.[0]?.replace(/^ROLE_/, '') || undefined,
  roles: userInfo?.roles || [],
  permissions: userInfo?.permissions || [],
  status: userInfo?.status ?? 1,
});

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,

      login: async (username: string, password: string) => {
        set({ isLoading: true });
        try {
          // Call the login endpoint
          const loginResponse = await authService.login({ username, password }) as unknown as LoginResponse;

          console.log('🔐 Login response:', loginResponse);

          // Save the token to cookies and local storage
          tokenStorage.saveToken(loginResponse);

          // Update state
          set({
            user: normalizeLoginUser(loginResponse.userInfo),
            token: loginResponse.accessToken,
            isAuthenticated: true,
            isLoading: false,
          });

          console.log('✅ Login successful, token saved');
        } catch (error) {
          set({ isLoading: false });
          throw error;
        }
      },

      logout: async () => {
        try {
          await authService.logout();
        } finally {
          get().clearAuth();
        }
      },

      updateUser: (user: User) => {
        set({ user });
      },

      checkAuth: async () => {
        // Prefer getting the token from cookies
        const token = tokenStorage.getAccessToken() || get().token || localStorage.getItem('token');
        const storedUserInfo = tokenStorage.getUserInfo();

        if (!token) {
          set({ isAuthenticated: false, user: null, token: null });
          return;
        }

        set({ isLoading: true, token });
        try {
          const currentUser = await authService.getCurrentUser();
          const user = normalizeLoginUser({
            ...storedUserInfo,
            ...currentUser,
            roles: currentUser?.roles ?? storedUserInfo?.roles,
            permissions: currentUser?.permissions ?? storedUserInfo?.permissions,
          });

          tokenStorage.saveUserInfo({
            userId: user.id,
            username: user.username,
            nickname: user.nickname,
            email: user.email,
            phone: user.phone,
            avatar: user.avatar,
            role: user.role,
            roles: user.roles,
            permissions: user.permissions,
            status: typeof user.status === 'number' ? user.status : 1,
          });

          set({
            user,
            isAuthenticated: true,
            isLoading: false,
          });
        } catch (error) {
          get().clearAuth();
          set({ isLoading: false });
        }
      },

      clearAuth: () => {
        // Clear the token
        tokenStorage.clearToken();

        set({
          user: null,
          token: null,
          isAuthenticated: false,
        });

        // Clear the legacy token in localStorage and the zustand-persisted data
        localStorage.removeItem('token');
        localStorage.removeItem('auth-storage');
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
