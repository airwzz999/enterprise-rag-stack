/**
 * Token storage service
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Manages storage and retrieval of the JWT token</li>
 *   <li>Supports both cookie and localStorage storage</li>
 *   <li>Cookie storage: used so HTTP requests carry it automatically</li>
 *   <li>LocalStorage storage: used by frontend business logic</li>
 *   <li>Provides token expiration checks and auto-refresh</li>
 * </ul>
 *
 * <p>Storage strategy:</p>
 * <ul>
 *   <li>accessToken: cookie (HttpOnly) + localStorage (for business logic)</li>
 *   <li>refreshToken: cookie (HttpOnly)</li>
 *   <li>userInfo: localStorage</li>
 * </ul>
 *
 * @since 1.0.0
 */

import type { EntityId } from '@/types';
import { cookieManager } from './cookie';

/**
 * Token info interface
 */
export interface TokenInfo {
  /** Access token */
  accessToken: string;
  /** Refresh token */
  refreshToken: string;
  /** Token type (usually 'Bearer') */
  tokenType: string;
  /** Expiration duration (in seconds) */
  expiresIn: number;
  /** Expiration timestamp */
  expiresAt: number;
}

/**
 * User info interface
 */
export interface UserInfo {
  userId: EntityId;
  username: string;
  nickname?: string;
  email?: string;
  phone?: string | null;
  avatar?: string;
  role?: string;
  roles?: string[];
  permissions?: string[];
  status?: number;
}

/**
 * Login response interface
 */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userInfo: UserInfo;
}

/**
 * Token storage key name constants
 */
const TOKEN_KEYS = {
  ACCESS_TOKEN: 'access_token',
  REFRESH_TOKEN: 'refresh_token',
  TOKEN_TYPE: 'token_type',
  EXPIRES_AT: 'expires_at',
  USER_INFO: 'user_info',
} as const;

/**
 * Token storage service class
 */
class TokenStorageService {
  /**
   * Saves token information
   *
   * <p>Saved to both cookie and localStorage:</p>
   * <ul>
   *   <li>Cookie: primary storage, read back and added to the request header</li>
   *   <li>LocalStorage: secondary storage, used by frontend business logic</li>
   * </ul>
   *
   * @param loginResponse Login response data
   */
  saveToken(loginResponse: LoginResponse): void {
    const { accessToken, refreshToken, tokenType, expiresIn, userInfo } = loginResponse;

    // Compute the expiration timestamp
    const expiresAt = Date.now() + expiresIn * 1000;

    // Save to localStorage (secondary)
    localStorage.setItem(TOKEN_KEYS.ACCESS_TOKEN, accessToken);
    localStorage.setItem(TOKEN_KEYS.REFRESH_TOKEN, refreshToken);
    localStorage.setItem(TOKEN_KEYS.TOKEN_TYPE, tokenType);
    localStorage.setItem(TOKEN_KEYS.EXPIRES_AT, String(expiresAt));
    localStorage.setItem(TOKEN_KEYS.USER_INFO, JSON.stringify(userInfo));

    // Save to cookie (primary storage)
    // Used to store the token; the frontend reads it from the cookie and adds it to the Authorization header
    const cookieOptions = {
      expires: expiresIn / 86400, // Convert to days
      path: '/',
      sameSite: 'lax' as const,
    };

    cookieManager.set(TOKEN_KEYS.ACCESS_TOKEN, accessToken, cookieOptions);
    cookieManager.set(TOKEN_KEYS.REFRESH_TOKEN, refreshToken, cookieOptions);

    console.log('✅ Token saved to cookie:', accessToken.substring(0, 50) + '...');
  }

  /**
   * Gets the access token
   *
   * <p>Prefers reading the token from the cookie, then falls back to localStorage</p>
   *
   * @returns The access token, or null if it doesn't exist
   */
  getAccessToken(): string | null {
    // Prefer the cookie (primary storage)
    const tokenFromCookie = cookieManager.get(TOKEN_KEYS.ACCESS_TOKEN);
    if (tokenFromCookie) {
      return tokenFromCookie;
    }

    // Fall back to localStorage (secondary storage)
    return localStorage.getItem(TOKEN_KEYS.ACCESS_TOKEN);
  }

  /**
   * Gets the refresh token
   *
   * @returns The refresh token, or null if it doesn't exist
   */
  getRefreshToken(): string | null {
    // Prefer the cookie
    const tokenFromCookie = cookieManager.get(TOKEN_KEYS.REFRESH_TOKEN);
    if (tokenFromCookie) {
      return tokenFromCookie;
    }

    // Fall back to localStorage
    return localStorage.getItem(TOKEN_KEYS.REFRESH_TOKEN);
  }

