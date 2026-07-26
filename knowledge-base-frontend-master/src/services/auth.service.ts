import { http } from './request';
import { LoginRequest, LoginResponse, RegisterRequest, RegisterResponse, User } from '@/types';
import { tokenStorage } from '@/utils/token-storage';

/**
 * Authentication service
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>User login and logout</li>
 *   <li>Token management (storage, refresh, clearing)</li>
 *   <li>Fetching current user info</li>
 *   <li>User profile management</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */
export const authService = {
  /**
   * User login
   *
   * @param data Login request parameters
   * @returns Promise<LoginResponse>
   */
  login: (data: LoginRequest): Promise<LoginResponse> => {
    return http.post<LoginResponse>('/auth/auth/login', data);
  },

  /**
   * User logout
   *
   * @returns Promise<void>
   */
  logout: (): Promise<void> => {
    return http.post<void>('/auth/auth/logout').then(() => {
      // Clear local token after successful logout
      tokenStorage.clearToken();
    });
  },

  /**
   * Get current user info
   *
   * @returns Promise<User>
   */
  getCurrentUser: (): Promise<User> => {
    return http.get<User>('/auth/auth/me');
  },

  /**
   * Refresh token
   *
   * @param refreshToken Refresh token
   * @returns Promise<LoginResponse>
   */
  refreshToken: (refreshToken: string): Promise<LoginResponse> => {
    return http.post<LoginResponse>('/auth/auth/refresh', null, {
      params: { refreshToken },
      // Use skipAuth when refreshing the token to avoid an infinite loop
      skipAuth: true,
    });
  },

  /**
   * Change password
   *
   * @param data Password info
   * @returns Promise<void>
   */
  changePassword: (data: { oldPassword: string; newPassword: string }): Promise<void> => {
    return http.put<void>('/auth/users/password/change', null, {
      params: data,
    });
  },

  /**
   * Update user info
   *
   * @param data User info
   * @returns Promise<User>
   */
  updateProfile: (data: Record<string, unknown>): Promise<void> => {
    return http.put<void>('/auth/users', data);
  },

  /**
   * Upload avatar
   *
   * @param file Avatar file
   * @returns Promise<{ url: string }>
   */
  uploadAvatar: (file: File): Promise<{ url: string }> => {
    const formData = new FormData();
    formData.append('file', file);
    return http.post<{ url: string }>('/auth/avatar', formData);
  },

  /**
   * Register
   *
   * @param data Registration request parameters
   * @returns Promise<RegisterResponse> Registration response (includes email verification status)
   */
  register: (data: RegisterRequest): Promise<RegisterResponse> => {
    return http.post<RegisterResponse>('/auth/auth/register', data);
  },

  /**
   * Activate account via email verification
   *
   * @param token Activation token
   * @returns Promise<string> Activation result message
   */
  verifyEmail: (token: string): Promise<string> => {
    return http.get<string>('/auth/auth/verify-email', { params: { token } });
  },

  /**
   * Password recovery - send verification code
   *
   * @param email Email address
   * @returns Promise<void>
   */
  sendResetCode: (email: string): Promise<void> => {
    return http.post<void>('/auth/auth/password/reset/send-code', { email });
  },

  /**
   * Password recovery - verify verification code
   *
   * @param email Email address
   * @param code Verification code
   * @returns Promise<void>
   */
  verifyResetCode: (email: string, code: string): Promise<void> => {
    return http.post<void>('/auth/auth/password/reset/verify-code', { email, code });
  },

  /**
   * Password recovery - reset password
   *
   * @param data Password reset info
   * @returns Promise<void>
   */
  resetPassword: (data: { email: string; code: string; newPassword: string }): Promise<void> => {
    return http.post<void>('/auth/auth/password/reset', data);
  },

  // ===== Token management methods =====

  /**
   * Save login info
   *
   * @param loginResponse Login response data
   */
  saveLoginInfo: (loginResponse: LoginResponse): void => {
    tokenStorage.saveToken(loginResponse as unknown as import('@/utils/token-storage').LoginResponse);
  },

  /**
   * Clear login info
   */
  clearLoginInfo: (): void => {
    tokenStorage.clearToken();
  },

  /**
   * Check login status
   *
   * @returns Whether the user is logged in
   */
  isAuthenticated: (): boolean => {
    return tokenStorage.isAuthenticated();
  },

  /**
   * Get current user info (from local storage)
   *
   * @returns User info, or null if not logged in
   */
  getUserInfo: () => {
    return tokenStorage.getUserInfo();
  },

  /**
   * Get access token
   *
   * @returns Access token
   */
  getAccessToken: (): string | null => {
    return tokenStorage.getAccessToken();
  },

  /**
   * Get Authorization header
   *
   * @returns Authorization header value
   */
  getAuthorizationHeader: (): string => {
    return tokenStorage.getAuthorizationHeader();
  },
};

export default authService;
