import axios, { AxiosError, AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { messageHolder as message } from '@/utils/message-holder';
import { tokenStorage } from '@/utils/token-storage';
import type { LoginResponse } from '@/utils/token-storage';

/**
 * HTTP request configuration
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Unified axios instance configuration</li>
 *   <li>Request interception: automatically attach the token, handle encryption</li>
 *   <li>Response interception: unified response data handling and error handling</li>
 *   <li>Token refresh: automatically refresh an expired token</li>
 * </ul>
 *
 * @author airwzz999
 * @since 1.0.0
 */

// Extend the AxiosRequestConfig type with custom config options
interface CustomAxiosRequestConfig extends AxiosRequestConfig {
  // Whether to skip token validation
  skipAuth?: boolean;
  // Whether to skip the error toast
  skipErrorToast?: boolean;
  // Whether to show a loading spinner
  showLoading?: boolean;
  // Download mode: returns the full AxiosResponse so headers like Content-Disposition can be read
  _download?: boolean;
}

/**
 * Create the axios instance
 *
 * <p>Configuration notes:</p>
 * <ul>
 *   <li>withCredentials: false - does not rely on cookies being sent automatically</li>
 *   <li>The token is passed via the Authorization header (recommended approach)</li>
 *   <li>In development, cookies can be set manually via a browser extension for testing</li>
 * </ul>
 */
const request: AxiosInstance = axios.create({
  baseURL: '/api', // Uses a proxy path to avoid CORS issues
  timeout: 120000, // 2 minutes, to support long-running operations such as document import
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    'Accept': 'application/json;charset=UTF-8',
  },
  withCredentials: false, // Cookies are not sent automatically; the Authorization header is used instead
});

/**
 * Whether the token is currently being refreshed
 */
let isRefreshing = false;

/**
 * Waiting queue (requests paused while the token is being refreshed)
 */
let requests: Array<(token: string) => void> = [];

/**
 * Request interceptor
 *
 * <p>Main responsibilities:</p>
 * <ol>
 *   <li>Automatically attach the Authorization header</li>
 *   <li>Attach the CSRF token</li>
 *   <li>Attach a request tracing ID</li>
 *   <li>Log output in development</li>
 * </ol>
 */
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const customConfig = config as CustomAxiosRequestConfig;

    // Attach the token unless this request skips authentication
    if (!customConfig.skipAuth) {
      const authHeader = tokenStorage.getAuthorizationHeader();

      // Debug log: shows how the token was obtained (disabled by default)
      if (import.meta.env.DEV && false) {  // Set to false to disable this debug log
        console.log('🔐 Token retrieval status:');
        console.log('  - authHeader:', authHeader ? authHeader.substring(0, 50) + '...' : 'none');
        console.log('  - Cookie:', document.cookie.split('; ').find(c => c.startsWith('access_token='))?.substring(0, 50) + '...' || 'none');
      }

      if (authHeader && config.headers) {
        config.headers.Authorization = authHeader;
      } else if (import.meta.env.DEV) {
        console.warn('⚠️ Token not found, the request may fail');
      }
    }

    // Attach the CSRF token (if present)
    const csrfToken = document
      .querySelector('meta[name="csrf-token"]')
      ?.getAttribute('content');
    if (csrfToken && config.headers) {
      config.headers['X-CSRF-TOKEN'] = csrfToken;
    }

    // Attach a request tracing ID (used for log tracing)
    if (config.headers) {
      config.headers['X-Request-ID'] = generateRequestId();
      config.headers['X-Requested-With'] = 'XMLHttpRequest';
    }

    // Add debug info in development (disabled by default to reduce console noise)
    if (import.meta.env.DEV && false) {  // Set to false to disable this debug log
      console.log('🔵 Request:', config.method?.toUpperCase(), (config.baseURL || '') + (config.url || ''));
      console.log('🔵 Headers:', config.headers);
      console.log('🔵 Data:', config.data);
    }

    return config;
  },
  (error: AxiosError) => {
    console.error('Request interceptor error:', error);
    return Promise.reject(error);
  }
);

