package com.knowledge.base.foundation.service;

import com.knowledge.base.foundation.dto.SettingsDTO;
import com.knowledge.base.foundation.vo.SettingsVO;
import com.knowledge.base.foundation.vo.SystemStatusVO;

/**
 * System settings Service interface
 *
 * <p>Provides the ability to query and batch-update system settings by section,
 * encapsulating the read/write operations on the underlying configuration table.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface SettingsService {

    /**
     * Get all system settings (returned by section)
     */
    SettingsVO getSettings();

    /**
     * Batch-update settings by section
     */
    Boolean updateSettings(SettingsDTO settingsDTO);

    /**
     * Get the system's running status
     */
    SystemStatusVO getSystemStatus();

    /**
     * Clear the system cache
     */
    String clearCache();

    /**
     * Create a data backup
     */
    String createBackup();
}
