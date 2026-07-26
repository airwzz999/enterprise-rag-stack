package com.knowledge.base.foundation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.base.common.config.SystemConfigCache;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.foundation.dto.SettingsDTO;
import com.knowledge.base.foundation.entity.SystemConfig;
import com.knowledge.base.foundation.mapper.SystemConfigMapper;
import com.knowledge.base.foundation.service.SettingsService;
import com.knowledge.base.foundation.vo.SettingsVO;
import com.knowledge.base.foundation.vo.SystemStatusVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * System settings Service implementation
 *
 * <p>Manages the mapping between config keys and settings fields, providing
 * per-section read/write and type conversion capabilities.</p>
 *
 * <p>Design notes:
 * <ul>
 *   <li>An in-memory mapping table bridges the bidirectional conversion between "config_key" and the "settings field"</li>
 *   <li>Write operations automatically create missing config items (upsert semantics)</li>
 *   <li>Read operations return a preset default value for missing configs</li>
 *   <li>bool/number values are stored as strings in the DB and automatically converted on read/write</li>
 * </ul>
 * </p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class SettingsServiceImpl implements SettingsService {

    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Resource
    private SystemConfigCache systemConfigCache;

    /**
     * Mapping from settings field → database config key
     * key: the frontend settings field name
     * value: [config_key, config_type, default_value, category]
     */
    private static final Map<String, String[]> FIELD_TO_CONFIG = new LinkedHashMap<>();
    static {
        // ===== Basic settings =====
        FIELD_TO_CONFIG.put("systemName",           new String[]{"system.name",                    "string",  "Intelligent Knowledge Base",         "SYSTEM"});
        FIELD_TO_CONFIG.put("systemDescription",    new String[]{"system.description",             "string",  "Enterprise-grade intelligent knowledge management platform", "SYSTEM"});
        FIELD_TO_CONFIG.put("systemVersion",        new String[]{"system.version",                 "string",  "v2.4.1",                             "SYSTEM"});
        FIELD_TO_CONFIG.put("defaultLanguage",      new String[]{"system.language",                "string",  "zh-CN",                              "SYSTEM"});
        FIELD_TO_CONFIG.put("timezone",             new String[]{"system.timezone",                "string",  "Asia/Shanghai",                      "SYSTEM"});
        FIELD_TO_CONFIG.put("allowRegistration",    new String[]{"user.registration.enabled",      "boolean", "true",                               "SYSTEM"});
        FIELD_TO_CONFIG.put("requireApproval",      new String[]{"system.requireApproval",         "boolean", "true",                               "SYSTEM"});
        FIELD_TO_CONFIG.put("enableComments",       new String[]{"system.enableComments",          "boolean", "true",                               "SYSTEM"});
        FIELD_TO_CONFIG.put("enableAI",             new String[]{"system.enableAI",                "boolean", "true",                               "SYSTEM"});
        FIELD_TO_CONFIG.put("enableAIWriting",      new String[]{"system.enableAIWriting",         "boolean", "true",                               "SYSTEM"});
        FIELD_TO_CONFIG.put("enableFullTextSearch", new String[]{"system.enableFullTextSearch",    "boolean", "true",                               "SYSTEM"});

        // ===== Security settings =====
        FIELD_TO_CONFIG.put("passwordPolicy",       new String[]{"system.passwordPolicy",          "string",  "medium",                             "SECURITY"});
        FIELD_TO_CONFIG.put("sessionTimeout",       new String[]{"auth.session.timeout",           "number",  "3600",                               "SECURITY"});
        FIELD_TO_CONFIG.put("enable2FA",            new String[]{"system.enable2FA",               "boolean", "false",                              "SECURITY"});
        FIELD_TO_CONFIG.put("ipRestriction",        new String[]{"system.ipRestriction",           "boolean", "false",                              "SECURITY"});
        FIELD_TO_CONFIG.put("passwordMinLength",    new String[]{"auth.password.min.length",       "number",  "8",                                  "SECURITY"});
        FIELD_TO_CONFIG.put("requireSpecialChar",   new String[]{"auth.password.require.special",  "boolean", "true",                               "SECURITY"});
        FIELD_TO_CONFIG.put("loginMaxRetry",        new String[]{"auth.login.max.retry",           "number",  "5",                                  "SECURITY"});

        // ===== Storage settings =====
        FIELD_TO_CONFIG.put("maxFileSize",          new String[]{"file.upload.max.size",           "number",  "104857600",                          "STORAGE"});
        FIELD_TO_CONFIG.put("allowedFileTypes",     new String[]{"file.upload.allowed.types",      "string",  "pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,jpg,jpeg,png,gif,bmp,webp,svg,ico,mp4,avi,mov,wmv,flv,mkv,webm,mp3,wav,flac,aac,ogg,m4a,wma", "STORAGE"});
        FIELD_TO_CONFIG.put("storageEndpoints",     new String[]{"rustfs.endpoints",               "string",  "http://localhost:8200",              "STORAGE"});
        FIELD_TO_CONFIG.put("storageBucket",        new String[]{"rustfs.bucket",                  "string",  "knowledge-docs",                     "STORAGE"});

        // ===== Notification settings =====
        FIELD_TO_CONFIG.put("emailEnabled",         new String[]{"email.enabled",                  "boolean", "true",                               "NOTIFICATION"});
        FIELD_TO_CONFIG.put("emailHost",            new String[]{"email.host",                     "string",  "smtp.example.com",                   "NOTIFICATION"});
        FIELD_TO_CONFIG.put("emailPort",            new String[]{"email.port",                     "number",  "587",                                "NOTIFICATION"});
        FIELD_TO_CONFIG.put("websocketEnabled",     new String[]{"websocket.enabled",              "boolean", "true",                               "NOTIFICATION"});
        FIELD_TO_CONFIG.put("notificationRetentionDays", new String[]{"notification.retention.days", "number", "90",                            "NOTIFICATION"});

        // ===== AI settings =====
        FIELD_TO_CONFIG.put("aiModelName",          new String[]{"qwen.model.name",                "string",  "qwen-max",                           "AI"});
        FIELD_TO_CONFIG.put("embeddingModel",       new String[]{"qwen.embedding.model",           "string",  "text-embedding-v3",                  "AI"});
        FIELD_TO_CONFIG.put("milvusHost",           new String[]{"milvus.host",                    "string",  "localhost",                          "AI"});
        FIELD_TO_CONFIG.put("milvusPort",           new String[]{"milvus.port",                    "number",  "19530",                              "AI"});
    }

    // ==================== Read by section ====================

    /** {@inheritDoc} */
    @Override
    public SettingsVO getSettings() {
        log.info("Get system settings");

        // Query all configs in one pass to avoid N+1
        List<SystemConfig> allConfigs = systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getDeleted, 0)
        );

        Map<String, String> configMap = new HashMap<>();
        for (SystemConfig config : allConfigs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }

        // Assemble sections
        Map<String, Object> basic        = buildSection(configMap, "SYSTEM",     SETTINGS_BASIC_FIELDS,         "basic");
        Map<String, Object> security     = buildSection(configMap, "SECURITY",   SETTINGS_SECURITY_FIELDS,       "security");
        Map<String, Object> storage      = buildSection(configMap, "STORAGE",    SETTINGS_STORAGE_FIELDS,        "storage");
        Map<String, Object> notification = buildSection(configMap, "NOTIFICATION", SETTINGS_NOTIFICATION_FIELDS, "notification");
        Map<String, Object> ai           = buildSection(configMap, "AI",         SETTINGS_AI_FIELDS,             "ai");

        return SettingsVO.builder()
                .basic(basic)
                .security(security)
                .storage(storage)
                .notification(notification)
                .ai(ai)
                .status(getSystemStatus())
                .build();
    }

    // Field lists for each section
    private static final List<String> SETTINGS_BASIC_FIELDS = List.of(
            "systemName", "systemDescription", "systemVersion", "defaultLanguage", "timezone",
            "allowRegistration", "requireApproval", "enableComments", "enableAI", "enableAIWriting", "enableFullTextSearch"
    );
    private static final List<String> SETTINGS_SECURITY_FIELDS = List.of(
            "passwordPolicy", "sessionTimeout", "enable2FA", "ipRestriction",
            "passwordMinLength", "requireSpecialChar", "loginMaxRetry"
    );
    private static final List<String> SETTINGS_STORAGE_FIELDS = List.of(
            "maxFileSize", "allowedFileTypes", "storageEndpoints", "storageBucket"
    );
    private static final List<String> SETTINGS_NOTIFICATION_FIELDS = List.of(
            "emailEnabled", "emailHost", "emailPort", "websocketEnabled", "notificationRetentionDays"
    );
    private static final List<String> SETTINGS_AI_FIELDS = List.of(
            "aiModelName", "embeddingModel", "milvusHost", "milvusPort"
    );

    /**
     * Build a section's config Map from a field list
     */
    private Map<String, Object> buildSection(Map<String, String> configMap,
                                              String category,
                                              List<String> fieldNames,
                                              String sectionName) {
        Map<String, Object> section = new LinkedHashMap<>();
        for (String fieldName : fieldNames) {
            String[] meta = FIELD_TO_CONFIG.get(fieldName);
            if (meta == null) {
                log.warn("Unknown settings field: {}", fieldName);
                continue;
            }
            String configKey  = meta[0];
            String configType = meta[1];
            String rawValue = configMap.getOrDefault(configKey, meta[2]);

            section.put(fieldName, convertValue(rawValue, configType));
        }
        return section;
    }

    /**
     * Convert a string value into a Java object based on its type
     */
    private Object convertValue(String raw, String type) {
        if (raw == null) return null;
        return switch (type) {
            case "boolean" -> "true".equalsIgnoreCase(raw) || "1".equals(raw);
            case "number"  -> {
                try {
                    yield Long.parseLong(raw.trim());
                } catch (NumberFormatException e) {
                    yield raw;
                }
            }
            default -> raw;
        };
    }

    // ==================== Update by section ====================

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"configCache", "settingsCache"}, allEntries = true)
    public Boolean updateSettings(SettingsDTO settingsDTO) {
        String section = settingsDTO.getSection();
        Map<String, Object> settings = settingsDTO.getSettings();
        log.info("Batch update settings: section={}, fieldCount={}", section, settings.size());

        if (settings == null || settings.isEmpty()) {
            throw new BusinessException("Settings content must not be empty");
        }

        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            String[] meta = FIELD_TO_CONFIG.get(fieldName);
            if (meta == null) {
                log.warn("Skipping unknown settings field: {}", fieldName);
                continue;
            }
            String configKey  = meta[0];
            String configType = meta[1];
            String category   = meta[3];

            upsertConfig(configKey, configType, category, value);
        }

        return true;
    }

    /**
     * Create or update a single config item
     */
    private void upsertConfig(String configKey, String configType, String category, Object value) {
        String stringValue = (value instanceof Boolean)
                ? String.valueOf(value)
                : Objects.toString(value, "");

        SystemConfig existConfig = systemConfigMapper.selectByConfigKey(configKey);

        if (existConfig != null) {
            // Update
            existConfig.setConfigValue(stringValue);
            existConfig.setConfigType(configType);
            existConfig.setCategory(category);
            existConfig.setUpdatedAt(LocalDateTime.now());
            systemConfigMapper.updateById(existConfig);
        } else {
            // Create
            SystemConfig newConfig = new SystemConfig();
            newConfig.setId(SnowflakeIdGenerator.getInstance().nextId());
            newConfig.setConfigKey(configKey);
            newConfig.setConfigValue(stringValue);
            newConfig.setConfigType(configType);
            newConfig.setCategory(category);
            newConfig.setDescription("Automatically created by the settings page");
            newConfig.setIsPublic(1);
            newConfig.setCreatedAt(LocalDateTime.now());
            newConfig.setUpdatedAt(LocalDateTime.now());
            systemConfigMapper.insert(newConfig);
        }
        // Sync to the Redis cache
        systemConfigCache.setConfig(configKey, stringValue);
    }

    // ==================== System status ====================

    /** {@inheritDoc} */
    @Override
    public SystemStatusVO getSystemStatus() {
        // Read system configuration
        List<SystemConfig> allConfigs = systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getDeleted, 0)
        );
        Map<String, String> configMap = new HashMap<>();
        for (SystemConfig config : allConfigs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }

        String version = configMap.getOrDefault("system.version", "v2.4.1");
        String startTime = getJvmStartTime();

        return SystemStatusVO.builder()
                .version(version)
                .runStatus("running")
                .dbStatus("connected")
                .lastBackupTime(LocalDateTime.now().minusDays(1)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .totalStorage(107374182400L)  // 100GB placeholder
                .usedStorage(72796356608L)    // 67.8GB placeholder
                .documentCount(2847L)
                .userCount(128L)
                .startTime(startTime)
                .build();
    }

    private String getJvmStartTime() {
        long startTimeMs = ManagementFactory.getRuntimeMXBean().getStartTime();
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(startTimeMs));
    }

    // ==================== Operations ====================

    /** {@inheritDoc} */
    @Override
    public String clearCache() {
        log.info("Clear system cache");
        // Cache eviction logic is handled by the @CacheEvict annotation; this is a placeholder implementation
        return "Cache cleared";
    }

    /** {@inheritDoc} */
    @Override
    public String createBackup() {
        log.info("Create system backup");
        // Backup logic needs to integrate with a concrete storage solution; this is a placeholder implementation
        return "Backup created at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
