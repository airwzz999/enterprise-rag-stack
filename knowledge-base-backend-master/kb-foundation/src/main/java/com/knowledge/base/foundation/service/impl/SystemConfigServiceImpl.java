package com.knowledge.base.foundation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.config.SystemConfigCache;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.foundation.entity.SystemConfig;
import com.knowledge.base.foundation.mapper.SystemConfigMapper;
import com.knowledge.base.foundation.service.SystemConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    @Resource
    private SystemConfigMapper systemConfigMapper;

    @Resource
    private SystemConfigCache systemConfigCache;

    /**
     * Load all configs into the Redis cache after the service finishes starting
     * (once infrastructure such as the DB and Redis are ready)
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadConfigsToRedis() {
        log.info("Starting to load system configs into the Redis cache...");
        try {
            List<SystemConfig> configs = systemConfigMapper.selectList(
                    new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getDeleted, 0));
            Map<String, String> configMap = new HashMap<>();
            for (SystemConfig config : configs) {
                configMap.put(config.getConfigKey(), config.getConfigValue());
            }
            systemConfigCache.loadAll(configMap);
        } catch (Exception e) {
            log.error("Failed to load system configs into the Redis cache: {}", e.getMessage(), e);
        }
    }

    /**
     * Write to the Redis cache (executed after the transaction commits, to avoid a rollback overwriting it)
     */
    private void syncToRedis(SystemConfig config) {
        try {
            systemConfigCache.setConfig(config.getConfigKey(), config.getConfigValue());
        } catch (Exception e) {
            log.error("Failed to sync config to Redis: key={}, error={}", config.getConfigKey(), e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public IPage<SystemConfig> pageConfigs(Long current, Long size, String category) {
        log.info("Paginated query of configs: current={}, size={}, category={}", current, size, category);

        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(category)) {
            wrapper.eq(SystemConfig::getCategory, category);
        }

        wrapper.orderByAsc(SystemConfig::getId);

        Page<SystemConfig> page = new Page<>(current, size);
        return systemConfigMapper.selectPage(page, wrapper);
    }

    /** {@inheritDoc} */
    @Override
    public SystemConfig getConfigByKey(String key) {
        log.info("Get config: key={}", key);

        if (!StringUtils.hasText(key)) {
            throw new BusinessException("Config key must not be empty");
        }

        return systemConfigMapper.selectByConfigKey(key);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean createConfig(SystemConfig config) {
        log.info("Create config: key={}", config.getConfigKey());

        if (!StringUtils.hasText(config.getConfigKey())) {
            throw new BusinessException("Config key must not be empty");
        }

        SystemConfig existConfig = systemConfigMapper.selectByConfigKey(config.getConfigKey());
        if (existConfig != null) {
            throw new BusinessException("Config key already exists");
        }

        config.setId(SnowflakeIdGenerator.getInstance().nextId());
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());

        int count = systemConfigMapper.insert(config);
        if (count > 0) {
            syncToRedis(config);
        }
        return count > 0;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateConfig(String key, SystemConfig config) {
        log.info("Update config: key={}", key);

        SystemConfig existConfig = systemConfigMapper.selectByConfigKey(key);
        if (existConfig == null) {
            throw new BusinessException("Config does not exist");
        }

        existConfig.setConfigValue(config.getConfigValue());
        existConfig.setDescription(config.getDescription());
        existConfig.setCategory(config.getCategory());
        existConfig.setIsPublic(config.getIsPublic());
        existConfig.setUpdatedAt(LocalDateTime.now());

        int count = systemConfigMapper.updateById(existConfig);
        if (count > 0) {
            syncToRedis(existConfig);
        }
        return count > 0;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteConfig(String key) {
        log.info("Delete config: key={}", key);

        SystemConfig existConfig = systemConfigMapper.selectByConfigKey(key);
        if (existConfig == null) {
            throw new BusinessException("Config does not exist");
        }

        int count = systemConfigMapper.deleteById(existConfig.getId());
        if (count > 0) {
            systemConfigCache.deleteConfig(key);
        }
        return count > 0;
    }

    /** {@inheritDoc} */
    @Override
    public List<SystemConfig> getConfigsByCategory(String category) {
        log.info("Get configs by category: category={}", category);

        if (!StringUtils.hasText(category)) {
            throw new BusinessException("Config category must not be empty");
        }

        return systemConfigMapper.selectByCategory(category);
    }

    /** {@inheritDoc} */
    @Override
    public List<SystemConfig> getPublicConfigs() {
        log.info("Get public configs");

        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getIsPublic, 1);

        return systemConfigMapper.selectList(wrapper);
    }
}