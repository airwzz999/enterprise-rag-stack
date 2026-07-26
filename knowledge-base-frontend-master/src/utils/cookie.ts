/**
 * Cookie management utility
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Provides read, set, and delete operations for cookies</li>
 *   <li>Supports HttpOnly and Secure attributes</li>
 *   <li>Supports the SameSite attribute (CSRF protection)</li>
 *   <li>Supports configuring an expiration time</li>
 * </ul>
 *
 * <p>Security features:</p>
 * <ul>
 *   <li>HttpOnly: prevents cookie theft via XSS</li>
 *   <li>Secure: only transmitted over HTTPS</li>
 *   <li>SameSite: prevents CSRF attacks</li>
 * </ul>
 *
 * @since 1.0.0
 */

/**
 * Cookie options interface
 */
export interface CookieOptions {
  /** Cookie expiration time (in days), defaults to 7 days */
  expires?: number;
  /** Cookie path, defaults to '/' */
  path?: string;
  /** Cookie domain */
  domain?: string;
  /** Whether to restrict transmission to HTTPS only (recommended in production) */
  secure?: boolean;
  /** Whether the cookie is HttpOnly (prevents XSS; must be set server-side) */
  httpOnly?: boolean;
  /** SameSite attribute ('strict' | 'lax' | 'none') */
  sameSite?: 'strict' | 'lax' | 'none';
}

/**
 * Cookie manager class
 */
class CookieManager {
  /**
   * Default cookie configuration
   */
  private readonly defaultOptions: CookieOptions = {
    expires: 7, // Expires after 7 days
    path: '/', // Available on all paths
    secure: false, // HTTPS not used in development (set to true in production)
    sameSite: 'lax', // Prevents CSRF while still allowing cross-site navigation
  };

  /**
   * Sets a cookie
   *
   * @param key Cookie key
   * @param value Cookie value (automatically URI-encoded)
   * @param options Cookie options
   */
  set(key: string, value: string, options: CookieOptions = {}): void {
    const mergedOptions = { ...this.defaultOptions, ...options };

    // Build the cookie string
    let cookieString = `${encodeURIComponent(key)}=${encodeURIComponent(value)}`;

    // Add the expiration time
    if (mergedOptions.expires) {
      const date = new Date();
      date.setTime(date.getTime() + mergedOptions.expires * 24 * 60 * 60 * 1000);
      cookieString += `; expires=${date.toUTCString()}`;
    }

    // Add the path
    if (mergedOptions.path) {
      cookieString += `; path=${mergedOptions.path}`;
    }

    // Add the domain
    if (mergedOptions.domain) {
      cookieString += `; domain=${mergedOptions.domain}`;
    }

    // Add the Secure flag
    if (mergedOptions.secure) {
      cookieString += '; secure';
    }

    // Add the SameSite flag
    if (mergedOptions.sameSite) {
      cookieString += `; samesite=${mergedOptions.sameSite}`;
    }

    // Set the cookie
    document.cookie = cookieString;
  }

  /**
   * Gets a cookie value
   *
   * @param key Cookie key
   * @returns The decoded cookie value, or null if it doesn't exist
   */
  get(key: string): string | null {
    const encodedKey = encodeURIComponent(key);
    const cookies = document.cookie.split(';');

    for (const cookie of cookies) {
      const [cookieKey, cookieValue] = cookie.trim().split('=');
      if (cookieKey === encodedKey) {
        return cookieValue ? decodeURIComponent(cookieValue) : '';
      }
    }

    return null;
  }

  /**
   * Removes a cookie
   *
   * @param key Cookie key
   * @param options Cookie options (mainly path and domain)
   */
  remove(key: string, options: CookieOptions = {}): void {
    // Set the expiration time in the past to invalidate the cookie
    this.set(key, '', {
      ...options,
      expires: -1, // Expire immediately
    });
  }

  /**
   * Checks whether a cookie exists
   *
   * @param key Cookie key
   * @returns Whether it exists
   */
  has(key: string): boolean {
    return this.get(key) !== null;
  }

  /**
   * Gets all cookie key/value pairs
   *
   * @returns A cookie object
   */
  getAll(): Record<string, string> {
    const cookies: Record<string, string> = {};
    const cookieStrings = document.cookie.split(';');

    for (const cookieString of cookieStrings) {
      const [key, value] = cookieString.trim().split('=');
      if (key && value !== undefined) {
        try {
          cookies[decodeURIComponent(key)] = decodeURIComponent(value);
        } catch {
          // Decoding failed, skip this cookie
          cookies[key] = value;
        }
      }
    }

    return cookies;
  }

  /**
   * Clears all cookies
   *
   * @param options Cookie options
   */
  clearAll(options: CookieOptions = {}): void {
    const cookies = this.getAll();
    Object.keys(cookies).forEach(key => {
      this.remove(key, options);
    });
  }
}

// Export the singleton
export const cookieManager = new CookieManager();

// Default export
export default cookieManager;
