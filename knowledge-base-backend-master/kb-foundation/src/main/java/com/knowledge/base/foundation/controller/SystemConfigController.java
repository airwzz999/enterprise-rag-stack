package com.knowledge.base.foundation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.base.common.result.Result;
import com.knowledge.base.foundation.entity.SystemConfig;
import com.knowledge.base.foundation.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * System configuration Controller
 *
 * <p>Designed according to the Alibaba Java Development Guidelines; provides
 * system configuration management endpoints</p>
 *
 * <p>Admin-only, except {@link #getPublicConfigs()}: {@code /config/public} is
 * intentionally open (permitted anonymously in {@code SecurityConfig} - needed by the
 * login/register pages before authentication). Every other endpoint here reads or
 * mutates system-wide config and is gated to admins, mirroring the frontend's
 * {@code admin/system-config} route, which is {@code requireAdmin}-gated.</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/config")
@Tag(name = "System Configuration Management", description = "System configuration management endpoints")
public class SystemConfigController {

    @Resource
    private SystemConfigService systemConfigService;

    /**
     * Paginated query of the config list
     *
     * @param current  current page
     * @param size     page size
     * @param category config category
     * @return paginated config information
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Paginated query of configs", description = "Paginated query of the system configuration list")
    public Result<IPage<SystemConfig>> pageConfigs(
        @Parameter(description = "Current page") @RequestParam(defaultValue = "1") Long current,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") Long size,
        @Parameter(description = "Config category") @RequestParam(required = false) String category) {
        log.info("Paginated config query request: current={}, size={}, category={}", current, size, category);

        IPage<SystemConfig> page = systemConfigService.pageConfigs(current, size, category);
        return Result.success(page);
    }

    /**
     * Query a config item by config key
     *
     * @param key config key
     * @return config information
     */
    @GetMapping("/{key}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Query config item", description = "Query a config item by config key")
    public Result<SystemConfig> getConfigByKey(
        @Parameter(description = "Config key", required = true)
        @PathVariable String key) {
        log.info("Query config item request: key={}", key);

        SystemConfig config = systemConfigService.getConfigByKey(key);
        return Result.success(config);
    }

    /**
     * Create a config
     *
     * @param config config information
     * @return whether it succeeded
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create config", description = "Create a new system config")
    public Result<Boolean> createConfig(@Valid @RequestBody SystemConfig config) {
        log.info("Create config request: key={}", config.getConfigKey());

        Boolean success = systemConfigService.createConfig(config);
        return Result.success("Config created successfully", success);
    }

    /**
     * Update a config
     *
     * @param key    config key
     * @param config config information
     * @return whether it succeeded
     */
    @PutMapping("/{key}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update config", description = "Update a system config")
    public Result<Boolean> updateConfig(
        @Parameter(description = "Config key", required = true)
        @PathVariable String key,
        @Valid @RequestBody SystemConfig config) {
        log.info("Update config request: key={}", key);

        Boolean success = systemConfigService.updateConfig(key, config);
        return Result.success("Config updated successfully", success);
    }

    /**
     * Delete a config
     *
     * @param key config key
     * @return whether it succeeded
     */
    @DeleteMapping("/{key}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete config", description = "Delete a config by config key")
    public Result<Boolean> deleteConfig(
        @Parameter(description = "Config key", required = true)
        @PathVariable String key) {
        log.info("Delete config request: key={}", key);

        Boolean success = systemConfigService.deleteConfig(key);
        return Result.success("Config deleted successfully", success);
    }

    /**
     * Get configs by category
     *
     * @param category config category
     * @return config list
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get configs by category", description = "Get the config list by config category")
    public Result<List<SystemConfig>> getConfigsByCategory(
        @Parameter(description = "Config category", required = true)
        @PathVariable String category) {
        log.info("Get configs by category request: category={}", category);

        List<SystemConfig> configs = systemConfigService.getConfigsByCategory(category);
        return Result.success(configs);
    }

    /**
     * Get public configs
     *
     * @return public config list
     */
    @GetMapping("/public")
    @Operation(summary = "Get public configs", description = "Get all public system configs")
    public Result<List<SystemConfig>> getPublicConfigs() {
        log.info("Get public configs request");

        List<SystemConfig> configs = systemConfigService.getPublicConfigs();
        return Result.success(configs);
    }
}
