package com.knowledge.base.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.knowledge.base.common.result.PageResult;
import com.knowledge.base.document.dto.TagCreateDTO;
import com.knowledge.base.document.dto.TagQueryDTO;
import com.knowledge.base.document.dto.TagUpdateDTO;
import com.knowledge.base.document.entity.Tag;
import com.knowledge.base.document.vo.TagVO;

import java.util.List;

/**
 * Tag Service interface
 *
 * @author airwzz999
 * @since 1.0.0
 */
public interface TagService extends IService<Tag> {

    /**
     * Creates a tag
     *
     * @param dto creation DTO
     * @return tag ID
     */
    Long createTag(TagCreateDTO dto);

    /**
     * Updates a tag
     *
     * @param dto update DTO
     * @return whether successful
     */
    Boolean updateTag(TagUpdateDTO dto);

    /**
     * Deletes a tag
     *
     * @param tagId tag ID
     * @return whether successful
     */
    Boolean deleteTag(Long tagId);

    /**
     * Gets tag details
     *
     * @param tagId tag ID
     * @return tag VO
     */
    TagVO getTagDetail(Long tagId);

    /**
     * Paginated query of tags
     *
     * @param dto query DTO
     * @return paginated result
     */
    PageResult<TagVO> pageTags(TagQueryDTO dto);

    /**
     * Gets popular tags
     *
     * @param limit result limit
     * @return tag list
     */
    List<TagVO> getHotTags(Integer limit);

    /**
     * Gets tags by category
     *
     * @param categoryId category ID
     * @return tag list
     */
    List<TagVO> getTagsByCategory(Long categoryId);

    /**
     * Batch-creates tags
     *
     * @param tagNames tag name list
     * @return tag ID list
     */
    List<Long> batchCreateTags(List<String> tagNames);
}
