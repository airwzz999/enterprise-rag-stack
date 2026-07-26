package com.knowledge.base.foundation.controller;

import com.knowledge.base.common.result.Result;
import com.knowledge.base.foundation.dto.SettingsDTO;
import com.knowledge.base.foundation.dto.TestEmailDTO;
import com.knowledge.base.foundation.service.SettingsService;
import com.knowledge.base.foundation.vo.SettingsVO;
import com.knowledge.base.foundation.vo.SystemStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * System settings Controller
 *
 * <p>Provides a unified entry point for reading and writing system settings by
 * section; the frontend SettingsPage uses this endpoint to load and save each
 * section's settings.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/config/settings")
@Tag(name = "System Settings", description = "System settings section management endpoints")
public class SettingsController {

    @Resource
    private SettingsService settingsService;

    /**
     * Get all system settings
     */
    @GetMapping
    @Operation(summary = "Get all settings", description = "Returns the settings organized by section along with the system status")
    public Result<SettingsVO> getSettings() {
        SettingsVO settings = settingsService.getSettings();
        return Result.success(settings);
    }

    /**
     * Update settings by section
     */
    @PutMapping
    @Operation(summary = "Batch update settings", description = "Update the settings under the specified section; fields not supplied remain unchanged")
    public Result<Boolean> updateSettings(@Valid @RequestBody SettingsDTO settingsDTO) {
        Boolean result = settingsService.updateSettings(settingsDTO);
        return Result.success("Settings saved", result);
    }

    /**
     * Get the system's running status
     */
    @GetMapping("/status")
    @Operation(summary = "Get system status", description = "Returns runtime metrics such as system version, database connection status, and storage usage")
    public Result<SystemStatusVO> getSystemStatus() {
        SystemStatusVO status = settingsService.getSystemStatus();
        return Result.success(status);
    }

    /**
     * Clear the system cache
     */
    @PostMapping("/cache/clear")
    @Operation(summary = "Clear cache", description = "Clear Redis and other system caches to free up storage space")
    public Result<String> clearCache() {
        String result = settingsService.clearCache();
        return Result.success("Cache cleared", result);
    }

    /**
     * Create a data backup
     */
    @PostMapping("/backup")
    @Operation(summary = "Create backup", description = "Create a full backup of the system data")
    public Result<String> createBackup() {
        String result = settingsService.createBackup();
        return Result.success("Backup created successfully", result);
    }
}