/**
 * Response interceptor
 *
 * <p>Main responsibilities:</p>
 * <ol>
 *   <li>Unified handling of the response data structure</li>
 *   <li>Handling business error codes</li>
 *   <li>Automatic token refresh</li>
 *   <li>Unified error handling</li>
 * </ol>
 */
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data, config } = response;
    const customConfig = config as CustomAxiosRequestConfig;

    // Download requests: return the full AxiosResponse so headers like Content-Disposition can be read
    if (customConfig._download) {
      return response;
    }

    // Add debug info in development (disabled by default to reduce console noise)
    if (import.meta.env.DEV && false) {  // Set to false to disable this debug log
      console.log('🟢 Response:', response.config.method?.toUpperCase(), response.config.url);
      console.log('🟢 Status:', response.status);
      console.log('🟢 Data:', data);
    }

    // Handle according to the backend response structure
    // Unified backend response format: { code, message, data, timestamp }
    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code === 200) {
        // Success response, return the data field
        return data.data;
      }

      // 401 Unauthorized: clear the token and redirect to the login page
      if (data.code === 401) {
        if (!customConfig.skipAuth) {
          handleUnauthorized();
        }
        return Promise.reject(new Error(data.message || 'Unauthorized, please log in again'));
      }

      // Other business errors
      const errorMessage = data.message || 'Request failed';
      if (!customConfig.skipErrorToast) {
        message.error(errorMessage);
      }
      return Promise.reject(new Error(errorMessage));
    }

    // Non-standard response format, return as-is
    return data;
  },
  (error: AxiosError) => {
    const { config, response } = error;
    const customConfig = config as CustomAxiosRequestConfig;

    // Add debug info in development (disabled by default to reduce console noise)
    if (import.meta.env.DEV && false) {  // Set to false to disable this debug log
      console.error('🔴 Response Error:', error);
      console.error('🔴 Config:', error.config);
      console.error('🔴 Response:', error.response);
    }

    // Handle 401 Unauthorized errors
    if (response?.status === 401) {
      // Try to refresh the token unless this request skips authentication
      if (!customConfig?.skipAuth && !isRefreshing) {
        // Token expired or invalid
        return handleTokenExpired(error);
      } else {
        // Skipping auth or already refreshing, redirect to login directly
        handleUnauthorized();
        return Promise.reject(new Error('Unauthorized, please log in again'));
      }
    }

    // Handle other errors
    return handleRequestError(error, customConfig);
  }
);

/**
 * Handle token expiration
 *
 * @param error Original error
 * @returns Promise
 */
function handleTokenExpired(error: AxiosError): Promise<any> {
  // If the token is already being refreshed, queue this request
  if (isRefreshing) {
    return new Promise((resolve) => {
      requests.push((token: string) => {
        const originalRequest = error.config;
        if (originalRequest?.headers) {
          originalRequest.headers.Authorization = `Bearer ${token}`;
        }
        resolve(request(originalRequest!));
      });
    });
  }

  isRefreshing = true;

  // Try to refresh the token
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) {
    // No refresh token available, redirect to login directly
    handleUnauthorized();
    isRefreshing = false;
    return Promise.reject(new Error('Unauthorized, please log in again'));
  }

  // Call the token refresh endpoint
  return http
    .post<LoginResponse>('/auth/auth/refresh', null, {
      params: { refreshToken },
      skipAuth: true, // Do not use the old token when refreshing
    })
    .then((newTokenInfo) => {
      // Save the new token
      tokenStorage.saveToken(newTokenInfo);

      // Run the queued requests
      requests.forEach((callback) => callback(newTokenInfo.accessToken));
      requests = [];

      // Retry the original request
      const originalRequest = error.config;
      if (originalRequest?.headers) {
        originalRequest.headers.Authorization = `Bearer ${newTokenInfo.accessToken}`;
      }
      return request(originalRequest!);
    })
    .catch(() => {
      // Refresh failed, clear the token and redirect to login
      tokenStorage.clearToken();
      handleUnauthorized();
      return Promise.reject(new Error('Token refresh failed, please log in again'));
    })
    .finally(() => {
      isRefreshing = false;
    });
}

/**
 * Handle unauthorized errors
 */
function handleUnauthorized(): void {
  // Clear the token
  tokenStorage.clearToken();

  // Clear user info and zustand-persisted data
  localStorage.removeItem('user');
  localStorage.removeItem('auth-storage');

  // Show a notice
  message.warning('Your session has expired, please log in again');

  // Delay the redirect to avoid blocking
  setTimeout(() => {
    window.location.href = '/login';
  }, 1000);
}

/**
 * Handle request errors
 *
 * @param error Axios error object
 * @param customConfig Custom config
 * @returns Promise
 */