  /**
   * Gets the token type
   *
   * @returns The token type, defaults to 'Bearer'
   */
  getTokenType(): string {
    return localStorage.getItem(TOKEN_KEYS.TOKEN_TYPE) || 'Bearer';
  }

  /**
   * Gets the full Authorization header value
   *
   * @returns The Authorization header value (e.g. 'Bearer xxx')
   */
  getAuthorizationHeader(): string {
    const token = this.getAccessToken();
    const tokenType = this.getTokenType();
    return token ? `${tokenType} ${token}` : '';
  }

  /**
   * Checks whether the token is expiring
   *
   * @param bufferTime Lead time in seconds, defaults to 300 (5 minutes)
   * @returns Whether the token is about to expire or has already expired
   */
  isTokenExpiring(bufferTime: number = 300): boolean {
    const expiresAt = localStorage.getItem(TOKEN_KEYS.EXPIRES_AT);
    if (!expiresAt) {
      return true;
    }

    const now = Date.now();
    const expiresAtNum = parseInt(expiresAt, 10);
    const bufferTimeMs = bufferTime * 1000;

    return now >= (expiresAtNum - bufferTimeMs);
  }

  /**
   * Checks whether the token has expired
   *
   * @returns Whether it has expired
   */
  isTokenExpired(): boolean {
    const expiresAt = localStorage.getItem(TOKEN_KEYS.EXPIRES_AT);
    if (!expiresAt) {
      return true;
    }

    return Date.now() >= parseInt(expiresAt, 10);
  }

  /**
   * Gets the user info
   *
   * @returns The user info, or null if it doesn't exist
   */
  getUserInfo(): UserInfo | null {
    const userInfoStr = localStorage.getItem(TOKEN_KEYS.USER_INFO);
    if (!userInfoStr) {
      return null;
    }

    try {
      return JSON.parse(userInfoStr) as UserInfo;
    } catch {
      return null;
    }
  }

  /**
   * Saves the user info
   *
   * @param userInfo User info
   */
  saveUserInfo(userInfo: UserInfo): void {
    localStorage.setItem(TOKEN_KEYS.USER_INFO, JSON.stringify(userInfo));
  }

  /**
   * Checks whether the user is logged in
   *
   * @returns Whether the user is logged in
   */
  isAuthenticated(): boolean {
    const token = this.getAccessToken();
    return token !== null && !this.isTokenExpired();
  }

  /**
   * Clears the token information
   *
   * <p>Clears all authentication info from both cookie and localStorage</p>
   */
  clearToken(): void {
    // Clear localStorage
    localStorage.removeItem(TOKEN_KEYS.ACCESS_TOKEN);
    localStorage.removeItem(TOKEN_KEYS.REFRESH_TOKEN);
    localStorage.removeItem(TOKEN_KEYS.TOKEN_TYPE);
    localStorage.removeItem(TOKEN_KEYS.EXPIRES_AT);
    localStorage.removeItem(TOKEN_KEYS.USER_INFO);

    // Clear cookies
    cookieManager.remove(TOKEN_KEYS.ACCESS_TOKEN);
    cookieManager.remove(TOKEN_KEYS.REFRESH_TOKEN);
  }

  /**
   * Refreshes the token
   *
   * @param newTokenInfo New token information
   */
  updateToken(newTokenInfo: Partial<TokenInfo>): void {
    const { accessToken, refreshToken } = newTokenInfo;

    if (accessToken) {
      localStorage.setItem(TOKEN_KEYS.ACCESS_TOKEN, accessToken);
      const expiresIn = parseInt(localStorage.getItem(TOKEN_KEYS.EXPIRES_AT) || '0', 10) - Date.now();
      cookieManager.set(TOKEN_KEYS.ACCESS_TOKEN, accessToken, {
        expires: expiresIn / 86400,
        path: '/',
        sameSite: 'lax',
      });
    }

    if (refreshToken) {
      localStorage.setItem(TOKEN_KEYS.REFRESH_TOKEN, refreshToken);
      const expiresIn = parseInt(localStorage.getItem(TOKEN_KEYS.EXPIRES_AT) || '0', 10) - Date.now();
      cookieManager.set(TOKEN_KEYS.REFRESH_TOKEN, refreshToken, {
        expires: expiresIn / 86400,
        path: '/',
        sameSite: 'lax',
      });
    }
  }
}

// Export the singleton
export const tokenStorage = new TokenStorageService();

// Default export
export default tokenStorage;
