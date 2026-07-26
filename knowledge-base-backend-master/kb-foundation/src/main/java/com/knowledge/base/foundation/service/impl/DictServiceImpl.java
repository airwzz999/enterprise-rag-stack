package com.knowledge.base.foundation.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.foundation.entity.Dict;
import com.knowledge.base.foundation.entity.DictData;
import com.knowledge.base.foundation.mapper.DictDataMapper;
import com.knowledge.base.foundation.mapper.DictMapper;
import com.knowledge.base.foundation.service.DictService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

    @Resource
    private DictMapper dictMapper;

    @Resource
    private DictDataMapper dictDataMapper;

    /** {@inheritDoc} */
    @Override
    public IPage<Dict> pageDicts(Long current, Long size, String keyword) {
        log.info("Paginated query of dictionaries: current={}, size={}, keyword={}", current, size, keyword);

        LambdaQueryWrapper<Dict> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(Dict::getDictCode, keyword)
                    .or()
                    .like(Dict::getDictName, keyword);
        }

        wrapper.orderByAsc(Dict::getSort);

        Page<Dict> page = new Page<>(current, size);
        return dictMapper.selectPage(page, wrapper);
    }

    /** {@inheritDoc} */
    @Override
    public Dict getDictByCode(String code) {
        log.info("Get dictionary by code: code={}", code);

        if (!StringUtils.hasText(code)) {
            throw new BusinessException("Dictionary code must not be empty");
        }

        return dictMapper.selectByDictCode(code);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean createDict(Dict dict) {
        log.info("Create dictionary: code={}, name={}", dict.getDictCode(), dict.getDictName());

        if (!StringUtils.hasText(dict.getDictCode())) {
            throw new BusinessException("Dictionary code must not be empty");
        }
        if (!StringUtils.hasText(dict.getDictName())) {
            throw new BusinessException("Dictionary name must not be empty");
        }

        Dict existDict = dictMapper.selectByDictCode(dict.getDictCode());
        if (existDict != null) {
            throw new BusinessException("Dictionary code already exists");
        }

        dict.setId(SnowflakeIdGenerator.getInstance().nextId());
        dict.setCreatedAt(LocalDateTime.now());
        dict.setUpdatedAt(LocalDateTime.now());

        int count = dictMapper.insert(dict);
        return count > 0;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateDict(String code, Dict dict) {
        log.info("Update dictionary: code={}", code);

        Dict existDict = dictMapper.selectByDictCode(code);
        if (existDict == null) {
            throw new BusinessException("Dictionary does not exist");
        }

        existDict.setDictName(dict.getDictName());
        existDict.setDescription(dict.getDescription());
        existDict.setSort(dict.getSort());
        existDict.setStatus(dict.getStatus());
        existDict.setUpdatedAt(LocalDateTime.now());

        int count = dictMapper.updateById(existDict);
        return count > 0;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDict(String code) {
        log.info("Delete dictionary: code={}", code);

        Dict existDict = dictMapper.selectByDictCode(code);
        if (existDict == null) {
            throw new BusinessException("Dictionary does not exist");
        }

        LambdaQueryWrapper<DictData> dataWrapper = new LambdaQueryWrapper<>();
        dataWrapper.eq(DictData::getDictId, existDict.getId());
        dictDataMapper.delete(dataWrapper);

        int count = dictMapper.deleteById(existDict.getId());
        return count > 0;
    }

    /** {@inheritDoc} */
    @Override
    public List<DictData> getDictData(String code) {
        log.info("Get dictionary data: code={}", code);

        if (!StringUtils.hasText(code)) {
            throw new BusinessException("Dictionary code must not be empty");
        }

        return dictDataMapper.selectByDictCode(code);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean addDictData(String code, DictData dictData) {
        log.info("Add dictionary data: code={}, label={}", code, dictData.getDictLabel());

        Dict dict = dictMapper.selectByDictCode(code);
        if (dict == null) {
            throw new BusinessException("Dictionary does not exist");
        }

        dictData.setId(SnowflakeIdGenerator.getInstance().nextId());
        dictData.setDictId(dict.getId());
        dictData.setCreatedAt(LocalDateTime.now());

        int count = dictDataMapper.insert(dictData);
        return count > 0;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateDictData(String code, DictData dictData) {
        log.info("Update dictionary data: code={}, id={}", code, dictData.getId());

        int count = dictDataMapper.updateById(dictData);
        return count > 0;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDictData(String code, Long id) {
        log.info("Delete dictionary data: code={}, id={}", code, id);

        int count = dictDataMapper.deleteById(id);
        return count > 0;
    }
}