function handleRequestError(
  error: AxiosError,
  customConfig: CustomAxiosRequestConfig
): Promise<any> {
  let errorMessage = 'Request failed';

  if (error.response) {
    const { status, data } = error.response;

    switch (status) {
      case 400:
        errorMessage = (data as any)?.message || 'Invalid request parameters';
        break;
      case 403:
        errorMessage = (data as any)?.message || 'Insufficient permissions to access this resource';
        break;
      case 404:
        errorMessage = 'The requested resource does not exist';
        break;
      case 500:
        errorMessage = 'Server error, please try again later';
        break;
      case 502:
        errorMessage = 'Gateway error, please try again later';
        break;
      case 503:
        errorMessage = 'Service temporarily unavailable, please try again later';
        break;
      default:
        errorMessage = (data as any)?.message || `Request failed (${status})`;
    }
  } else if (error.request) {
    // The request was sent but no response was received
    errorMessage = 'Network error, please check your connection';
  } else {
    // Request configuration error
    errorMessage = error.message || 'Request configuration error';
  }

  // Show the error notice
  if (!customConfig?.skipErrorToast) {
    message.error(errorMessage);
  }

  return Promise.reject(new Error(errorMessage));
}

/**
 * Generate a request tracing ID
 *
 * @returns Tracing ID
 */
function generateRequestId(): string {
  return `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
}

/**
 * Wrapper for common request methods
 */
export const http = {
  get: <T = any>(url: string, config?: CustomAxiosRequestConfig): Promise<T> => {
    return request.get(url, config);
  },

  post: <T = any>(
    url: string,
    data?: any,
    config?: CustomAxiosRequestConfig
  ): Promise<T> => {
    return request.post(url, data, config);
  },

  put: <T = any>(
    url: string,
    data?: any,
    config?: CustomAxiosRequestConfig
  ): Promise<T> => {
    return request.put(url, data, config);
  },

  delete: <T = any>(url: string, config?: CustomAxiosRequestConfig): Promise<T> => {
    return request.delete(url, config);
  },

  patch: <T = any>(
    url: string,
    data?: any,
    config?: CustomAxiosRequestConfig
  ): Promise<T> => {
    return request.patch(url, data, config);
  },

  /**
   * Form submission (for file uploads, etc.)
   */
  postForm: <T = any>(url: string, data: FormData): Promise<T> => {
    return request.post(url, data, {
      headers: {
        // Do not set Content-Type manually; let the browser set it automatically (including the boundary)
        'Content-Type': undefined,
      },
    });
  },

  /**
   * Download file
   */
  download: (url: string, filename?: string): Promise<void> => {
    return request.get(url, {
      responseType: 'blob',
      _download: true,
    } as any).then((response: AxiosResponse) => {
      // Try to extract the filename from the Content-Disposition header
      let finalFilename = filename || 'download.pdf';
      const contentDisposition = response.headers?.['content-disposition'];
      if (contentDisposition) {
        // Prefer the filename*=UTF-8''xxx format (RFC 5987)
        const rfc5987Match = contentDisposition.match(/filename\*=UTF-8''([^"';]+)/);
        if (rfc5987Match) {
          try {
            finalFilename = decodeURIComponent(rfc5987Match[1]);
          } catch (e) {
            finalFilename = rfc5987Match[1];
          }
        } else {
          // Fall back to the filename="xxx" format
          const standardMatch = contentDisposition.match(/filename="([^"]+)"/);
          if (standardMatch) {
            finalFilename = standardMatch[1];
          }
        }
      }

      const blob = new Blob([response.data], { type: 'application/pdf' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = finalFilename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(link.href);
    });
  },

  /**
   * Download file via POST (used for batch export scenarios, etc.)
   */
  downloadPost: (url: string, data: any, filename?: string): Promise<void> => {
    return request.post(url, data, {
      responseType: 'blob',
    } as any).then((response: AxiosResponse) => {
      let finalFilename = filename || 'export.zip';
      const contentDisposition = response.headers?.['content-disposition'];
      if (contentDisposition) {
        const rfc5987Match = contentDisposition.match(/filename\*=UTF-8''([^"';]+)/);
        if (rfc5987Match) {
          try {
            finalFilename = decodeURIComponent(rfc5987Match[1]);
          } catch (e) {
            finalFilename = rfc5987Match[1];
          }
        } else {
          const standardMatch = contentDisposition.match(/filename="([^"]+)"/);
          if (standardMatch) {
            finalFilename = standardMatch[1];
          }
        }
      }

      const blob = new Blob([response.data], { type: 'application/zip' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = finalFilename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(link.href);
    });
  },
};

export default request;
