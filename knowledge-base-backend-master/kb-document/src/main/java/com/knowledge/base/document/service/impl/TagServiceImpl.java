package com.knowledge.base.document.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowledge.base.common.exception.BusinessException;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.common.utils.SnowflakeIdGenerator;
import com.knowledge.base.document.dto.TagCreateDTO;
import com.knowledge.base.document.dto.TagQueryDTO;
import com.knowledge.base.document.dto.TagUpdateDTO;
import com.knowledge.base.document.entity.Tag;
import com.knowledge.base.document.mapper.TagMapper;
import com.knowledge.base.document.service.TagService;
import com.knowledge.base.document.vo.TagVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tag Service implementation class
 *
 * <p>Designed following the Alibaba Java Coding Guidelines, implements tag related business logic</p>
 *
 * @author airwzz999
 * @since 1.0.0
 */
@Slf4j
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Resource
    private TagMapper tagMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTag(TagCreateDTO dto) {
        log.info("Create tag: tagName={}", dto.getTagName());

        // Check whether the tag name already exists
        Tag existTag = tagMapper.selectOne(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getTagName, dto.getTagName())
        );
        if (existTag != null) {
            throw new BusinessException("Tag name already exists");
        }

        // Generate the tag code
        String tagCode = StringUtils.hasText(dto.getTagName())
                ? generateTagCode(dto.getTagName())
                : "TAG_" + System.currentTimeMillis();

        // Build the tag entity
        Tag tag = new Tag();
        tag.setId(SnowflakeIdGenerator.getInstance().nextId());
        tag.setTagName(dto.getTagName());
        tag.setTagCode(tagCode);
        tag.setCategoryId(dto.getCategoryId());
        tag.setTagType(dto.getTagType() != null ? dto.getTagType() : 1);
        tag.setColor(dto.getColor());
        tag.setIcon(dto.getIcon());
        tag.setDocCount(0);
        tag.setStatus(1);

        // Save the tag
        int count = tagMapper.insert(tag);
        if (count <= 0) {
            throw new BusinessException("Failed to create tag");
        }

        return tag.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTag(TagUpdateDTO dto) {
        log.info("Update tag: tagId={}", dto.getId());

        if (dto.getId() == null) {
            throw new BusinessException("Tag ID must not be null");
        }

        // Check whether the tag exists
        Tag existTag = tagMapper.selectById(dto.getId());
        if (existTag == null) {
            throw new BusinessException("Tag does not exist");
        }

        // Check whether the tag name is used by another tag
        if (StringUtils.hasText(dto.getTagName())
                && !dto.getTagName().equals(existTag.getTagName())) {
            Tag tag = tagMapper.selectOne(
                    new LambdaQueryWrapper<Tag>()
                            .eq(Tag::getTagName, dto.getTagName())
            );
            if (tag != null && !tag.getId().equals(dto.getId())) {
                throw new BusinessException("Tag name is already in use");
            }
        }

        // Build the update entity
        Tag tag = new Tag();
        tag.setId(dto.getId());
        if (StringUtils.hasText(dto.getTagName())) {
            tag.setTagName(dto.getTagName());
        }
        if (dto.getCategoryId() != null) {
            tag.setCategoryId(dto.getCategoryId());
        }
        if (dto.getColor() != null) {
            tag.setColor(dto.getColor());
        }
        if (dto.getIcon() != null) {
            tag.setIcon(dto.getIcon());
        }
        if (dto.getStatus() != null) {
            tag.setStatus(dto.getStatus());
        }

        int count = tagMapper.updateById(tag);
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTag(Long tagId) {
        log.info("Delete tag: tagId={}", tagId);

        if (tagId == null) {
            throw new BusinessException("Tag ID must not be null");
        }

        // Check whether the tag exists
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BusinessException("Tag does not exist");
        }

        // Check whether there are associated documents
        if (tag.getDocCount() != null && tag.getDocCount() > 0) {
            throw new BusinessException("This tag has documents and cannot be deleted");
        }

        // Delete the tag
        int count = tagMapper.deleteById(tagId);
        return count > 0;
    }

    @Override
    public TagVO getTagDetail(Long tagId) {
        if (tagId == null) {
            throw new BusinessException("Tag ID must not be null");
        }

        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new BusinessException("Tag does not exist");
        }

        return convertToVO(tag);
    }

    @Override
    public PageResult<TagVO> pageTags(TagQueryDTO dto) {
        // Build the query conditions
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getTagName())) {
            wrapper.like(Tag::getTagName, dto.getTagName())
                    .or()
                    .like(Tag::getTagCode, dto.getTagName());
        }
        if (dto.getCategoryId() != null) {
            wrapper.eq(Tag::getCategoryId, dto.getCategoryId());
        }
        if (dto.getTagType() != null) {
            wrapper.eq(Tag::getTagType, dto.getTagType());
        }
        wrapper.eq(Tag::getStatus, 1);

        // Paginated query
        Page<Tag> page = new Page<>(dto.getCurrent(), dto.getSize());
        IPage<Tag> tagPage = tagMapper.selectPage(page, wrapper);

        // Convert to VO
        IPage<TagVO> voPage = tagPage.convert(this::convertToVO);

        return PageResult.<TagVO>builder()
                .records(voPage.getRecords())
                .total(voPage.getTotal())
                .current(voPage.getCurrent())
                .size(voPage.getSize())
                .build();
    }

    @Override
    public List<TagVO> getHotTags(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        List<Tag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getStatus, 1)
                        .orderByDesc(Tag::getDocCount)
                        .last("LIMIT " + limit)
        );

        return tags.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TagVO> getTagsByCategory(Long categoryId) {
        if (categoryId == null) {
            throw new BusinessException("Category ID must not be null");
        }

        List<Tag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getCategoryId, categoryId)
                        .eq(Tag::getStatus, 1)
                        .orderByDesc(Tag::getDocCount)
        );

        return tags.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchCreateTags(List<String> tagNames) {
        log.info("Batch create tags: tagCount={}", tagNames.size());

        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> tagIds = new ArrayList<>();

        for (String tagName : tagNames) {
            if (!StringUtils.hasText(tagName)) {
                continue;
            }

            // Check whether the tag already exists
            Tag existTag = tagMapper.selectOne(
                    new LambdaQueryWrapper<Tag>()
                            .eq(Tag::getTagName, tagName.trim())
            );

            Long tagId;
            if (existTag != null) {
                tagId = existTag.getId();
            } else {
                // Create a new tag
                TagCreateDTO dto = new TagCreateDTO();
                dto.setTagName(tagName.trim());
                dto.setTagType(1); // 1-USER type

                tagId = createTag(dto);
            }

            tagIds.add(tagId);
        }

        return tagIds;
    }

    /**
     * Converts to VO
     *
     * @param tag tag entity
     * @return tag VO
     */
    private TagVO convertToVO(Tag tag) {
        return TagVO.builder()
                .id(tag.getId())
                .tagName(tag.getTagName())
                .tagCode(tag.getTagCode())
                .categoryId(tag.getCategoryId())
                .tagType(tag.getTagType())
                .color(tag.getColor())
                .icon(tag.getIcon())
                .docCount(tag.getDocCount() != null ? tag.getDocCount() : 0)
                .status(tag.getStatus())
                .createdAt(tag.getCreatedAt())
                .build();
    }

    /**
     * Generates a tag code
     *
     * @param tagName tag name
     * @return tag code
     */
    private String generateTagCode(String tagName) {
        // Simple code generation logic
        return "TAG_" + tagName.toUpperCase()
                .replaceAll("[^A-Z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "") + "_" + System.currentTimeMillis();
    }
}
