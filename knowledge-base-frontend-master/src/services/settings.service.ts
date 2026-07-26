import { http } from './request';
import type { SystemSettings, SystemStatus } from '@/types';

const BASE_URL = '/config/settings';

/**
 * System settings service
 *
 * <p>Connects to the backend SettingsController, providing a unified entry point for
 * reading and writing system settings by section.
 * Unlike the SystemConfig CRUD in foundation.service.ts, this service is geared toward
 * the settings page's grouped display and batch update scenarios.</p>
 */
export const settingsService = {
  /**
   * Get all system settings (including system status)
   *
   * Return structure: { basic, security, storage, notification, ai, status }
   */
  getSettings: (): Promise<SystemSettings> =>
    http.get(BASE_URL),

  /**
   * Batch update settings by section
   *
   * @param section - Section identifier: basic | security | storage | notification | ai
   * @param settings - Field key-value pairs to update within that section
   */
  updateSettings: (section: string, settings: Record<string, unknown>): Promise<boolean> =>
    http.put(BASE_URL, { section, settings }),

  /**
   * Get system running status
   */
  getSystemStatus: (): Promise<SystemStatus> =>
    http.get(`${BASE_URL}/status`),

  /**
   * Clear system cache
   */
  clearCache: (): Promise<string> =>
    http.post(`${BASE_URL}/cache/clear`),

  /**
   * Create a data backup
   */
  createBackup: (): Promise<string> =>
    http.post(`${BASE_URL}/backup`),

  /**
   * Send a test email
   *
   * @param email - Target email address
   */
  testEmail: (email: string): Promise<void> =>
    http.post(`${BASE_URL}/test-email`, { email }),
};
